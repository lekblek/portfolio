import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, RouterOutlet } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { NavItem } from './navigation';
import { SiteNav } from './site-nav';

const ITEMS: NavItem[] = [
  { path: '/about', label: 'À propos', footer: true },
  { path: '/projects', label: 'Projets', footer: true },
];

@Component({
  imports: [SiteNav, RouterOutlet],
  template: `<app-site-nav [items]="items()" /><router-outlet />`,
})
class Host {
  readonly items = signal<readonly NavItem[]>(ITEMS);
}

@Component({ template: '<h1>Page</h1>' })
class Page {}

describe('SiteNav', () => {
  async function setUp(items: readonly NavItem[] = ITEMS) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: '',
            component: Host,
            children: [
              { path: 'about', component: Page },
              { path: 'projects', component: Page },
            ],
          },
        ]),
      ],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/about');
    const element: HTMLElement = harness.fixture.nativeElement;
    if (items !== ITEMS) {
      const component = harness.fixture.debugElement.query(
        (d) => d.componentInstance instanceof Host,
      ).componentInstance as Host;
      component.items.set(items);
      harness.detectChanges();
    }
    document.body.appendChild(element);
    return {
      harness,
      element,
      toggle: () => element.querySelector<HTMLButtonElement>('button[aria-controls]'),
      nav: () => element.querySelector<HTMLElement>('nav'),
    };
  }

  afterEach(() => document.body.replaceChildren());

  it('renders nothing while no page is available', async () => {
    const { element } = await setUp([]);

    expect(element.querySelector('nav')).toBeNull();
    expect(element.querySelector('button')).toBeNull();
  });

  it('marks the link of the current page', async () => {
    const { element } = await setUp();

    const current = element.querySelector('a[aria-current="page"]');
    expect(current?.textContent).toBe('À propos');
    expect(element.querySelectorAll('a[aria-current]')).toHaveLength(1);
  });

  it('opens and closes the mobile list with its button', async () => {
    const { harness, toggle, nav } = await setUp();
    expect(toggle()?.getAttribute('aria-expanded')).toBe('false');
    expect(toggle()?.getAttribute('aria-controls')).toBe(nav()?.id);

    toggle()?.click();
    harness.detectChanges();
    expect(toggle()?.getAttribute('aria-expanded')).toBe('true');
    expect(nav()?.classList).toContain('site-nav-open');

    toggle()?.click();
    harness.detectChanges();
    expect(toggle()?.getAttribute('aria-expanded')).toBe('false');
  });

  it('closes on Escape and gives the focus back to the button', async () => {
    const { harness, toggle, nav } = await setUp();
    toggle()?.click();
    harness.detectChanges();
    const link = nav()?.querySelector('a') as HTMLAnchorElement;
    link.focus();

    link.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    harness.detectChanges();

    expect(toggle()?.getAttribute('aria-expanded')).toBe('false');
    expect(document.activeElement).toBe(toggle());
  });

  it('closes after a navigation', async () => {
    const { harness, toggle } = await setUp();
    toggle()?.click();
    harness.detectChanges();

    await harness.navigateByUrl('/projects');

    expect(toggle()?.getAttribute('aria-expanded')).toBe('false');
  });
});
