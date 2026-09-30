import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { SITE_NAME } from '../../core/seo/site-config';
import { PublicShell } from './public-shell';

@Component({ template: '<h1>Page introuvable</h1>' })
class Page {}

describe('PublicShell', () => {
  async function render(url: string) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: '', component: PublicShell, children: [{ path: '**', component: Page }] },
        ]),
      ],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url);
    return harness.fixture.nativeElement as HTMLElement;
  }

  it('provides the banner, main and contentinfo landmarks around the page', async () => {
    const element = await render('/x');

    expect(element.querySelector('header')).not.toBeNull();
    expect(element.querySelector('main#contenu h1')?.textContent).toBe('Page introuvable');
    expect(element.querySelector('footer')).not.toBeNull();
  });

  it('points the skip link to the content of the current page', async () => {
    const element = await render('/articles/exemple?page=2');

    const skip = element.querySelector('a.skip-link');
    expect(skip?.getAttribute('href')).toBe('/articles/exemple?page=2#contenu');
    expect(element.querySelector('app-public-shell')?.firstElementChild).toBe(skip);
  });

  it('names the site in the header and the footer, without a link to a missing home page', async () => {
    const element = await render('/x');

    expect(element.querySelector('header')?.textContent).toContain(SITE_NAME);
    expect(element.querySelector('header a[href="/"]')).toBeNull();
    expect(element.querySelector('footer')?.textContent).toContain(`© ${new Date().getFullYear()}`);
  });
});
