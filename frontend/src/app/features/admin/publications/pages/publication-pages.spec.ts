import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { AdminPublication, AdminPublicationSummary } from '../../../../core/api/api-types';
import { Toaster } from '../../../../shared/ui/toast';
import { PublicationFormPage } from './publication-form-page';
import { PublicationListPage } from './publication-list-page';

const API = '/api/admin/publications';

const DRAFT: AdminPublication = {
  id: 9,
  type: 'ARTICLE',
  title: 'Suivi multi-objets',
  slug: 'suivi-multi-objets',
  slugLocked: false,
  summary: 'Associer les détections.',
  contentMarkdown: '',
  status: 'DRAFT',
  publishedAt: null,
  featured: false,
  categoryId: null,
  tagIds: [],
  coverMediaId: null,
  seoTitle: null,
  seoDescription: null,
  createdAt: '2026-10-01T08:00:00Z',
  updatedAt: '2026-10-01T08:00:00Z',
};

@Component({ template: '<h1>Liste</h1>' })
class ListStub {}

async function open(url: string, publication?: AdminPublication) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter(
        [
          { path: 'admin/publications', component: PublicationListPage },
          {
            path: 'admin/publications/new/article',
            component: PublicationFormPage,
            data: { createType: 'ARTICLE' },
          },
          { path: 'admin/publications/:id', component: PublicationFormPage },
          { path: 'articles/:slug', component: ListStub },
        ],
        withComponentInputBinding(),
      ),
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  const http = TestBed.inject(HttpTestingController);
  const tick = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    harness.detectChanges();
  };
  const flushVocabularies = () => {
    for (const request of http.match((r) => r.url === '/api/admin/categories')) {
      request.flush([{ id: 3, name: 'Backend', slug: 'backend', description: null }]);
    }
    for (const request of http.match((r) => r.url === '/api/admin/tags')) {
      request.flush([{ id: 1, name: 'Java', slug: 'java' }]);
    }
  };
  flushVocabularies();
  if (publication) {
    http.expectOne(`${API}/${publication.id}`).flush(publication);
  }
  await tick();
  const element = () => harness.routeNativeElement as HTMLElement;
  const settle = async () => {
    await tick();
    await harness.fixture.whenStable();
    harness.detectChanges();
  };
  const type = (id: string, value: string) => {
    const field = element().querySelector<HTMLInputElement>(`#${id}`)!;
    field.value = value;
    field.dispatchEvent(new Event('input'));
  };
  const button = (name: string) =>
    Array.from(element().querySelectorAll('button')).find(
      (candidate) => candidate.textContent?.replace(/\s+/g, ' ').trim() === name,
    )!;
  const submit = async () => {
    element()
      .querySelector('form.publication-form')!
      .dispatchEvent(new Event('submit', { cancelable: true }));
    await tick();
  };
  return { harness, http, element, settle, tick, type, button, submit, flushVocabularies };
}

describe('PublicationListPage', () => {
  it('lists the publications with their type, status and dates, filtered from the address', async () => {
    const { http, element, tick } = await open('/admin/publications?type=ARTICLE&status=SCHEDULED');
    const request = http.expectOne((r) => r.url === API);
    expect(request.request.params.get('type')).toBe('ARTICLE');
    expect(request.request.params.get('status')).toBe('SCHEDULED');
    const rows: AdminPublicationSummary[] = [
      { ...DRAFT, status: 'SCHEDULED', publishedAt: '2026-10-24T08:00:00Z' },
    ];
    request.flush({
      content: rows,
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    });
    await tick();

    const row = element().querySelector('tbody tr')!;
    expect(row.querySelector('th')?.textContent).toContain('Suivi multi-objets');
    expect(row.textContent).toContain('Article');
    expect(row.querySelector('app-status-badge')?.textContent?.trim()).toBe('Programmée');
    expect(row.textContent).toContain('prévue le 24 octobre 2026');
    expect(element().querySelector<HTMLSelectElement>('#filtre-statut')?.value).toBe('SCHEDULED');
  });
});

describe('PublicationFormPage', () => {
  it('creates a draft, then opens its own page', async () => {
    const { http, element, type, submit, tick } = await open('/admin/publications/new/article');

    expect(element().querySelector('h1')?.textContent).toBe('Nouvel article');
    expect(element().textContent).toContain('Une publication naît brouillon');
    type('publication-titre', 'Suivi multi-objets');
    type('publication-resume', 'Associer les détections.');
    await submit();

    const post = http.expectOne({ method: 'POST', url: API });
    expect(post.request.body).toMatchObject({
      type: 'ARTICLE',
      title: 'Suivi multi-objets',
      tagIds: [],
    });
    post.flush(DRAFT);
    await tick();
    await tick();

    expect(TestBed.inject(Router).url).toBe('/admin/publications/9');
    expect(TestBed.inject(Toaster).toasts()[0].message).toBe(
      'Brouillon «\u00a0Suivi multi-objets\u00a0» créé.',
    );
  });

  it('saves a change and stays on the page', async () => {
    const { http, type, submit, tick } = await open('/admin/publications/9', DRAFT);

    type('publication-titre', 'Suivi multi-objets en temps réel');
    await submit();
    const put = http.expectOne({ method: 'PUT', url: `${API}/9` });
    expect(put.request.body).toMatchObject({
      title: 'Suivi multi-objets en temps réel',
      slug: undefined,
    });
    put.flush({ ...DRAFT, title: 'Suivi multi-objets en temps réel' });
    await tick();

    expect(TestBed.inject(Router).url).toBe('/admin/publications/9');
    expect(TestBed.inject(Toaster).toasts()[0].message).toContain('enregistré');
  });

  it('publishes from the status panel, offering only the allowed transitions', async () => {
    const { http, element, button, tick } = await open('/admin/publications/9', DRAFT);

    const panel = element().querySelector('app-publication-status-panel')!;
    expect(Array.from(panel.querySelectorAll('button')).map((b) => b.textContent?.trim())).toEqual([
      'Publier maintenant',
      'Programmer',
      'Passer en relecture',
    ]);
    button('Publier maintenant').click();
    await tick();

    const change = http.expectOne({ method: 'POST', url: `${API}/9/status` });
    expect(change.request.body).toEqual({ status: 'PUBLISHED', publishedAt: undefined });
    change.flush({
      ...DRAFT,
      status: 'PUBLISHED',
      publishedAt: '2026-10-03T10:00:00Z',
      slugLocked: true,
    });
    await tick();

    expect(panel.querySelector('app-status-badge')?.textContent?.trim()).toBe('Publiée');
    expect(element().querySelector('a[href="/articles/suivi-multi-objets"]')).not.toBeNull();
    expect(TestBed.inject(Toaster).toasts()[0].message).toContain('publiée');
  });

  it('asks a future date to schedule, and blocks transitions while the input is unsaved', async () => {
    const { http, element, button, type, tick } = await open('/admin/publications/9', DRAFT);

    button('Programmer').click();
    await tick();
    const when = element().querySelector<HTMLInputElement>('#publication-programmation')!;
    expect(document.activeElement).toBe(when);
    when.value = '2020-01-01T10:00';
    when.dispatchEvent(new Event('input'));
    element()
      .querySelector('app-publication-status-panel form')!
      .dispatchEvent(new Event('submit', { cancelable: true }));
    await tick();
    http.expectNone({ method: 'POST', url: `${API}/9/status` });
    expect(element().querySelector('#publication-programmation-erreur')?.textContent).toContain(
      'Choisissez un moment à venir.',
    );

    button('Annuler').click();
    await tick();
    type('publication-titre', 'Nouveau titre');
    await tick();
    expect(button('Publier maintenant').disabled).toBe(true);
    expect(element().textContent).toContain('Enregistrez d’abord vos modifications');
  });

  it('freezes the slug of a published publication', async () => {
    const { element } = await open('/admin/publications/9', {
      ...DRAFT,
      status: 'PUBLISHED',
      slugLocked: true,
    });

    expect(element().querySelector<HTMLInputElement>('#publication-slug')?.readOnly).toBe(true);
    expect(element().textContent).toContain('Figé : cette publication a déjà été publiée');
  });

  it('says why a slug change is refused when the publication was published meanwhile', async () => {
    const { http, element, type, submit, tick } = await open('/admin/publications/9', DRAFT);

    type('publication-slug', 'suivi-temps-reel');
    await submit();
    http
      .expectOne({ method: 'PUT', url: `${API}/9` })
      .flush({ status: 409, code: 'SLUG_LOCKED' }, { status: 409, statusText: 'Conflict' });
    await tick();
    expect(element().querySelector('#publication-slug-erreur')?.textContent).toContain(
      'son slug ne peut plus changer',
    );
  });
});
