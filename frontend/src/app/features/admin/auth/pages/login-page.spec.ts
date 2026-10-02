import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { AdminSession } from '../admin-session';
import { LoginPage } from './login-page';

const URL = '/api/admin/session';
const ACCOUNT = { login: 'admin', lastLoginAt: '2026-10-02T08:00:00Z' };

@Component({ template: '<h1>Administration</h1>' })
class AdminPage {}

async function setUp(path = '/admin/login', before?: (session: AdminSession) => void) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter(
        [
          { path: 'admin/login', component: LoginPage },
          { path: 'admin', component: AdminPage },
          { path: 'admin/projects', component: AdminPage },
        ],
        withComponentInputBinding(),
      ),
    ],
  });
  before?.(TestBed.inject(AdminSession));
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(path);
  const http = TestBed.inject(HttpTestingController);
  const router = TestBed.inject(Router);
  const element = () => harness.routeNativeElement as HTMLElement;
  const control = (id: string) => element().querySelector<HTMLInputElement>(`#${id}`)!;
  const type = (id: string, value: string) => {
    control(id).value = value;
    control(id).dispatchEvent(new Event('input'));
  };
  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await harness.fixture.whenStable();
    harness.detectChanges();
  };
  const noSession = async () => {
    http
      .expectOne({ method: 'GET', url: URL })
      .flush({ status: 401 }, { status: 401, statusText: '' });
    await settle();
  };
  const submit = async (login: string, password: string) => {
    type('connexion-identifiant', login);
    type('connexion-mot-de-passe', password);
    element()
      .querySelector('form')!
      .dispatchEvent(new Event('submit', { cancelable: true }));
    await harness.fixture.whenStable();
    harness.detectChanges();
  };
  return { http, router, element, control, settle, noSession, submit };
}

describe('LoginPage', () => {
  const document = () => TestBed.inject(DOCUMENT);

  it('labels both fields for the password manager and focuses the login', async () => {
    const { element, control, noSession } = await setUp();
    await noSession();

    const labels = Array.from(element().querySelectorAll('app-field label')).map((label) =>
      label.textContent?.trim(),
    );
    expect(labels).toEqual(['Identifiant', 'Mot de passe']);
    expect(control('connexion-identifiant').getAttribute('autocomplete')).toBe('username');
    expect(control('connexion-mot-de-passe').getAttribute('autocomplete')).toBe('current-password');
    expect(control('connexion-mot-de-passe').type).toBe('password');
    expect(document().activeElement).toBe(control('connexion-identifiant'));
  });

  it('goes straight to the requested page when the session is already open', async () => {
    const { http, router, settle } = await setUp('/admin/login?returnUrl=%2Fadmin%2Fprojects');

    http.expectOne(URL).flush(ACCOUNT);
    await settle();

    expect(router.url).toBe('/admin/projects');
  });

  it('opens the session, then follows the return address of the administration only', async () => {
    const { http, router, settle, noSession, submit } = await setUp(
      '/admin/login?returnUrl=https%3A%2F%2Fexemple.invalid',
    );
    await noSession();

    await submit('admin', 'mot de passe');
    const post = http.expectOne({ method: 'POST', url: URL });
    expect(post.request.body).toEqual({ login: 'admin', password: 'mot de passe' });
    post.flush(ACCOUNT);
    await settle();

    expect(router.url).toBe('/admin');
  });

  it('refuses wrong credentials, clears and focuses the password', async () => {
    const { http, element, control, settle, noSession, submit } = await setUp();
    await noSession();

    await submit('admin', 'faux');
    http
      .expectOne(URL)
      .flush(
        { status: 401, code: 'INVALID_CREDENTIALS' },
        { status: 401, statusText: 'Unauthorized' },
      );
    await settle();
    await settle();

    expect(element().querySelector('[role="alert"]')?.textContent).toContain(
      'Identifiant ou mot de passe incorrect',
    );
    expect(control('connexion-identifiant').value).toBe('admin');
    expect(control('connexion-mot-de-passe').value).toBe('');
    expect(element().querySelector('.field-error')).toBeNull();
    expect(document().activeElement).toBe(control('connexion-mot-de-passe'));
  });

  it('says how long to wait after too many attempts', async () => {
    const { http, element, control, settle, noSession, submit } = await setUp();
    await noSession();

    await submit('admin', 'faux');
    http.expectOne(URL).flush(
      { status: 429, code: 'TOO_MANY_LOGIN_ATTEMPTS' },
      {
        status: 429,
        statusText: 'Too Many Requests',
        headers: new HttpHeaders({ 'Retry-After': '600' }),
      },
    );
    await settle();

    expect(element().querySelector('[role="alert"]')?.textContent).toContain(
      'Réessayez dans 10 minutes.',
    );
    expect(control('connexion-mot-de-passe').value).toBe('faux');
  });

  it('says so when the server cannot be reached', async () => {
    const { http, element, settle } = await setUp();

    http.expectOne(URL).error(new ProgressEvent('error'));
    await settle();

    expect(element().querySelector('[role="alert"]')?.textContent).toContain(
      'Connexion impossible',
    );
  });

  it('shows the errors of empty fields without asking the server', async () => {
    const { http, element, control, noSession, submit } = await setUp();
    await noSession();

    await submit('', '');

    http.expectNone(URL);
    expect(
      Array.from(element().querySelectorAll('.field-error')).map((e) => e.textContent?.trim()),
    ).toEqual(['Indiquez votre identifiant.', 'Indiquez votre mot de passe.']);
    expect(document().activeElement).toBe(control('connexion-identifiant'));
  });

  it('says that the session expired and gives it the focus', async () => {
    const { element, noSession } = await setUp('/admin/login', (session) => session.expire());
    await noSession();

    const notice = element().querySelector('app-alert')!;
    expect(notice.textContent).toContain('Session expirée');
    expect(document().activeElement).toBe(notice);
  });
});
