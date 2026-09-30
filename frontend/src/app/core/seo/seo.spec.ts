import { DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { Seo } from './seo';
import { SITE_DESCRIPTION, SITE_TITLE, SITE_URL } from './site-config';

describe('Seo', () => {
  let seo: Seo;
  let document: Document;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: SITE_URL, useValue: 'https://blek.example' }],
    });
    seo = TestBed.inject(Seo);
    document = TestBed.inject(DOCUMENT);
  });

  afterEach(() => {
    document.head
      .querySelectorAll('meta, link[rel="canonical"], script[type="application/ld+json"]')
      .forEach((element) => element.remove());
  });

  const meta = (selector: string) =>
    document.head.querySelector(`meta[${selector}]`)?.getAttribute('content');

  it('describes a page with its own title and description', () => {
    seo.set({
      title: 'Projets',
      description: 'Travaux menés de la conception à la mise en service.',
      path: '/projects',
    });

    expect(document.title).toBe('Projets — Blek Ngossanga');
    expect(meta('name="description"')).toBe('Travaux menés de la conception à la mise en service.');
    expect(meta('property="og:title"')).toBe('Projets — Blek Ngossanga');
    expect(meta('property="og:url"')).toBe('https://blek.example/projects');
    expect(meta('property="og:locale"')).toBe('fr_FR');
  });

  it('points the canonical link to an absolute address on the public site', () => {
    seo.set({ path: '/articles/erreurs-api' });

    const canonical = document.head.querySelectorAll('link[rel="canonical"]');
    expect(canonical).toHaveLength(1);
    expect(canonical[0].getAttribute('href')).toBe('https://blek.example/articles/erreurs-api');
  });

  it('falls back to the reference title and the default description', () => {
    seo.set({ path: '/' });

    expect(document.title).toBe(SITE_TITLE);
    expect(meta('name="description"')).toBe(SITE_DESCRIPTION);
    expect(meta('property="og:type"')).toBe('website');
  });

  it('replaces everything left by the previous page', () => {
    seo.set({
      title: 'Introuvable',
      description: 'Une description',
      path: '/x',
      image: '/api/public/media/couverture',
      type: 'article',
      noindex: true,
    });

    seo.set({ title: 'Projets', path: '/projects' });

    expect(meta('name="robots"')).toBeUndefined();
    expect(meta('property="og:image"')).toBeUndefined();
    expect(meta('name="twitter:card"')).toBe('summary');
    expect(meta('name="description"')).toBe(SITE_DESCRIPTION);
    expect(meta('property="og:type"')).toBe('website');
    expect(document.head.querySelectorAll('meta[name="description"]')).toHaveLength(1);
    expect(document.head.querySelectorAll('link[rel="canonical"]')).toHaveLength(1);
  });

  it('asks search engines not to index a page', () => {
    seo.set({ title: 'Page introuvable', path: '/x', noindex: true });

    expect(meta('name="robots"')).toBe('noindex');
  });

  it('makes the sharing image address absolute', () => {
    seo.set({ path: '/projects/a', image: '/api/public/media/cle' });

    expect(meta('property="og:image"')).toBe('https://blek.example/api/public/media/cle');
    expect(meta('name="twitter:card"')).toBe('summary_large_image');
  });

  it('declares the site once in structured data, even when called again', () => {
    seo.setWebsiteJsonLd();
    seo.setWebsiteJsonLd();

    const scripts = document.head.querySelectorAll('script[type="application/ld+json"]');
    expect(scripts).toHaveLength(1);
    expect(JSON.parse(scripts[0].textContent ?? '')).toMatchObject({
      '@type': 'WebSite',
      name: 'Blek Ngossanga',
      url: 'https://blek.example/',
      inLanguage: 'fr',
    });
  });

  it('gives the page its own structured data, removed by the next page', () => {
    seo.setWebsiteJsonLd();
    seo.set({ path: '/about', jsonLd: { '@type': 'ProfilePage', name: '</script>' } });

    const page = document.getElementById('json-ld-page');
    expect(JSON.parse(page?.textContent ?? '')).toEqual({
      '@type': 'ProfilePage',
      name: '</script>',
    });
    expect(page?.textContent).not.toContain('</script>');

    seo.set({ path: '/projects' });

    expect(document.getElementById('json-ld-page')).toBeNull();
    expect(document.getElementById('json-ld-site')).not.toBeNull();
  });

  it('makes a path of the site absolute', () => {
    expect(seo.absolute('/api/public/media/cle')).toBe('https://blek.example/api/public/media/cle');
  });
});
