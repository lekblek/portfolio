import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { MarkdownView } from './markdown-view';

@Component({
  imports: [MarkdownView],
  template: `<app-markdown-view [source]="source()" />`,
})
class Page {
  readonly source = signal('## Une erreur, un format\n\n[plus bas](#suite)');
}

describe('MarkdownView', () => {
  async function setUp() {
    TestBed.configureTestingModule({
      providers: [provideRouter([{ path: 'articles/:slug', component: Page }])],
    });
    const harness = await RouterTestingHarness.create();
    const page = await harness.navigateByUrl('/articles/exemple?page=2', Page);
    return { harness, page, element: harness.routeNativeElement as HTMLElement };
  }

  it('renders the content with reading styles, keeping heading ids', async () => {
    const { element } = await setUp();

    const view = element.querySelector('app-markdown-view') as HTMLElement;
    expect(view.classList).toContain('prose');
    expect(view.querySelector('h2')?.id).toBe('une-erreur-un-format');
  });

  it('points anchors to the current page, without its query', async () => {
    const { element } = await setUp();

    expect(element.querySelector('a')?.getAttribute('href')).toBe('/articles/exemple#suite');
  });

  it('renders the new content when the source changes', async () => {
    const { harness, page, element } = await setUp();

    page.source.set('Texte **gras**');
    harness.detectChanges();

    expect(element.querySelector('strong')?.textContent).toBe('gras');
    expect(element.querySelector('h2')).toBeNull();
  });
});
