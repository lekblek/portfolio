import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { Toaster } from '../../../../shared/ui/toast';
import { TermListPage } from './term-list-page';

const API = '/api/admin/tags';
const TAGS = [
  { id: 1, name: 'Java', slug: 'java' },
  { id: 2, name: 'Tests', slug: 'tests' },
];

async function setUp() {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter(
        [{ path: 'admin/taxonomy/tags', component: TermListPage, data: { vocabularyKey: 'tags' } }],
        withComponentInputBinding(),
      ),
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl('/admin/taxonomy/tags');
  const http = TestBed.inject(HttpTestingController);
  const element = () => harness.routeNativeElement as HTMLElement;
  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await harness.fixture.whenStable();
    harness.detectChanges();
  };
  const load = async (tags: object[] = TAGS) => {
    http.expectOne({ method: 'GET', url: API }).flush(tags);
    await settle();
  };
  const button = (name: string) =>
    Array.from(element().querySelectorAll('button')).find((candidate) =>
      candidate.textContent?.replace(/\s+/g, ' ').trim().startsWith(name),
    )!;
  const confirmButton = (name: string) =>
    Array.from(element().querySelectorAll('dialog button')).find(
      (candidate) => candidate.textContent?.trim() === name,
    ) as HTMLButtonElement;
  return { http, element, settle, load, button, confirmButton, toaster: TestBed.inject(Toaster) };
}

describe('TermListPage', () => {
  it('lists the terms in a table, with an accessible name for each action', async () => {
    const { element, load } = await setUp();
    await load();

    expect(element().querySelector('h1')?.textContent).toBe('Tags');
    expect(element().textContent).toContain('2 tags');
    const rows = Array.from(element().querySelectorAll('tbody tr')).map((row) =>
      row.querySelector('th[scope="row"]')?.textContent?.trim(),
    );
    expect(rows).toEqual(['Java', 'Tests']);
    expect(element().querySelector('tbody tr a')?.textContent?.replace(/\s+/g, ' ').trim()).toBe(
      'Modifier le tag « Java »',
    );
    expect(element().querySelector('a[href="/admin/taxonomy/tags/new"]')?.textContent?.trim()).toBe(
      'Nouveau tag',
    );
  });

  it('says so when the vocabulary is empty', async () => {
    const { element, load } = await setUp();
    await load([]);

    expect(element().querySelector('table')).toBeNull();
    expect(element().textContent).toContain('Aucun tag pour le moment.');
  });

  it('deletes a term once confirmed, then notifies it', async () => {
    const { http, load, button, confirmButton, settle, toaster } = await setUp();
    await load();

    button('Supprimer le tag « Java »').click();
    await settle();
    confirmButton('Supprimer le tag').click();
    // La suppression est en cours : la page n'est pas stable avant la réponse
    await new Promise((resolve) => setTimeout(resolve));
    http
      .expectOne({ method: 'DELETE', url: `${API}/1` })
      .flush(null, { status: 204, statusText: '' });
    await new Promise((resolve) => setTimeout(resolve));
    // La liste se recharge au passage suivant de la détection des changements
    TestBed.tick();
    http.expectOne({ method: 'GET', url: API }).flush([TAGS[1]]);
    await settle();

    expect(toaster.toasts().map((toast) => [toast.tone, toast.message])).toEqual([
      ['success', 'Tag «\u00a0Java\u00a0» supprimé.'],
    ]);
  });

  it('keeps a term that is still used, and says why', async () => {
    const { http, load, button, confirmButton, settle, toaster } = await setUp();
    await load();

    button('Supprimer le tag « Java »').click();
    await settle();
    confirmButton('Supprimer le tag').click();
    // La suppression est en cours : la page n'est pas stable avant la réponse
    await new Promise((resolve) => setTimeout(resolve));
    http
      .expectOne(`${API}/1`)
      .flush({ status: 409, code: 'TERM_STILL_USED' }, { status: 409, statusText: 'Conflict' });
    await new Promise((resolve) => setTimeout(resolve));
    TestBed.tick();
    http.expectOne({ method: 'GET', url: API }).flush(TAGS);
    await settle();

    const [toast] = toaster.toasts();
    expect(toast.tone).toBe('danger');
    expect(toast.message).toContain('est encore utilisé par une publication');
  });

  it('does nothing when the deletion is cancelled', async () => {
    const { http, load, button, confirmButton, settle, toaster } = await setUp();
    await load();

    button('Supprimer le tag « Java »').click();
    await settle();
    confirmButton('Annuler').click();
    await settle();

    http.expectNone({ method: 'DELETE' });
    expect(toaster.toasts()).toEqual([]);
  });
});
