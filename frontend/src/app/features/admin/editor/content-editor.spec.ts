import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { protectLiteralDollars } from './milkdown/markdown-editor';
import { ContentEditor } from './content-editor';
import { imageMarkdown } from './snippets';

@Component({
  imports: [ContentEditor],
  template: `<app-content-editor controlId="texte" [(value)]="text" [error]="error()" />`,
})
class Host {
  readonly text = signal('Premier paragraphe.');
  readonly error = signal<string | null>(null);
}

async function render() {
  TestBed.configureTestingModule({
    providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
  });
  const fixture = TestBed.createComponent(Host);
  fixture.detectChanges();
  await fixture.whenStable();
  const element = fixture.nativeElement as HTMLElement;
  const tab = (name: string) =>
    Array.from(element.querySelectorAll<HTMLElement>('[role="tab"]')).find(
      (candidate) => candidate.textContent?.trim() === name,
    )!;
  const open = async (name: string) => {
    tab(name).click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  };
  return { fixture, element, tab, open, http: TestBed.inject(HttpTestingController) };
}

describe('imageMarkdown', () => {
  it('writes an image, or a captioned figure, escaping brackets and quotes', () => {
    expect(imageMarkdown('/m/a.webp', 'Un [plan]', '')).toBe('![Un \\[plan\\]](/m/a.webp)');
    expect(imageMarkdown('/m/a.webp', 'Plan', 'Le "plan" final')).toBe(
      '![Plan](/m/a.webp "Le \\"plan\\" final")',
    );
  });
});

describe('protectLiteralDollars', () => {
  it('escapes the dollars that the public engine reads as text, outside code', () => {
    expect(protectLiteralDollars('5 $ et 6 $, puis $x$.')).toBe('5 \\$ et 6 \\$, puis $x$.');
    expect(protectLiteralDollars('$$\nE = mc^2\n$$')).toBe('$$\nE = mc^2\n$$');
    expect(protectLiteralDollars('`a $ b` et \\$4')).toBe('`a $ b` et \\$4');
    expect(protectLiteralDollars('```bash\necho $ HOME\n```')).toBe('```bash\necho $ HOME\n```');
    expect(protectLiteralDollars('coût $5$4')).toBe('coût \\$5\\$4');
  });
});

describe('ContentEditor', () => {
  it('offers three tabs, the visual one first, as an APG tab list', async () => {
    const { element, tab } = await render();

    const names = Array.from(element.querySelectorAll('[role="tab"]')).map((node) =>
      node.textContent?.trim(),
    );
    expect(names).toEqual(['Visuel', 'Markdown', 'Aperçu']);
    expect(tab('Visuel').getAttribute('aria-selected')).toBe('true');
    expect(element.querySelector('[role="tablist"]')?.getAttribute('aria-labelledby')).toBe(
      'texte-onglets',
    );
    expect(element.querySelector('[role="toolbar"]')?.getAttribute('aria-label')).toBe(
      'Mise en forme',
    );
  });

  it('writes the markdown typed in its tab back to the form, and previews it with the site engine', async () => {
    const { fixture, element, open } = await render();
    await open('Markdown');

    const source = element.querySelector<HTMLTextAreaElement>('#texte')!;
    expect(source.value).toBe('Premier paragraphe.');
    source.value = '## Titre\n\nUn **mot**.';
    source.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(fixture.componentInstance.text()).toBe('## Titre\n\nUn **mot**.');

    await open('Aperçu');
    expect(element.querySelector('app-markdown-view h3')?.textContent).toContain('Titre');
    expect(element.querySelector('app-markdown-view strong')?.textContent).toBe('mot');
  });

  it('inserts an image of the media library at the cursor, with its alternative text', async () => {
    const { fixture, element, open, http } = await render();
    await open('Markdown');
    const source = element.querySelector<HTMLTextAreaElement>('#texte')!;
    source.setSelectionRange(0, 0);

    element.querySelector<HTMLButtonElement>('[aria-label="Insertion"] button')!.click();
    fixture.detectChanges();
    http
      .expectOne((request) => request.url === '/api/admin/media')
      .flush({
        content: [
          {
            id: 3,
            url: '/api/public/media/plan.webp',
            originalName: 'plan.webp',
            altText: 'Plan du réseau',
            format: 'WEBP',
            mimeType: 'image/webp',
            width: 1600,
            height: 1000,
            sizeBytes: 40_000,
            createdAt: '2026-10-01T08:00:00Z',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      });
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    element.querySelector<HTMLButtonElement>('app-media-picker ul button')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const alt = element.querySelector<HTMLInputElement>('#texte-alt')!;
    expect(alt.value).toBe('Plan du réseau');
    const caption = element.querySelector<HTMLInputElement>('#texte-legende')!;
    caption.value = 'Le réseau';
    caption.dispatchEvent(new Event('input'));
    element
      .querySelector('dialog.editor-dialog form')!
      .dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();

    expect(fixture.componentInstance.text()).toBe(
      '![Plan du réseau](/api/public/media/plan.webp "Le réseau")\n\nPremier paragraphe.',
    );
  });

  it('sends to the markdown tab when the content has an error', async () => {
    const { fixture, element, tab } = await render();
    fixture.componentInstance.error.set('Le contenu compte 100 000 caractères au plus.');
    fixture.detectChanges();

    const fix = element.querySelector<HTMLButtonElement>('[data-invalid="true"]')!;
    expect(fix.closest('p')?.textContent).toContain('100 000 caractères');
    fix.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(tab('Markdown').getAttribute('aria-selected')).toBe('true');
  });
});
