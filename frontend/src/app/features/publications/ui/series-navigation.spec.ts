import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { SeriesNavigation as Navigation } from '../../../core/api/api-types';
import { SeriesNavigation } from './series-navigation';

const FIRST: Navigation = {
  series: { title: 'Spring Boot', slug: 'spring-boot' },
  position: 1,
  chapterCount: 3,
  previous: null,
  next: { position: 2, title: 'Deuxième', slug: 'deuxieme' },
};

@Component({
  imports: [SeriesNavigation],
  template: `<app-series-navigation [navigation]="navigation()" [variant]="variant()" />`,
})
class Host {
  readonly navigation = signal<Navigation>(FIRST);
  readonly variant = signal<'context' | 'chapters'>('chapters');
}

describe('SeriesNavigation', () => {
  function render(navigation: Navigation, variant: 'context' | 'chapters' = 'chapters') {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(Host);
    fixture.componentInstance.navigation.set(navigation);
    fixture.componentInstance.variant.set(variant);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('offers only the next chapter on the first one', () => {
    const element = render(FIRST);

    expect(element.querySelector('a[rel="prev"]')).toBeNull();
    expect(element.querySelector('a[rel="next"]')?.getAttribute('href')).toBe('/articles/deuxieme');
    expect(element.querySelector('nav')?.getAttribute('aria-label')).toBe('Série « Spring Boot »');
  });

  it('offers only the previous chapter on the last one', () => {
    const element = render({
      ...FIRST,
      position: 3,
      previous: { position: 2, title: 'Deuxième', slug: 'deuxieme' },
      next: null,
    });

    expect(element.querySelector('a[rel="prev"]')?.textContent).toBe('Deuxième');
    expect(element.querySelector('a[rel="next"]')).toBeNull();
  });

  it('states the position in one line above the article', () => {
    const element = render(FIRST, 'context');

    expect(element.textContent?.replace(/\s+/g, ' ').trim()).toBe(
      'Chapitre 1 sur 3 de la série Spring Boot',
    );
    expect(element.querySelector('nav')).toBeNull();
  });
});
