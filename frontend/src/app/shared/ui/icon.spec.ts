import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { Icon } from './icon';
import { ICONS } from './icons';

@Component({
  imports: [Icon],
  template: `<button type="button"><app-icon name="close" /> Fermer</button>`,
})
class Host {}

describe('Icon', () => {
  it('draws the paths of the requested icon', () => {
    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();

    const paths = [...fixture.nativeElement.querySelectorAll('path')].map((p: SVGPathElement) =>
      p.getAttribute('d'),
    );

    expect(paths).toEqual([...ICONS.close]);
  });

  it('is hidden from assistive technologies, the control keeps its text name', () => {
    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;

    expect(element.querySelector('app-icon')?.getAttribute('aria-hidden')).toBe('true');
    expect(element.querySelector('svg')?.getAttribute('focusable')).toBe('false');
    expect(element.querySelector('button')?.textContent?.trim()).toBe('Fermer');
  });
});
