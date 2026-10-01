import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { Pagination } from './pagination';

@Component({
  imports: [Pagination],
  template: `<app-pagination [page]="page()" [totalPages]="totalPages()" />`,
})
class Host {
  readonly page = signal(1);
  readonly totalPages = signal(10);
}

describe('Pagination', () => {
  async function render(page: number, totalPages: number, url = '/projects?technology=java') {
    TestBed.configureTestingModule({
      providers: [provideRouter([{ path: 'projects', component: Host }])],
    });
    const harness = await RouterTestingHarness.create();
    const host = await harness.navigateByUrl(url, Host);
    host.page.set(page);
    host.totalPages.set(totalPages);
    harness.detectChanges();
    const element = harness.routeNativeElement as HTMLElement;
    const links = () =>
      Array.from(element.querySelectorAll('a')).map((link) => ({
        text: link.textContent?.replace(/\s+/g, ' ').trim(),
        href: link.getAttribute('href'),
        current: link.getAttribute('aria-current'),
      }));
    return { element, links };
  }

  it('renders nothing for a single page', async () => {
    const { element } = await render(1, 1);

    expect(element.querySelector('nav')).toBeNull();
  });

  it('links the first page without parameter and marks it current, keeping the filter', async () => {
    const { element, links } = await render(1, 3);

    expect(element.querySelector('nav')?.getAttribute('aria-label')).toBe('Pagination');
    expect(links()).toEqual([
      { text: 'Page 1', href: '/projects?technology=java', current: 'page' },
      { text: 'Page 2', href: '/projects?technology=java&page=2', current: null },
      { text: 'Page 3', href: '/projects?technology=java&page=3', current: null },
      { text: 'Suivante', href: '/projects?technology=java&page=2', current: null },
    ]);
  });

  it('offers the previous page and elides distant pages in the middle of a long list', async () => {
    const { element, links } = await render(5, 10, '/projects?page=5');

    expect(links().map((link) => link.text)).toEqual([
      'Précédente',
      'Page 1',
      'Page 4',
      'Page 5',
      'Page 6',
      'Page 10',
      'Suivante',
    ]);
    expect(links().find((link) => link.current === 'page')?.text).toBe('Page 5');
    expect(links()[1].href).toBe('/projects');
    expect(element.querySelectorAll('[aria-hidden="true"]')).toHaveLength(2);
  });

  it('has no next link on the last page', async () => {
    const { links } = await render(3, 3, '/projects?page=3');

    expect(links().map((link) => link.text)).toEqual(['Précédente', 'Page 1', 'Page 2', 'Page 3']);
  });
});
