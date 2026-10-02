import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { AdminMedia } from '../../../../core/api/api-types';
import { Toaster } from '../../../../shared/ui/toast';
import { MediaEditPage } from './media-edit-page';

const IMAGE: AdminMedia = {
  id: 4,
  url: '/api/public/media/cle.png',
  originalName: 'couverture.png',
  format: 'PNG',
  mimeType: 'image/png',
  sizeBytes: 120_000,
  width: 1200,
  height: 800,
  altText: null,
  createdAt: '2026-10-02T08:00:00Z',
};

@Component({ template: '<h1>Médias</h1>' })
class ListStub {}

async function setUp(media: AdminMedia) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter(
        [
          { path: 'admin/media', component: ListStub },
          { path: 'admin/media/:id', component: MediaEditPage },
        ],
        withComponentInputBinding(),
      ),
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(`/admin/media/${media.id}`);
  const http = TestBed.inject(HttpTestingController);
  http.expectOne(`/api/admin/media/${media.id}`).flush(media);
  const element = () => harness.routeNativeElement as HTMLElement;
  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await harness.fixture.whenStable();
    harness.detectChanges();
  };
  await settle();
  const submit = async () => {
    element()
      .querySelector('form')!
      .dispatchEvent(new Event('submit', { cancelable: true }));
    await new Promise((resolve) => setTimeout(resolve));
    harness.detectChanges();
  };
  return { http, element, settle, submit };
}

describe('MediaEditPage', () => {
  it('describes the image and requires a text alternative', async () => {
    const { http, element, submit } = await setUp(IMAGE);

    expect(element().querySelector('h1')?.textContent).toBe('Média «\u00a0couverture.png\u00a0»');
    expect(element().querySelector('.media-facts')?.textContent).toContain(
      '1200\u00a0×\u00a0800 pixels',
    );

    await submit();

    http.expectNone({ method: 'PATCH' });
    expect(element().querySelector('#media-texte-alternatif-erreur')?.textContent).toContain(
      'Décrivez l’image',
    );
  });

  it('saves the text alternative, then notifies it', async () => {
    const { http, element, submit, settle } = await setUp(IMAGE);
    const field = element().querySelector<HTMLTextAreaElement>('#media-texte-alternatif')!;
    field.value = '  Page d’accueil du portfolio.  ';
    field.dispatchEvent(new Event('input'));

    await submit();
    const patch = http.expectOne({ method: 'PATCH', url: '/api/admin/media/4' });
    expect(patch.request.body).toEqual({ altText: 'Page d’accueil du portfolio.' });
    patch.flush({ ...IMAGE, altText: 'Page d’accueil du portfolio.' });
    await settle();

    expect(TestBed.inject(Router).url).toBe('/admin/media');
    expect(TestBed.inject(Toaster).toasts()[0].message).toBe(
      'Texte alternatif de «\u00a0couverture.png\u00a0» enregistré.',
    );
  });

  it('asks nothing of a PDF', async () => {
    const { element } = await setUp({
      ...IMAGE,
      format: 'PDF',
      mimeType: 'application/pdf',
      originalName: 'cv.pdf',
      width: null,
      height: null,
    });

    expect(element().querySelector('form')).toBeNull();
    expect(element().querySelector('a[href="/api/public/media/cle.png"]')?.textContent).toContain(
      'Ouvrir le PDF',
    );
    expect(element().textContent).toContain('Un PDF n’a pas de texte alternatif');
  });
});
