import { HttpEventType, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { AdminMedia } from '../../../../core/api/api-types';
import { Toaster } from '../../../../shared/ui/toast';
import { MediaListPage } from './media-list-page';

const API = '/api/admin/media';

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
const PDF: AdminMedia = {
  ...IMAGE,
  id: 3,
  url: '/api/public/media/cle.pdf',
  originalName: 'cv.pdf',
  format: 'PDF',
  mimeType: 'application/pdf',
  width: null,
  height: null,
};

function page(content: AdminMedia[]) {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: 1,
    first: true,
    last: true,
  };
}

async function setUp() {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter(
        [{ path: 'admin/media', component: MediaListPage }],
        withComponentInputBinding(),
      ),
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl('/admin/media');
  const http = TestBed.inject(HttpTestingController);
  const element = () => harness.routeNativeElement as HTMLElement;
  const tick = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    TestBed.tick();
  };
  const choose = async (files: File[]) => {
    const input = element().querySelector<HTMLInputElement>('input[type="file"]')!;
    Object.defineProperty(input, 'files', { value: files, configurable: true });
    input.dispatchEvent(new Event('change'));
    await tick();
  };
  const uploadsText = () =>
    element().querySelector('[aria-labelledby="envois"]')?.textContent ?? '';
  return { http, element, tick, choose, uploadsText, toaster: TestBed.inject(Toaster) };
}

describe('MediaListPage', () => {
  it('lists the media in a table and flags an image without a text alternative', async () => {
    const { http, element, tick } = await setUp();
    http.expectOne(`${API}?page=0`).flush(page([IMAGE, PDF]));
    await tick();

    const rows = Array.from(element().querySelectorAll('tbody tr'));
    expect(rows.map((row) => row.querySelector('th')?.textContent?.trim())).toEqual([
      'couverture.png',
      'cv.pdf',
    ]);
    expect(rows[0].textContent).toContain('PNG · 1200\u00a0×\u00a0800');
    expect(rows[0].textContent).toContain('Manquant');
    expect(rows[0].querySelector('img')?.getAttribute('loading')).toBe('lazy');
    expect(rows[1].querySelector('img')).toBeNull();
  });

  it('sends the files one by one, with their progress, then shows the newest', async () => {
    const { http, element, tick, choose, uploadsText, toaster } = await setUp();
    http.expectOne(`${API}?page=0`).flush(page([]));
    await tick();

    await choose([
      new File(['png'], 'couverture.png', { type: 'image/png' }),
      new File(['texte'], 'notes.txt', { type: 'text/plain' }),
    ]);

    const post = http.expectOne({ method: 'POST', url: API });
    expect((post.request.body as FormData).get('file')).toBeInstanceOf(File);
    post.event({ type: HttpEventType.UploadProgress, loaded: 50, total: 100 });
    await tick();
    expect(element().querySelector('progress')?.getAttribute('value')).toBe('50');
    expect(uploadsText()).toContain('Format refusé');

    post.flush(IMAGE);
    await tick();
    http.expectOne(`${API}?page=0`).flush(page([IMAGE]));
    await tick();

    expect(uploadsText()).toContain('Envoyé.');
    expect(uploadsText()).toContain('Écrire le texte alternatif');
    expect(toaster.toasts().map((toast) => toast.message)).toEqual([
      '1 fichier envoyé. Écrivez le texte alternatif des images.',
    ]);
    expect(element().textContent).toContain('1 fichier refusé');
  });

  it('says why the server refused a file', async () => {
    const { http, tick, choose, uploadsText } = await setUp();
    http.expectOne(`${API}?page=0`).flush(page([]));
    await tick();

    await choose([new File(['faux'], 'faux.png', { type: 'image/png' })]);
    http
      .expectOne({ method: 'POST', url: API })
      .flush(
        { status: 415, code: 'UNSUPPORTED_MEDIA_FORMAT' },
        { status: 415, statusText: 'Unsupported Media Type' },
      );
    await tick();

    expect(uploadsText()).toContain('le contenu n’est ni une image PNG, JPEG, WebP, ni un PDF');
  });

  it('keeps a medium still used by a content, and says why', async () => {
    const { http, element, tick, toaster } = await setUp();
    http.expectOne(`${API}?page=0`).flush(page([IMAGE]));
    await tick();

    Array.from(element().querySelectorAll('tbody button'))
      .find((button) => button.textContent?.includes('Supprimer'))!
      .dispatchEvent(new Event('click'));
    await tick();
    Array.from(element().querySelectorAll('dialog button'))
      .find((button) => button.textContent?.trim() === 'Supprimer le média')!
      .dispatchEvent(new Event('click'));
    await tick();
    http
      .expectOne({ method: 'DELETE', url: `${API}/4` })
      .flush({ status: 409, code: 'MEDIA_STILL_REFERENCED' }, { status: 409, statusText: '' });
    await tick();
    http.expectOne(`${API}?page=0`).flush(page([IMAGE]));
    await tick();

    const [toast] = toaster.toasts();
    expect(toast.tone).toBe('danger');
    expect(toast.message).toContain('il est encore utilisé par un contenu');
  });
});
