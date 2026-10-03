import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, RouterOutlet } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { focusPageHeadingOnNavigation } from './focus-on-navigation';

@Component({
  selector: 'app-test-nav-shell',
  imports: [RouterOutlet],
  template: `<main><router-outlet /></main>`,
})
class Shell {
  constructor() {
    focusPageHeadingOnNavigation();
  }
}

@Component({
  selector: 'app-test-with-heading',
  template: `<h1>Projets</h1>
    <a href="/x">lien</a>`,
})
class WithHeading {}

@Component({ selector: 'app-test-without-heading', template: `<p>Sans titre</p>` })
class WithoutHeading {}

describe('focusPageHeadingOnNavigation', () => {
  async function setUp() {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: '',
            component: Shell,
            children: [
              { path: 'first', component: WithHeading },
              { path: 'second', component: WithHeading },
              { path: 'bare', component: WithoutHeading },
            ],
          },
        ]),
      ],
    });
    const harness = await RouterTestingHarness.create();
    document.body.appendChild(harness.fixture.nativeElement);
    return harness;
  }

  afterEach(() => document.body.replaceChildren());

  it('leaves the focus alone on the first display', async () => {
    const harness = await setUp();

    await harness.navigateByUrl('/first');
    await harness.fixture.whenStable();

    expect(document.activeElement).toBe(document.body);
  });

  it('moves the focus to the page heading after a navigation', async () => {
    const harness = await setUp();
    await harness.navigateByUrl('/first');

    await harness.navigateByUrl('/second');
    await harness.fixture.whenStable();

    const heading = document.querySelector('main h1');
    expect(document.activeElement).toBe(heading);
    expect(heading?.getAttribute('tabindex')).toBe('-1');
  });

  it('falls back to the main landmark when the page has no heading', async () => {
    const harness = await setUp();
    await harness.navigateByUrl('/first');

    await harness.navigateByUrl('/bare');
    await harness.fixture.whenStable();

    expect(document.activeElement).toBe(document.querySelector('main'));
  });

  it('lets the router handle addresses with an anchor', async () => {
    const harness = await setUp();
    await harness.navigateByUrl('/first');

    await harness.navigateByUrl('/second#section');
    await harness.fixture.whenStable();

    expect(document.activeElement).toBe(document.body);
  });
});
