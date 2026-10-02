import { Component, DOCUMENT, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { AdminAccount } from '../../core/api/api-types';
import { AdminShell } from './admin-shell';

@Component({
  imports: [AdminShell],
  template: `
    <app-admin-shell
      [account]="account()"
      [navigation]="[{ path: '/admin', label: 'Tableau de bord', exact: true }]"
      (signOut)="signedOut.set(true)"
    >
      <h1>Contenu</h1>
    </app-admin-shell>
  `,
})
class Host {
  readonly account = signal<AdminAccount | null>({
    login: 'admin',
    lastLoginAt: '2026-10-02T08:42:00Z',
  });
  readonly signedOut = signal(false);
}

async function setUp() {
  TestBed.configureTestingModule({
    providers: [provideRouter([{ path: 'admin', component: Host }])],
  });
  const harness = await RouterTestingHarness.create();
  const host = await harness.navigateByUrl('/admin', Host);
  const element = () => harness.routeNativeElement as HTMLElement;
  const toggle = () => element().querySelector<HTMLButtonElement>('[aria-controls]')!;
  return { harness, host, element, toggle };
}

describe('AdminShell', () => {
  it('names the administrator and the time of the login in the banner', async () => {
    const { element } = await setUp();

    const banner = element().querySelector('header')!;
    expect(banner.textContent?.replace(/\s+/g, ' ')).toContain(
      'Connecté en tant que admin, depuis le 2 octobre 2026 à 10:42',
    );
    expect(banner.querySelector('strong')?.getAttribute('translate')).toBe('no');
  });

  it('marks the current page, keeps a way back to the site and a skip link', async () => {
    const { element } = await setUp();

    const nav = element().querySelector('nav[aria-label="Administration"]')!;
    expect(nav.querySelector('[aria-current="page"]')?.textContent?.trim()).toBe('Tableau de bord');
    expect(nav.querySelector('a[href="/"]')?.textContent?.trim()).toBe('Voir le site');
    expect(element().querySelector('.skip-link')?.getAttribute('href')).toBe('/admin#contenu');
    expect(element().querySelector('main#contenu h1')?.textContent).toBe('Contenu');
  });

  it('unfolds the navigation with the menu button and folds it with escape', async () => {
    const { harness, element, toggle } = await setUp();
    const nav = () => element().querySelector('nav')!;

    toggle().click();
    harness.detectChanges();
    expect(toggle().getAttribute('aria-expanded')).toBe('true');
    expect(nav().classList).toContain('admin-panel-open');

    nav()
      .querySelector('a')!
      .dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    harness.detectChanges();

    expect(toggle().getAttribute('aria-expanded')).toBe('false');
    expect(nav().classList).not.toContain('admin-panel-open');
    expect(TestBed.inject(DOCUMENT).activeElement).toBe(toggle());
  });

  it('asks the page to sign out', async () => {
    const { host, element } = await setUp();

    Array.from(element().querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Se déconnecter'))!
      .click();

    expect(host.signedOut()).toBe(true);
  });
});
