import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { AdminSession } from '../auth/admin-session';
import { DashboardPage } from './dashboard-page';

const URL = '/api/admin/session';

@Component({ template: '<h1>Connexion</h1>' })
class LoginStub {}

async function setUp() {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter([
        { path: 'admin', component: DashboardPage },
        { path: 'admin/login', component: LoginStub },
      ]),
    ],
  });
  const http = TestBed.inject(HttpTestingController);
  const session = TestBed.inject(AdminSession);
  const opening = session.open({ login: 'admin', password: 'mot de passe' });
  http.expectOne(URL).flush({ login: 'admin', lastLoginAt: '2026-10-02T08:00:00Z' });
  await opening;
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl('/admin');
  const element = () => harness.routeNativeElement as HTMLElement;
  const signOut = async () => {
    element().querySelector('button')!.click();
    await harness.fixture.whenStable();
  };
  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await harness.fixture.whenStable();
    harness.detectChanges();
  };
  return { http, session, element, signOut, settle };
}

describe('DashboardPage', () => {
  it('names the administrator, then closes the session and goes to the login', async () => {
    const { http, session, element, signOut, settle } = await setUp();
    expect(element().textContent).toContain('Connecté en tant que admin.');

    await signOut();
    http.expectOne({ method: 'DELETE', url: URL }).flush(null, { status: 204, statusText: '' });
    await settle();

    expect(TestBed.inject(Router).url).toBe('/admin/login');
    expect(session.closedByUser()).toBe(true);
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
