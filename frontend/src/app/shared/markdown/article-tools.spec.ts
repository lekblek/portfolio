import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { CodeCopy } from './code-copy';
import { TableOfContents } from './table-of-contents';
import { TocEntry } from './toc';

@Component({
  imports: [TableOfContents],
  template: `<app-table-of-contents [entries]="entries()" path="/articles/exemple" />`,
})
class TocHost {
  readonly entries = signal<TocEntry[]>([
    { id: 'contexte', text: 'Contexte', level: 2 },
    { id: 'contraintes', text: 'Contraintes', level: 3 },
    { id: 'detail', text: 'Détail', level: 4 },
    { id: 'architecture', text: 'Architecture', level: 2 },
  ]);
}

@Component({
  imports: [CodeCopy],
  template: `
    <app-code-copy>
      <div class="code-block">
        <div class="code-bar">
          <button type="button" class="code-copy" data-code-copy>
            Copier<span class="sr-only"> le code</span>
          </button>
        </div>
        <pre class="code"><code>const x = 1;</code></pre>
      </div>
    </app-code-copy>
  `,
})
class CopyHost {}

describe('TableOfContents', () => {
  it('lists the first two heading levels, nested, as links on the page', () => {
    const fixture = TestBed.createComponent(TocHost);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('nav')?.getAttribute('aria-label')).toBe('Sommaire');
    const top = Array.from(element.querySelectorAll('nav > ol > li > a'));
    expect(top.map((link) => link.getAttribute('href'))).toEqual([
      '/articles/exemple#contexte',
      '/articles/exemple#architecture',
    ]);
    expect(element.querySelector('ol ol a')?.textContent).toBe('Contraintes');
    expect(element.textContent).not.toContain('Détail');
  });
});

describe('CodeCopy', () => {
  let writeText: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.useFakeTimers();
    writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
  });

  afterEach(() => vi.useRealTimers());

  async function click() {
    const fixture = TestBed.createComponent(CopyHost);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector('button') as HTMLButtonElement;
    button.click();
    // Promesse du presse-papiers résolue, sans avancer le délai de retour du libellé
    for (let i = 0; i < 3; i++) {
      await Promise.resolve();
    }
    fixture.detectChanges();
    return { fixture, element, button };
  }

  it('copies the code of its block and announces it', async () => {
    const { element, button } = await click();

    expect(writeText).toHaveBeenCalledWith('const x = 1;');
    expect(button.textContent?.trim()).toBe('Copié le code');
    expect(element.querySelector('[aria-live="polite"]')?.textContent).toBe(
      'Code copié dans le presse-papiers.',
    );
  });

  it('comes back to its label after a moment', async () => {
    const { fixture, element, button } = await click();

    vi.advanceTimersByTime(2000);
    fixture.detectChanges();

    expect(button.textContent?.trim()).toBe('Copier le code');
    expect(element.querySelector('[aria-live="polite"]')?.textContent).toBe('');
  });

  it('announces a failure without the clipboard', async () => {
    writeText.mockRejectedValue(new Error('refusé'));

    const { element } = await click();

    expect(element.querySelector('[aria-live="polite"]')?.textContent).toContain(
      'La copie a échoué',
    );
  });
});
