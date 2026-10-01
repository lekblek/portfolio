import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { Series, SeriesSummary } from '../../../core/api/api-types';
import { SITE_URL } from '../../../core/seo/site-config';
import { SeriesDetail } from './series-detail';
import { SeriesList } from './series-list';

const SERIES: Series = {
  title: 'Spring Boot de zéro à la production',
  slug: 'spring-boot',
  descriptionMarkdown: 'Une **série** en plusieurs étapes.',
  cover: null,
  chapters: [
    {
      position: 1,
      title: 'Construire une API',
      slug: 'api',
      summary: 'Premier chapitre.',
      publishedAt: '2026-08-25T08:00:00Z',
      readingTimeMinutes: 6,
    },
    {
      position: 2,
      title: 'Mettre en production',
      slug: 'production',
      summary: 'Second chapitre.',
      publishedAt: '2026-09-02T08:00:00Z',
      readingTimeMinutes: 9,
    },
  ],
};

const SUMMARY: SeriesSummary = {
  title: SERIES.title,
  slug: SERIES.slug,
  descriptionMarkdown: SERIES.descriptionMarkdown,
  cover: null,
  chapterCount: 2,
};

async function setUp(url: string) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter(
        [
          { path: 'series', component: SeriesList },
          { path: 'series/:slug', component: SeriesDetail },
        ],
        withComponentInputBinding(),
      ),
      { provide: SITE_URL, useValue: 'https://blek.example' },
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  const element = () => harness.routeNativeElement as HTMLElement;
  const settle = async () => {
    await harness.fixture.whenStable();
    harness.detectChanges();
  };
  return { http: TestBed.inject(HttpTestingController), element, settle };
}

describe('SeriesList', () => {
  it('lists the series with their number of chapters and a plain excerpt', async () => {
    const { http, element, settle } = await setUp('/series');
    http
      .expectOne((r) => r.url === '/api/public/series')
      .flush({
        content: [SUMMARY],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
      });
    await settle();

    expect(element().querySelector('h1')?.textContent).toBe('Séries');
    expect(element().querySelector('h2 a')?.getAttribute('href')).toBe('/series/spring-boot');
    expect(element().textContent).toContain('2 chapitres');
    expect(element().querySelector('.register-summary')?.textContent?.trim()).toBe(
      'Une série en plusieurs étapes.',
    );
  });
});

describe('SeriesDetail', () => {
  afterEach(() =>
    TestBed.inject(DOCUMENT)
      .head.querySelectorAll('meta, link[rel="canonical"], script[type="application/ld+json"]')
      .forEach((node) => node.remove()),
  );

  it('lists the chapters in reading order, with the total reading time', async () => {
    const { http, element, settle } = await setUp('/series/spring-boot');
    http.expectOne('/api/public/series/spring-boot').flush(SERIES);
    await settle();

    expect(element().querySelector('h1')?.textContent).toBe(SERIES.title);
    const chapters = Array.from(element().querySelectorAll('ol > li h3 a')).map((link) => [
      link.textContent,
      link.getAttribute('href'),
    ]);
    expect(chapters).toEqual([
      ['Construire une API', '/articles/api'],
      ['Mettre en production', '/articles/production'],
    ]);
    expect(element().textContent).toContain('Chapitre 2');
    expect(element().querySelector('dl')?.textContent).toContain('15\u00a0min de lecture');
    const jsonLd = TestBed.inject(DOCUMENT).getElementById('json-ld-page')?.textContent ?? '';
    expect(JSON.parse(jsonLd)).toMatchObject({
      '@type': 'CreativeWorkSeries',
      hasPart: [{ position: 1 }, { position: 2 }],
    });
  });

  it('shows a not found page for an unknown series', async () => {
    const { http, element, settle } = await setUp('/series/inconnue');
    http
      .expectOne('/api/public/series/inconnue')
      .flush({ status: 404 }, { status: 404, statusText: 'Introuvable' });
    await settle();

    expect(element().querySelector('h1')?.textContent).toBe('Série introuvable');
  });
});
