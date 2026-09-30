import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { CareerEntry } from './career-entry';

@Component({
  imports: [CareerEntry],
  template: `
    <app-career-entry
      heading="Ingénieur logiciel"
      [details]="details()"
      when="depuis janvier 2024"
      [description]="description()"
    >
      <p class="extra">Suite</p>
    </app-career-entry>
  `,
})
class Host {
  readonly details = signal<string[]>(['Atelier', 'Tanger']);
  readonly description = signal<string | null>('Conception.');
}

describe('CareerEntry', () => {
  function render() {
    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  it('puts the heading first, then the details, the date and the description', () => {
    const { element } = render();

    const texts = Array.from(element.querySelectorAll('app-career-entry > *')).map((node) =>
      node.textContent?.replace(/\s+/g, ' ').trim(),
    );
    expect(texts).toEqual([
      'Ingénieur logiciel',
      'Atelier · Tanger',
      'depuis janvier 2024',
      'Conception.',
      'Suite',
    ]);
    expect(element.querySelector('h3')?.textContent).toBe('Ingénieur logiciel');
  });

  it('hides the separators from assistive technologies', () => {
    const { element } = render();

    expect(element.querySelector('.entry-details [aria-hidden="true"]')?.textContent).toContain(
      '·',
    );
  });

  it('omits the details and the description when there are none', () => {
    const { fixture, element } = render();
    fixture.componentInstance.details.set([]);
    fixture.componentInstance.description.set(null);
    fixture.detectChanges();

    expect(element.querySelector('.entry-details')).toBeNull();
    expect(element.querySelector('.entry-description')).toBeNull();
  });
});
