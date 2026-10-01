import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { Project, ProjectSummary } from '../../../core/api/api-types';
import { Page } from '../../../core/api/page';
import { SITE_URL } from '../../../core/seo/site-config';
import { ProjectDetail } from './project-detail';
import { ProjectList } from './project-list';

const SUMMARY: ProjectSummary = {
  title: 'Détection de changements',
  slug: 'detection',
  shortDescription: 'Traitement d’images satellitaires.',
  stage: 'IN_PROGRESS',
  startDate: '2024-01-01',
  endDate: null,
  featured: true,
  cover: null,
  technologies: [{ name: 'Spring Boot', slug: 'spring-boot' }],
};

const PROJECT: Project = {
  ...SUMMARY,
  descriptionMarkdown: '## Contexte\n\nUn service.',
  repositoryUrl: 'https://git.example/detection',
  demoUrl: null,
  cover: { url: '/api/public/media/cover', width: 1200, height: 750, altText: 'Carte' },
  screenshots: [
    {
      image: { url: '/api/public/media/capture', width: 800, height: 600, altText: 'Tableau' },
      caption: 'Tableau de bord.',
    },
  ],
};

function page(
  content: ProjectSummary[],
  totalElements = content.length,
  number = 0,
): Page<ProjectSummary> {
  const totalPages = Math.ceil(totalElements / 10);
  return {
    content,
    page: number,
    size: 10,
    totalElements,
    totalPages,
    first: number === 0,
    last: number >= totalPages - 1,
  };
}

async function setUp(url: string) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter(
        [
          { path: 'projects', component: ProjectList },
          { path: 'projects/:slug', component: ProjectDetail },
        ],
        withComponentInputBinding(),
      ),
      { provide: SITE_URL, useValue: 'https://blek.example' },
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  const http = TestBed.inject(HttpTestingController);
  const text = () => (harness.routeNativeElement as HTMLElement).textContent?.replace(/\s+/g, ' ');
  const element = () => harness.routeNativeElement as HTMLElement;
  return { harness, http, element, text };
}

const settle = async (harness: RouterTestingHarness) => {
  await harness.fixture.whenStable();
  harness.detectChanges();
};

describe('ProjectList', () => {
  afterEach(() =>
    TestBed.inject(DOCUMENT)
      .head.querySelectorAll('meta, link[rel="canonical"], script[type="application/ld+json"]')
      .forEach((node) => node.remove()),
  );

  it('asks the api for the page and the filter of the url', async () => {
    const { http, harness, element } = await setUp('/projects?technology=spring-boot&page=2');

    const request = http.expectOne((r) => r.url === '/api/public/projects');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('technology')).toBe('spring-boot');
    request.flush(page([SUMMARY], 11, 1));
    await settle(harness);

    expect(element().querySelector('h1')?.textContent).toBe('Projets');
    expect(element().textContent).toContain('Technologie');
    expect(element().querySelector('strong')?.textContent).toBe('Spring Boot');
    expect(element().querySelector('h2 a')?.getAttribute('href')).toBe('/projects/detection');
    const remove = Array.from(element().querySelectorAll('a')).find((a) =>
      a.textContent?.includes('Retirer le filtre'),
    );
    expect(remove?.getAttribute('href')).toBe('/projects');
    expect(element().querySelector('a[aria-current="page"]')?.textContent?.trim()).toBe('Page 2');
    // Vue filtrée : pas d'indexation, adresse canonique de la vue
    const document = TestBed.inject(DOCUMENT);
    expect(document.head.querySelector('meta[name="robots"]')?.getAttribute('content')).toBe(
      'noindex',
    );
    expect(document.head.querySelector('link[rel="canonical"]')?.getAttribute('href')).toBe(
      'https://blek.example/projects?technology=spring-boot&page=2',
    );
  });

  it('reads an invalid page as the first one', async () => {
    const { http } = await setUp('/projects?page=abc');

    expect(http.expectOne((r) => r.url === '/api/public/projects').request.params.get('page')).toBe(
      '0',
    );
  });

  it('says when nothing is published, when a filter matches nothing and when the page is past the end', async () => {
    let context = await setUp('/projects');
    context.http.expectOne((r) => r.url === '/api/public/projects').flush(page([]));
    await settle(context.harness);
    expect(context.text()).toContain('Aucun projet n’est encore publié.');
    expect(context.text()).toContain('0 projet');

    TestBed.resetTestingModule();
    context = await setUp('/projects?technology=cobol');
    context.http.expectOne((r) => r.url === '/api/public/projects').flush(page([]));
    await settle(context.harness);
    expect(context.text()).toContain('Aucun projet publié n’utilise cette technologie.');

    TestBed.resetTestingModule();
    context = await setUp('/projects?page=9');
    context.http.expectOne((r) => r.url === '/api/public/projects').flush(page([], 3, 8));
    await settle(context.harness);
    expect(context.text()).toContain('Cette page n’existe pas');
  });

  it('offers to retry when the api fails', async () => {
    const { http, harness, element } = await setUp('/projects');
    http
      .expectOne((r) => r.url === '/api/public/projects')
      .flush(null, { status: 500, statusText: 'Erreur' });
    await settle(harness);

    const alert = element().querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('La liste des projets n’a pas pu être chargée.');
    alert?.querySelector('button')?.click();
    harness.detectChanges();
    http.expectOne((r) => r.url === '/api/public/projects').flush(page([SUMMARY]));
    await settle(harness);
    expect(element().querySelector('[role="alert"]')).toBeNull();
  });
});

describe('ProjectDetail', () => {
  afterEach(() =>
    TestBed.inject(DOCUMENT)
      .head.querySelectorAll('meta, link[rel="canonical"], script[type="application/ld+json"]')
      .forEach((node) => node.remove()),
  );

  it('renders the project, its sheet, its description and its figures', async () => {
    const { http, harness, element, text } = await setUp('/projects/detection');
    http.expectOne('/api/public/projects/detection').flush(PROJECT);
    await settle(harness);

    expect(element().querySelector('h1')?.textContent).toBe('Détection de changements');
    const sheet = Array.from(element().querySelectorAll('dl > div')).map((row) => [
      row.querySelector('dt')?.textContent?.trim(),
      row.querySelector('dd')?.textContent?.trim(),
    ]);
    expect(sheet.slice(0, 2)).toEqual([
      ['État', 'En cours'],
      ['Période', 'depuis janvier 2024'],
    ]);
    expect(element().querySelector('a.external')?.getAttribute('href')).toBe(
      'https://git.example/detection',
    );
    expect(text()).not.toContain('Voir la démonstration');
    expect(element().querySelector('h2')?.textContent).toBe('Contexte');
    const figure = element().querySelector('ol figure');
    expect(figure?.querySelector('img')?.getAttribute('alt')).toBe('Tableau');
    expect(figure?.querySelector('figcaption')?.textContent?.replace(/\s+/g, ' ').trim()).toBe(
      'Figure 1 — Tableau de bord.',
    );

    const document = TestBed.inject(DOCUMENT);
    expect(document.title).toBe('Détection de changements — Blek Ngossanga');
    expect(document.head.querySelector('meta[property="og:image"]')?.getAttribute('content')).toBe(
      'https://blek.example/api/public/media/cover',
    );
    expect(JSON.parse(document.getElementById('json-ld-page')?.textContent ?? '')).toMatchObject({
      '@type': 'CreativeWork',
      name: 'Détection de changements',
      url: 'https://blek.example/projects/detection',
      keywords: 'Spring Boot',
    });
  });

  it('keeps a coherent page without cover, links or screenshots', async () => {
    const { http, harness, element } = await setUp('/projects/detection');
    http
      .expectOne('/api/public/projects/detection')
      .flush({ ...PROJECT, cover: null, repositoryUrl: null, screenshots: [] });
    await settle(harness);

    expect(element().querySelectorAll('img, figure, a.external, #captures')).toHaveLength(0);
  });

  it('shows a not found page for an unknown slug, without indexing', async () => {
    const { http, harness, element } = await setUp('/projects/inconnu');
    http
      .expectOne('/api/public/projects/inconnu')
      .flush(
        { status: 404, code: 'RESOURCE_NOT_FOUND' },
        { status: 404, statusText: 'Introuvable' },
      );
    await settle(harness);

    expect(element().querySelector('h1')?.textContent).toBe('Projet introuvable');
    expect(element().querySelector('a[href="/projects"]')).not.toBeNull();
    expect(
      TestBed.inject(DOCUMENT).head.querySelector('meta[name="robots"]')?.getAttribute('content'),
    ).toBe('noindex');
  });
});
