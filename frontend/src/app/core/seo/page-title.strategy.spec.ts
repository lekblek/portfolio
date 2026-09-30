import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { provideRouter, TitleStrategy } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { PageTitleStrategy, SITE_NAME } from './page-title.strategy';

@Component({ template: '' })
class Blank {}

describe('PageTitleStrategy', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'titled', title: 'Projets', component: Blank },
          { path: 'untitled', component: Blank },
        ]),
        { provide: TitleStrategy, useClass: PageTitleStrategy },
      ],
    });
  });

  it('appends the site name to the page title', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/titled');

    expect(TestBed.inject(Title).getTitle()).toBe(`Projets — ${SITE_NAME}`);
  });

  it('uses the site name alone for a route without title', async () => {
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/untitled');

    expect(TestBed.inject(Title).getTitle()).toBe(SITE_NAME);
  });
});
