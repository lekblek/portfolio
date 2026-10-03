import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { AdminPublicationSummary, AdminSeries } from '../../../../core/api/api-types';
import { Toaster } from '../../../../shared/ui/toast';
import { SeriesFormPage } from './series-form-page';
import { SeriesListPage } from './series-list-page';

const API = '/api/admin/series';

const SERIES: AdminSeries = {
  id: 5,
  title: 'Vision appliquée',
  slug: 'vision-appliquee',
  slugLocked: false,
  descriptionMarkdown: 'De l’annotation au déploiement.',
  coverMediaId: null,
  chapters: [
    { position: 1, publicationId: 11, title: 'Annoter', slug: 'annoter', status: 'PUBLISHED' },
    { position: 2, publicationId: 12, title: 'Mesurer', slug: 'mesurer', status: 'DRAFT' },
  ],
};

const ARTICLES: AdminPublicationSummary[] = [
  {
    id: 11,
    type: 'ARTICLE',
    title: 'Annoter',
    slug: 'annoter',
    status: 'PUBLISHED',
    publishedAt: null,
    featured: false,
    updatedAt: '2026-10-01T08:00:00Z',
  },
  {
    id: 12,
    type: 'ARTICLE',
    title: 'Mesurer',
    slug: 'mesurer',
    status: 'DRAFT',
    publishedAt: null,
    featured: false,
    updatedAt: '2026-10-01T08:00:00Z',
  },
  {
    id: 13,
    type: 'ARTICLE',
    title: 'Détecter',
    slug: 'detecter',
    status: 'PUBLISHED',
    publishedAt: null,
    featured: false,
    updatedAt: '2026-10-01T08:00:00Z',
  },
];

function page<T>(content: T[]) {
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

async function open(url: string, series?: AdminSeries) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter(
        [
          { path: 'admin/series', component: SeriesListPage },
          { path: 'admin/series/new', component: SeriesFormPage },
          { path: 'admin/series/:id', component: SeriesFormPage },
        ],
        withComponentInputBinding(),
      ),
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  const http = TestBed.inject(HttpTestingController);
  for (const request of http.match((r) => r.url === '/api/admin/publications')) {
    expect(request.request.params.get('type')).toBe('ARTICLE');
    request.flush(page(ARTICLES));
  }
  if (series) {
    http.expectOne(`${API}/${series.id}`).flush(series);
  }
  const tick = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    harness.detectChanges();
  };
  await tick();
  const element = () => harness.routeNativeElement as HTMLElement;
  const button = (name: string) =>
    Array.from(element().querySelectorAll('button')).find(
      (candidate) => candidate.textContent?.replace(/\s+/g, ' ').trim() === name,
    )!;
  return { harness, http, element, tick, button };
}

describe('SeriesListPage', () => {
  it('lists the series with their number of chapters', async () => {
    const { http, element, tick } = await open('/admin/series');
    http
      .expectOne((r) => r.url === API)
      .flush(
        page([{ id: 5, title: 'Vision appliquée', slug: 'vision-appliquee', chapterCount: 2 }]),
      );
    await tick();

    const row = element().querySelector('tbody tr')!;
    expect(row.querySelector('th')?.textContent).toContain('Vision appliquée');
    expect(row.querySelector('.data-table-number')?.textContent?.trim()).toBe('2');
  });
});

describe('SeriesFormPage', () => {
  it('creates a series, then opens its page to add chapters', async () => {
    const { http, element, tick } = await open('/admin/series/new');
    expect(element().textContent).toContain('Les chapitres s’ajoutent une fois la série créée.');
    const title = element().querySelector<HTMLInputElement>('#serie-titre')!;
    title.value = 'Vision appliquée';
    title.dispatchEvent(new Event('input'));
    element()
      .querySelector('form')!
      .dispatchEvent(new Event('submit', { cancelable: true }));
    await tick();

    const post = http.expectOne({ method: 'POST', url: API });
    expect(post.request.body).toEqual({
      title: 'Vision appliquée',
      slug: undefined,
      descriptionMarkdown: '',
      coverMediaId: undefined,
    });
    post.flush({ ...SERIES, chapters: [] });
    await tick();
    await tick();
    expect(TestBed.inject(Router).url).toBe('/admin/series/5');
  });

  it('adds, reorders and saves the chapters as one list', async () => {
    const { http, element, tick, button } = await open('/admin/series/5', SERIES);

    const options = Array.from(element().querySelectorAll('#chapitre-ajout option')).map((option) =>
      option.textContent?.trim(),
    );
    expect(options).toEqual(['Choisir un article', 'Détecter']);
    const select = element().querySelector<HTMLSelectElement>('#chapitre-ajout')!;
    select.value = '13';
    select.dispatchEvent(new Event('change'));
    await tick();
    button('Ajouter').click();
    await tick();
    button('Monter le chapitre «\u00a0Détecter\u00a0»'.replace(/\u00a0/g, ' ')).click();
    await tick();

    button('Enregistrer les chapitres').click();
    await tick();
    const put = http.expectOne({ method: 'PUT', url: `${API}/5/chapters` });
    expect(put.request.body).toEqual({ publicationIds: [11, 13, 12] });
    put.flush(SERIES);
    await tick();
    expect(TestBed.inject(Toaster).toasts()[0].message).toContain('Chapitres de');
  });

  it('explains an article already filed in another series', async () => {
    const { http, element, tick, button } = await open('/admin/series/5', SERIES);
    button('Retirer le chapitre «\u00a0Mesurer\u00a0»'.replace(/\u00a0/g, ' ')).click();
    await tick();
    button('Enregistrer les chapitres').click();
    await tick();
    http
      .expectOne({ method: 'PUT', url: `${API}/5/chapters` })
      .flush(
        { status: 409, code: 'ARTICLE_ALREADY_IN_SERIES' },
        { status: 409, statusText: 'Conflict' },
      );
    await tick();

    expect(element().querySelector('[role="alert"]')?.textContent).toContain(
      'appartient déjà à une autre série',
    );
  });
});
