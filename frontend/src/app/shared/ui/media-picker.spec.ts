import { Component, DOCUMENT, signal, viewChild } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AdminMedia } from '../../core/api/api-types';
import { MediaKind, MediaPicker, MediaPickerState } from './media-picker';

const IMAGE: AdminMedia = {
  id: 4,
  url: '/api/public/media/portrait.png',
  originalName: 'portrait.png',
  format: 'PNG',
  mimeType: 'image/png',
  sizeBytes: 120_000,
  width: 800,
  height: 800,
  altText: 'Portrait',
  createdAt: '2026-10-02T08:00:00Z',
};
const PDF: AdminMedia = {
  ...IMAGE,
  id: 5,
  url: '/api/public/media/cv.pdf',
  originalName: 'cv.pdf',
  format: 'PDF',
  mimeType: 'application/pdf',
  width: null,
  height: null,
  altText: null,
};

@Component({
  imports: [MediaPicker],
  template: `
    <button type="button" id="ouvrir">Choisir l’avatar</button>
    <app-media-picker
      #picker
      title="Choisir l’avatar"
      [kind]="kind()"
      [media]="media()"
      [page]="page()"
      [totalPages]="2"
      [state]="state()"
      [selectedId]="4"
      libraryPath="/admin/media"
      (chosen)="chosen.set($event)"
      (pageChange)="page.set($event)"
    />
  `,
})
class Host {
  readonly picker = viewChild.required<MediaPicker>('picker');
  readonly kind = signal<MediaKind>('image');
  readonly media = signal<AdminMedia[]>([IMAGE, PDF]);
  readonly page = signal(1);
  readonly state = signal<MediaPickerState>('ready');
  readonly chosen = signal<AdminMedia | null>(null);
}

async function open(kind: MediaKind = 'image') {
  TestBed.configureTestingModule({ providers: [provideRouter([])] });
  const fixture = TestBed.createComponent(Host);
  fixture.componentInstance.kind.set(kind);
  await fixture.whenStable();
  const element = fixture.nativeElement as HTMLElement;
  const opener = element.querySelector<HTMLButtonElement>('#ouvrir')!;
  opener.focus();
  fixture.componentInstance.picker().open();
  await fixture.whenStable();
  const dialog = element.querySelector('dialog')!;
  const button = (name: string) =>
    Array.from(dialog.querySelectorAll('button')).find(
      (candidate) => candidate.textContent?.replace(/\s+/g, ' ').trim() === name,
    )!;
  return { fixture, dialog, opener, button };
}

describe('MediaPicker', () => {
  it('lists only the expected kind, marks the current one and focuses the first choice', async () => {
    const { dialog } = await open();

    expect(dialog.open).toBe(true);
    expect(dialog.querySelector(`#${dialog.getAttribute('aria-labelledby')}`)?.textContent).toBe(
      'Choisir l’avatar',
    );
    const choices = dialog.querySelectorAll('.media-picker-choice');
    expect(choices).toHaveLength(1);
    expect(choices[0].textContent?.replace(/\s+/g, ' ').trim()).toMatch(
      /^Choisir portrait\.png, 800 × 800 · /,
    );
    expect(choices[0].getAttribute('aria-current')).toBe('true');
    expect(TestBed.inject(DOCUMENT).activeElement).toBe(
      dialog.querySelector('.media-picker-choice'),
    );
    expect(dialog.querySelector('a')?.getAttribute('href')).toBe('/admin/media');
  });

  it('hands the choice over, closes and gives the focus back', async () => {
    const { fixture, dialog, opener } = await open('pdf');

    dialog.querySelector<HTMLButtonElement>('.media-picker-choice')!.click();

    expect(fixture.componentInstance.chosen()).toBe(PDF);
    expect(dialog.open).toBe(false);
    expect(TestBed.inject(DOCUMENT).activeElement).toBe(opener);
  });

  it('pages through the library', async () => {
    const { fixture, button } = await open();

    expect(button('Plus récents').disabled).toBe(true);
    button('Plus anciens').click();

    expect(fixture.componentInstance.page()).toBe(2);
  });

  it('says when the page holds nothing of the expected kind, or failed to load', async () => {
    const { fixture, dialog } = await open();

    fixture.componentInstance.media.set([PDF]);
    await fixture.whenStable();
    expect(dialog.textContent).toContain('Aucune image sur cette page de la médiathèque.');

    fixture.componentInstance.state.set('error');
    await fixture.whenStable();
    expect(dialog.querySelector('[role="alert"]')?.textContent).toContain(
      'La médiathèque n’a pas pu être chargée.',
    );
  });
});
