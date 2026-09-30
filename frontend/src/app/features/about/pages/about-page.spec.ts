import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { Profile } from '../../../core/api/api-types';
import { SITE_URL } from '../../../core/seo/site-config';
import { AboutPage } from './about-page';

const PROFILE: Profile = {
  displayName: 'Blek Gedeon Ngossanga',
  professionalTitle: 'Ingénieur logiciel',
  shortBio: 'Je conçois des applications web et des systèmes de vision par ordinateur.',
  aboutMarkdown: '## Démarche\n\nDes systèmes **mesurés** avant d’être optimisés.',
  publicLocation: 'Tanger, Maroc',
  publicEmail: 'contact@blek.example',
  avatar: { url: '/api/public/media/avatar', width: 400, height: 400, altText: 'Portrait' },
  cv: { url: '/api/public/media/cv', sizeBytes: 1_258_291 },
  links: [{ label: 'GitHub', url: 'https://github.example/blek' }],
  skillGroups: [{ category: 'Backend', skills: [{ name: 'Spring Boot' }, { name: 'PostgreSQL' }] }],
  experiences: [
    {
      organization: 'Atelier',
      title: 'Ingénieur logiciel',
      location: 'Tanger',
      startDate: '2024-01-01',
      endDate: null,
      description: 'Conception et exploitation.',
    },
  ],
  educations: [
    {
      institution: 'École',
      degree: 'Diplôme d’ingénieur',
      field: 'Informatique',
      location: 'Rabat',
      startDate: '2018-09-01',
      endDate: '2023-06-30',
      description: 'Systèmes et réseaux.',
    },
  ],
  certifications: [
    {
      name: 'Certification cloud',
      issuer: 'Émetteur',
      issuedAt: '2025-01-15',
      expiresAt: '2028-01-15',
      credentialUrl: 'https://credential.example/1',
    },
  ],
};

const MINIMAL: Profile = {
  ...PROFILE,
  aboutMarkdown: null,
  publicLocation: null,
  publicEmail: null,
  avatar: null,
  cv: null,
  links: [],
  skillGroups: [],
  experiences: [],
  educations: [],
  certifications: [],
};

describe('AboutPage', () => {
  async function setUp() {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'about', component: AboutPage }]),
        { provide: SITE_URL, useValue: 'https://blek.example' },
      ],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/about');
    const http = TestBed.inject(HttpTestingController);
    const element = harness.routeNativeElement as HTMLElement;
    const respond = async (profile: Profile) => {
      http.expectOne('/api/public/profile').flush(profile);
      await harness.fixture.whenStable();
    };
    const fail = async (status: number, body: object | null = null) => {
      http.expectOne('/api/public/profile').flush(body, { status, statusText: 'Erreur' });
      await harness.fixture.whenStable();
    };
    return { harness, http, element, respond, fail };
  }

  afterEach(() => {
    TestBed.inject(DOCUMENT)
      .head.querySelectorAll('meta, link[rel="canonical"], script[type="application/ld+json"]')
      .forEach((element) => element.remove());
  });

  const headings = (element: HTMLElement, level: string) =>
    Array.from(element.querySelectorAll(level)).map((heading) => heading.textContent?.trim());

  it('renders the whole profile under one heading, section by section', async () => {
    const { element, respond } = await setUp();
    await respond(PROFILE);

    expect(headings(element, 'h1')).toEqual(['Blek Gedeon Ngossanga']);
    expect(headings(element, 'h2')).toEqual([
      'Présentation',
      'Expériences',
      'Formation',
      'Compétences',
      'Certifications',
    ]);
    expect(headings(element, 'h3')).toEqual([
      'Démarche',
      'Ingénieur logiciel',
      'Diplôme d’ingénieur',
      'Certification cloud',
    ]);
    expect(element.textContent).toContain('Tanger, Maroc');
    expect(element.querySelector('img')?.getAttribute('alt')).toBe('Portrait');
  });

  it('never shows the public email address', async () => {
    const { element, respond } = await setUp();
    await respond(PROFILE);

    expect(element.textContent).not.toContain('contact@blek.example');
    expect(element.querySelector('a[href^="mailto:"]')).toBeNull();
  });

  it('offers the CV with its type and size, and marks external links', async () => {
    const { element, respond } = await setUp();
    await respond(PROFILE);

    const cv = element.querySelector<HTMLAnchorElement>('a[download]');
    expect(cv?.getAttribute('href')).toBe('/api/public/media/cv');
    expect(cv?.getAttribute('download')).toBe('CV Blek Gedeon Ngossanga.pdf');
    // Blancs du gabarit ramenés à une espace ; l'espace insécable avant l'unité reste
    expect(cv?.textContent?.replace(/[ \n]+/g, ' ').trim()).toBe(
      'Télécharger le CV (PDF, 1,2\u00a0Mo)',
    );
    const github = element.querySelector('ul[aria-label="Liens professionnels"] a');
    expect(github?.textContent).toBe('GitHub (site externe)');
    expect(github?.classList).toContain('external');
  });

  it('dates an ongoing experience from its start, and a finished education by both ends', async () => {
    const { element, respond } = await setUp();
    await respond(PROFILE);

    expect(element.textContent).toContain('depuis janvier 2024');
    expect(element.textContent).toContain('septembre 2018\u00a0– juin 2023');
    expect(element.textContent).toContain('Valable jusqu’en janvier 2028');
  });

  it('keeps a coherent page when optional data and every collection are absent', async () => {
    const { element, respond } = await setUp();
    await respond(MINIMAL);

    expect(headings(element, 'h1')).toEqual(['Blek Gedeon Ngossanga']);
    expect(element.querySelectorAll('h2, section, img, ol, dl')).toHaveLength(0);
    expect(element.querySelector('a')).toBeNull();
    expect(element.textContent).toContain(MINIMAL.shortBio);
  });

  it('describes the page for search engines as a profile page', async () => {
    const { respond } = await setUp();
    await respond(PROFILE);

    const document = TestBed.inject(DOCUMENT);
    expect(document.title).toBe('À propos — Blek Ngossanga');
    expect(document.head.querySelector('link[rel="canonical"]')?.getAttribute('href')).toBe(
      'https://blek.example/about',
    );
    const jsonLd = document.getElementById('json-ld-page')?.textContent ?? '';
    expect(JSON.parse(jsonLd)).toMatchObject({
      '@type': 'ProfilePage',
      url: 'https://blek.example/about',
      mainEntity: {
        '@type': 'Person',
        name: 'Blek Gedeon Ngossanga',
        image: 'https://blek.example/api/public/media/avatar',
        sameAs: ['https://github.example/blek'],
      },
    });
    expect(jsonLd).not.toContain('contact@blek.example');
  });

  it('says that the profile is not published yet when the api has none', async () => {
    const { element, fail } = await setUp();
    await fail(404, { status: 404, code: 'RESOURCE_NOT_FOUND' });

    expect(headings(element, 'h1')).toEqual(['Profil non publié']);
    expect(element.textContent).toContain('apparaîtront ici dès la publication du profil.');
    expect(element.querySelector('[role="alert"]')).toBeNull();
    const robots = TestBed.inject(DOCUMENT).head.querySelector('meta[name="robots"]');
    expect(robots?.getAttribute('content')).toBe('noindex');
  });

  it('shows the error and loads the profile again on request', async () => {
    const { element, fail, respond, harness } = await setUp();
    await fail(503);

    expect(headings(element, 'h1')).toEqual(['Profil indisponible']);
    const alert = element.querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Le profil n’a pas pu être chargé.');
    alert?.querySelector('button')?.click();
    harness.detectChanges();
    await respond(PROFILE);

    expect(headings(element, 'h1')).toEqual(['Blek Gedeon Ngossanga']);
  });
});
