import { Component, DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { provideRouter, TitleStrategy } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { PageTitleStrategy } from './page-title.strategy';
import { SITE_NAME, SITE_TITLE, SITE_URL } from './site-config';

@Component({ template: '' })
class Blank {}

describe('PageTitleStrategy', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'titled', title: 'Projets', component: Blank },
          { path: 'untitled', component: Blank },
          { path: 'hidden', title: 'Page introuvable', data: { noindex: true }, component: Blank },
        ]),
        { provide: TitleStrategy, useClass: PageTitleStrategy },
        { provide: SITE_URL, useValue: 'https://blek.example' },
      ],
    });
  });

  afterEach(() =>
    TestBed.inject(DOCUMENT)
      .head.querySelectorAll('meta, link[rel="canonical"]')
      .forEach((element) => element.remove()),
  );

  const head = () => TestBed.inject(DOCUMENT).head;

  it('appends the site name to the page title', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/titled');

    expect(TestBed.inject(Title).getTitle()).toBe(`Projets — ${SITE_NAME}`);
  });

  it('uses the reference title of the site for a route without title', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/untitled');

    expect(TestBed.inject(Title).getTitle()).toBe(SITE_TITLE);
  });

  it('sets the canonical address without query nor anchor', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/titled?page=2#haut');

    expect(head().querySelector('link[rel="canonical"]')?.getAttribute('href')).toBe(
      'https://blek.example/titled',
    );
  });

  it('keeps pages marked noindex out of search engines, and only them', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/hidden');
    expect(head().querySelector('meta[name="robots"]')?.getAttribute('content')).toBe('noindex');

    await harness.navigateByUrl('/titled');
    expect(head().querySelector('meta[name="robots"]')).toBeNull();
  });
});
