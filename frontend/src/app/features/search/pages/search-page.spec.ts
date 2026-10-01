import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { SearchResult } from '../../../core/api/api-types';
import { SITE_URL } from '../../../core/seo/site-config';
import { searchResultPath } from '../ui/search-result-entry';
import { SearchPage } from './search-page';

const RESULTS: SearchResult[] = [
  {
    type: 'ARTICLE',
    title: 'Détecter des changements',
    slug: 'detecter',
    summary: 'Une chaîne de traitement.',
    publishedAt: '2026-09-17T08:00:00Z',
  },
  {
    type: 'NEWS',
    title: 'Lancement',
    slug: 'lancement',
    summary: 'Le site est en ligne.',
    publishedAt: '2026-09-20T08:00:00Z',
  },
  {
    type: 'PROJECT',
    title: 'Portfolio',
    slug: 'portfolio',
    summary: 'Spring Boot et Angular.',
    publishedAt: null,
  },
];

function page(content: SearchResult[], totalElements = content.length, number = 0) {
  return {
    content,
    page: number,
    size: 10,
    totalElements,
    totalPages: Math.ceil(totalElements / 10),
    first: number === 0,
    last: true,
  };
}

async function setUp(url: string) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter([{ path: 'search', component: SearchPage }], withComponentInputBinding()),
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

describe('searchResultPath', () => {
  it('leads each result to the page of its type', () => {
    expect(RESULTS.map(searchResultPath)).toEqual([
      '/articles/detecter',
      '/news/lancement',
      '/projects/portfolio',
    ]);
  });
});

describe('SearchPage', () => {
  afterEach(() =>
    TestBed.inject(DOCUMENT)
      .head.querySelectorAll('meta, link[rel="canonical"], script[type="application/ld+json"]')
      .forEach((node) => node.remove()),
  );

  it('shows mixed results, announces their number and never asks to be indexed', async () => {
    const { http, element, settle } = await setUp('/search?q=%20d%C3%A9tection%20&page=2');

    const request = http.expectOne((r) => r.url === '/api/public/search');
    expect(request.request.params.get('q')).toBe('détection');
    expect(request.request.params.get('page')).toBe('1');
    request.flush(page(RESULTS, 13, 1));
    await settle();

    expect(element().querySelector<HTMLInputElement>('input[type="search"]')?.value).toBe(
      ' détection ',
    );
    expect(element().querySelector('[role="status"]')?.textContent?.trim()).toBe(
      '13 résultats pour « détection »',
    );
    expect(element().querySelector('ol')?.getAttribute('start')).toBe('11');
    const links = Array.from(element().querySelectorAll('ol h2 a')).map((a) =>
      a.getAttribute('href'),
    );
    expect(links).toEqual(['/articles/detecter', '/news/lancement', '/projects/portfolio']);
    expect(element().querySelector('ol li:last-child time')).toBeNull();

    const document = TestBed.inject(DOCUMENT);
    expect(document.title).toBe('Recherche : détection — Blek Ngossanga');
    expect(document.querySelector('meta[name="robots"]')?.getAttribute('content')).toBe('noindex');
  });

  it('asks nothing without text to search', async () => {
    const { http, element, settle } = await setUp('/search?q=%20%20');
    await settle();

    http.expectNone((r) => r.url === '/api/public/search');
    expect(element().querySelector('[role="status"]')?.textContent?.trim()).toBe('');
    expect(element().querySelector('ol')).toBeNull();
  });

  it('refuses more than 200 characters before calling the api', async () => {
    const { http, element, settle } = await setUp(`/search?q=${'a'.repeat(201)}`);
    await settle();

    http.expectNone((r) => r.url === '/api/public/search');
    const field = element().querySelector('input[type="search"]');
    expect(field?.getAttribute('maxlength')).toBe('200');
    expect(field?.getAttribute('aria-invalid')).toBe('true');
    expect(field?.getAttribute('aria-describedby')).toContain('search-q-error');
    expect(element().querySelector('#search-q-error')?.textContent).toContain('200 caractères');
  });

  it('says when nothing matches', async () => {
    const { http, element, settle } = await setUp('/search?q=introuvable');
    http.expectOne((r) => r.url === '/api/public/search').flush(page([]));
    await settle();

    expect(element().querySelector('[role="status"]')?.textContent?.trim()).toBe(
      'Aucun résultat pour « introuvable »',
    );
    expect(element().textContent).toContain('Aucun article, aucune actualité ni aucun projet');
  });

  it('turns a submission into a navigation, without the page reloading', async () => {
    const { http, element } = await setUp('/search');
    const router = TestBed.inject(Router);
    const field = element().querySelector<HTMLInputElement>('input[type="search"]');
    field!.value = '  spring  ';

    const submit = new SubmitEvent('submit', { cancelable: true });
    element().querySelector('form')!.dispatchEvent(submit);

    expect(submit.defaultPrevented).toBe(true);
    await vi.waitFor(() => expect(router.url).toBe('/search?q=spring'));
    await vi.waitFor(() => http.expectOne((r) => r.url === '/api/public/search').flush(page([])));
  });
});
