import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { AdminFrame } from './admin-frame';
import { AdminSession } from './auth/admin-session';

const URL = '/api/admin/session';

@Component({ template: '<h1>Tableau de bord</h1>' })
class DashboardStub {}

@Component({ template: '<h1>Connexion</h1>' })
class LoginStub {}

async function setUp() {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter([
        { path: 'admin/login', component: LoginStub },
        {
          path: 'admin',
          component: AdminFrame,
          children: [{ path: '', component: DashboardStub }],
        },
      ]),
    ],
  });
  const http = TestBed.inject(HttpTestingController);
  const session = TestBed.inject(AdminSession);
  const opening = session.open({ login: 'admin', password: 'mot de passe' });
  http.expectOne(URL).flush({ login: 'admin', lastLoginAt: '2026-10-02T08:42:00Z' });
  await opening;
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl('/admin');
  const element = () => harness.routeNativeElement as HTMLElement;
  const signOut = async () => {
    Array.from(element().querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Se déconnecter'))!
      .click();
    await harness.fixture.whenStable();
  };
  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await harness.fixture.whenStable();
    harness.detectChanges();
  };
  return { http, session, element, signOut, settle };
}

describe('AdminFrame', () => {
  it('frames the page with the administrator and the navigation', async () => {
    const { element } = await setUp();

    expect(element().querySelector('header')?.textContent?.replace(/\s+/g, ' ')).toContain(
      'Connecté en tant que admin, depuis le 2 octobre 2026 à 10:42',
    );
    expect(element().querySelector('main h1')?.textContent).toBe('Tableau de bord');
    expect(element().querySelector('[aria-current="page"]')?.textContent?.trim()).toBe(
      'Tableau de bord',
    );
  });

  it('closes the session, then goes to the login', async () => {
    const { http, session, signOut, settle } = await setUp();

    await signOut();
    http.expectOne({ method: 'DELETE', url: URL }).flush(null, { status: 204, statusText: '' });
    await settle();

    expect(TestBed.inject(Router).url).toBe('/admin/login');
    expect(session.end()).toBe('closed');
  });

  it('keeps the session and says so when the server does not answer', async () => {
    const { http, session, element, signOut, settle } = await setUp();

    await signOut();
    http.expectOne(URL).error(new ProgressEvent('error'));
    await settle();

    expect(element().querySelector('[role="alert"]')?.textContent).toContain(
      'Déconnexion impossible',
    );
    expect(TestBed.inject(Router).url).toBe('/admin');
    expect(session.account()?.login).toBe('admin');
  });
});
