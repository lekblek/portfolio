import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { Publication, PublicationSummary } from '../../../core/api/api-types';
import { SITE_URL } from '../../../core/seo/site-config';
import { PublicationDetail } from './publication-detail';
import { PublicationList } from './publication-list';

@Component({ template: '<h1>Autre page</h1>' })
class Elsewhere {}

const SUMMARY: PublicationSummary = {
  type: 'ARTICLE',
  title: 'Détecter des changements',
  slug: 'detecter',
  summary: 'Une chaîne de traitement.',
  publishedAt: '2026-09-17T08:00:00Z',
  featured: false,
  readingTimeMinutes: 4,
  cover: null,
  category: { name: 'Vision par ordinateur', slug: 'vision' },
  tags: [{ name: 'PyTorch', slug: 'pytorch' }],
};

const ARTICLE: Publication = {
  ...SUMMARY,
  contentMarkdown:
    '## Contexte\n\nTexte[^1].\n\n## Résultats\n\n```java\nclass A {}\n```\n\n[^1]: Une note.',
  seoTitle: 'Titre pour les moteurs',
  seoDescription: null,
};

async function setUp(url: string) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter(
        [
          { path: 'articles', component: PublicationList, data: { type: 'ARTICLE' } },
          { path: 'articles/:slug', component: PublicationDetail, data: { type: 'ARTICLE' } },
          { path: 'news', component: PublicationList, data: { type: 'NEWS' } },
          { path: 'news/:slug', component: Elsewhere },
        ],
        withComponentInputBinding(),
      ),
      { provide: SITE_URL, useValue: 'https://blek.example' },
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  const http = TestBed.inject(HttpTestingController);
  const element = () => harness.routeNativeElement as HTMLElement;
  const settle = async () => {
    await harness.fixture.whenStable();
    harness.detectChanges();
  };
  return { harness, http, element, settle };
}

/** Place dans une série demandée par la page d'article : réponse 404 de l'API (article hors série). */
function outsideAnySeries(http: HttpTestingController, slug: string): void {
  http
    .expectOne(`/api/public/publications/${slug}/series`)
    .flush({ status: 404 }, { status: 404, statusText: 'Introuvable' });
}

function page(content: PublicationSummary[], totalElements = content.length) {
  return {
    content,
    page: 0,
    size: 10,
    totalElements,
    totalPages: Math.ceil(totalElements / 10),
    first: true,
    last: true,
  };
}

describe('PublicationList', () => {
  afterEach(() =>
    TestBed.inject(DOCUMENT)
      .head.querySelectorAll('meta, link[rel="canonical"], script[type="application/ld+json"]')
      .forEach((node) => node.remove()),
  );

  it('asks for the publications of its type, with the filters of the url', async () => {
    const { http, element, settle } = await setUp('/articles?category=vision&tag=pytorch');

    const request = http.expectOne((r) => r.url === '/api/public/publications');
    expect(request.request.params.get('type')).toBe('ARTICLE');
    expect(request.request.params.get('category')).toBe('vision');
    expect(request.request.params.get('tag')).toBe('pytorch');
    request.flush(page([SUMMARY]));
    await settle();

    expect(element().querySelector('h1')?.textContent).toBe('Articles');
    const filters = element().querySelector('ul[aria-label="Filtres actifs"]');
    expect(filters?.textContent).toContain('Vision par ordinateur');
    const removals = Array.from(filters?.querySelectorAll('a') ?? []).map((a) => [
      a.textContent?.trim(),
      a.getAttribute('href'),
    ]);
    expect(removals).toEqual([
      ['Retirer ce filtre de catégorie', '/articles?tag=pytorch'],
      ['Retirer ce filtre de tag', '/articles?category=vision'],
    ]);
    expect(element().querySelector('h2 a')?.getAttribute('href')).toBe('/articles/detecter');
    expect(element().querySelector('time')?.getAttribute('datetime')).toBe('2026-09-17');
  });

  it('speaks of news, in the feminine, when nothing is published', async () => {
    const { http, element, settle } = await setUp('/news');

    const request = http.expectOne((r) => r.url === '/api/public/publications');
    expect(request.request.params.get('type')).toBe('NEWS');
    request.flush(page([]));
    await settle();

    expect(element().querySelector('h1')?.textContent).toBe('Actualités');
    expect(element().textContent).toContain('Aucune actualité n’est encore publiée.');
    expect(element().textContent).toContain('0 actualité');
  });
});

describe('PublicationDetail', () => {
  afterEach(() =>
    TestBed.inject(DOCUMENT)
      .head.querySelectorAll('meta, link[rel="canonical"], script[type="application/ld+json"]')
      .forEach((node) => node.remove()),
  );

  it('renders the article with its sheet, table of contents, notes and copy buttons', async () => {
    const { http, element, settle } = await setUp('/articles/detecter');
    http.expectOne('/api/public/publications/detecter').flush(ARTICLE);
    outsideAnySeries(http, 'detecter');
    await settle();

    expect(element().querySelector('h1')?.textContent).toBe('Détecter des changements');
    expect(element().querySelector('dl')?.textContent).toContain('17 septembre 2026');
    expect(element().querySelector('dl')?.textContent).toContain('4\u00a0min de lecture');
    const tocLinks = Array.from(element().querySelectorAll('.article-toc-side nav a')).map((link) =>
      link.getAttribute('href'),
    );
    expect(tocLinks).toEqual(['/articles/detecter#contexte', '/articles/detecter#resultats']);
    expect(element().querySelector('details summary')?.textContent).toBe('Sommaire');
    expect(element().querySelector('[role="note"]')?.textContent).toContain('Une note.');
    expect(element().querySelector('app-code-copy button[data-code-copy]')).not.toBeNull();

    const document = TestBed.inject(DOCUMENT);
    expect(document.title).toBe('Titre pour les moteurs — Blek Ngossanga');
    expect(JSON.parse(document.getElementById('json-ld-page')?.textContent ?? '')).toMatchObject({
      '@type': 'Article',
      headline: 'Titre pour les moteurs',
      datePublished: '2026-09-17T08:00:00Z',
      articleSection: 'Vision par ordinateur',
      url: 'https://blek.example/articles/detecter',
    });
  });

  it('shows no table of contents for fewer than two sections', async () => {
    const { http, element, settle } = await setUp('/articles/detecter');
    http
      .expectOne('/api/public/publications/detecter')
      .flush({ ...ARTICLE, contentMarkdown: '## Seul titre\n\nTexte.' });
    outsideAnySeries(http, 'detecter');
    await settle();

    expect(element().querySelector('app-table-of-contents, details')).toBeNull();
  });

  it('sends a news item opened as an article to its own address', async () => {
    const { http, harness } = await setUp('/articles/detecter');
    http.expectOne('/api/public/publications/detecter').flush({ ...ARTICLE, type: 'NEWS' });
    outsideAnySeries(http, 'detecter');
    await harness.fixture.whenStable();

    expect(TestBed.inject(Router).url).toBe('/news/detecter');
  });

  it('shows a not found page for an unknown slug', async () => {
    const { http, element, settle } = await setUp('/articles/inconnu');
    http
      .expectOne('/api/public/publications/inconnu')
      .flush({ status: 404 }, { status: 404, statusText: 'Introuvable' });
    outsideAnySeries(http, 'inconnu');
    await settle();

    expect(element().querySelector('h1')?.textContent).toBe('Article introuvable');
    expect(element().querySelector('a[href="/articles"]')?.textContent).toBe(
      'Voir tous les articles',
    );
  });
});
