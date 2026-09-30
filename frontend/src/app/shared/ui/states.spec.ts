import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { EmptyState } from './empty-state';
import { ErrorState } from './error-state';

@Component({
  imports: [EmptyState, ErrorState],
  template: `
    <app-empty-state message="Aucun article pour cette catégorie.">
      <a href="/articles">Voir tous les articles</a>
    </app-empty-state>
    <app-error-state [detail]="detail()" (retry)="retries.set(retries() + 1)" />
  `,
})
class Host {
  readonly detail = signal<string | null>(null);
  readonly retries = signal(0);
}

describe('screen states', () => {
  function setUp() {
    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  it('explains an empty state and offers what to do next', () => {
    const { element } = setUp();

    const empty = element.querySelector('app-empty-state') as HTMLElement;
    expect(empty.textContent).toContain('Aucun article pour cette catégorie.');
    expect(empty.querySelector('a')?.getAttribute('href')).toBe('/articles');
  });

  it('announces a loading error without a detail by default', () => {
    const { element } = setUp();

    const alert = element.querySelector('[role="alert"]') as HTMLElement;
    expect(alert.textContent).toContain('Le contenu n’a pas pu être chargé.');
    expect(alert.querySelectorAll('p')).toHaveLength(1);
  });

  it('shows the detail given by the api', () => {
    const { fixture, element } = setUp();

    fixture.componentInstance.detail.set('Service momentanément indisponible.');
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Service momentanément indisponible.',
    );
  });

  it('asks the page to retry', () => {
    const { fixture, element } = setUp();

    (element.querySelector('app-error-state button') as HTMLButtonElement).click();

    expect(fixture.componentInstance.retries()).toBe(1);
  });
});
