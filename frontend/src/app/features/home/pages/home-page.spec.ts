import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import {
  Profile,
  ProjectSummary,
  PublicationSummary,
  SeriesSummary,
} from '../../../core/api/api-types';
import { SITE_NAME, SITE_URL } from '../../../core/seo/site-config';
import { HomePage } from './home-page';

const PROFILE: Profile = {
  displayName: 'Blek Gedeon Ngossanga',
  professionalTitle: 'Ingénieur logiciel',
  shortBio: 'Je conçois des applications web et des systèmes de vision par ordinateur.',
  aboutMarkdown: null,
  publicLocation: 'Tanger, Maroc',
  publicEmail: 'contact@blek.example',
  avatar: null,
  cv: { url: '/api/public/media/cv', sizeBytes: 1_258_291 },
  links: [{ label: 'GitHub', url: 'https://github.example/blek' }],
  skillGroups: [],
  experiences: [],
  educations: [],
  certifications: [],
};

function project(slug: string, featured: boolean, technologies: string[]): ProjectSummary {
  return {
    title: `Projet ${slug}`,
    slug,
    shortDescription: 'Un projet.',
    stage: 'COMPLETED',
    startDate: '2024-01-01',
    endDate: '2024-06-30',
    featured,
    cover: null,
    technologies: technologies.map((name) => ({ name, slug: name.toLowerCase() })),
  };
}

function publication(
  type: 'ARTICLE' | 'NEWS',
  slug: string,
  publishedAt: string,
): PublicationSummary {
  return {
    type,
    title: `Publication ${slug}`,
    slug,
    summary: 'Un résumé.',
    publishedAt,
    featured: false,
    readingTimeMinutes: 3,
    cover: null,
    category: null,
    tags: [],
  };
}

const SERIES: SeriesSummary = {
  title: 'Une série',
  slug: 'une-serie',
  descriptionMarkdown: 'Plusieurs chapitres.',
  chapterCount: 2,
  cover: null,
};

function page<T>(content: T[], totalElements = content.length) {
  return {
    content,
    page: 0,
    size: 3,
    totalElements,
    totalPages: Math.ceil(totalElements / 3),
    first: true,
    last: totalElements <= 3,
  };
}

interface Responses {
  profile?: Profile | 404 | 500;
  featured?: ProjectSummary[];
  projects?: { content: ProjectSummary[]; total: number };
  articles?: PublicationSummary[] | 500;
  series?: SeriesSummary[];
  news?: PublicationSummary[];
}

async function render(responses: Responses) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter([{ path: '', component: HomePage }]),
      { provide: SITE_URL, useValue: 'https://blek.example' },
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl('/');
  const http = TestBed.inject(HttpTestingController);

  const profile = http.expectOne('/api/public/profile');
  const answer = responses.profile ?? PROFILE;
  if (typeof answer === 'number') {
    profile.flush({ status: answer }, { status: answer, statusText: 'Erreur' });
  } else {
    profile.flush(answer);
  }
  const projects = http.match((r) => r.url === '/api/public/projects');
  expect(projects.length).toBe(2);
  for (const request of projects) {
    expect(request.request.params.get('size')).toBe('3');
    if (request.request.params.get('featured') === 'true') {
      request.flush(page(responses.featured ?? []));
    } else {
      const all = responses.projects ?? { content: [], total: 0 };
      request.flush(page(all.content, all.total));
    }
  }
  for (const request of http.match((r) => r.url === '/api/public/publications')) {
    const type = request.request.params.get('type');
    const answer = type === 'ARTICLE' ? (responses.articles ?? []) : (responses.news ?? []);
    if (answer === 500) {
      request.flush({ status: 500 }, { status: 500, statusText: 'Erreur' });
    } else {
      request.flush(page(answer));
    }
  }
  http.expectOne((r) => r.url === '/api/public/series').flush(page(responses.series ?? []));
  http.verify();

  await harness.fixture.whenStable();
  harness.detectChanges();
  return harness.routeNativeElement as HTMLElement;
}

function headings(element: HTMLElement, level: string): string[] {
  return Array.from(element.querySelectorAll(level)).map((h) => h.textContent?.trim() ?? '');
}

describe('HomePage', () => {
  afterEach(() =>
    TestBed.inject(DOCUMENT)
      .head.querySelectorAll('meta, link[rel="canonical"], script[type="application/ld+json"]')
      .forEach((node) => node.remove()),
  );

  it('assembles the statement, the title block and a zone per published content', async () => {
    const element = await render({
      featured: [project('vitrine', true, ['Java', 'Angular'])],
      projects: { content: [project('vitrine', true, ['Java', 'Angular'])], total: 4 },
      articles: [publication('ARTICLE', 'premier', '2026-09-20T08:00:00Z')],
      series: [SERIES],
      news: [publication('NEWS', 'lancement', '2026-09-25T08:00:00Z')],
    });

    expect(headings(element, 'h1')).toEqual(['Blek Gedeon Ngossanga']);
    expect(headings(element, 'h2')).toEqual([
      'Projets mis en avant',
      'Derniers articles',
      'Séries',
      'Dernières actualités',
      'Contact',
    ]);
    expect(headings(element, 'h3')).toEqual([
      'Projet vitrine',
      'Publication premier',
      'Une série',
      'Publication lancement',
    ]);
    const facts = Array.from(element.querySelectorAll('table tr')).map((row) =>
      Array.from(row.children).map((cell) => cell.textContent?.trim()),
    );
    expect(facts).toEqual([
      ['Rôle', 'Ingénieur logiciel'],
      ['Lieu', 'Tanger, Maroc'],
      ['Pile', 'Java, Angular'],
      ['Projets publiés', '4'],
      ['Publications', '2'],
      ['Dernière publication', '25 septembre 2026'],
    ]);
    expect(element.querySelector('a[href="/projects"]')?.textContent).toBe('Tous les projets (4)');
    expect(element.querySelector('a[href="/news"]')?.textContent).toBe('Toutes les actualités (1)');
    expect(element.querySelector('a[href="/contact"]')?.textContent).toBe('écrire un message');
    expect(element.querySelector('ul[aria-label="Liens professionnels"] a')?.textContent).toContain(
      'GitHub',
    );
    // L'adresse électronique publique n'est jamais affichée (D-EA)
    expect(element.textContent).not.toContain('contact@blek.example');
    expect(TestBed.inject(DOCUMENT).title).toBe(
      'Blek Ngossanga — Software Engineering, AI Vision & Research',
    );
  });

  it('falls back to the first projects without a featured one, and hides empty zones', async () => {
    const element = await render({
      projects: { content: [project('premier', false, ['Python'])], total: 1 },
    });

    expect(headings(element, 'h2')).toEqual(['Projets', 'Contact']);
    expect(headings(element, 'h3')).toEqual(['Projet premier']);
    expect(element.querySelector('table')?.textContent).toContain('Python');
    expect(element.querySelector('table')?.textContent).not.toContain('Dernière publication');
  });

  it('says so when no project is published', async () => {
    const element = await render({});

    expect(element.textContent).toContain('Aucun projet n’est encore publié.');
  });

  it('keeps the site name without a published profile, and shows the error of a failed zone', async () => {
    const element = await render({ profile: 404, articles: 500 });

    expect(headings(element, 'h1')).toEqual([SITE_NAME]);
    expect(element.querySelectorAll('[role="alert"]').length).toBe(1);
    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Les articles n’ont pas pu être chargés.',
    );
  });
});
