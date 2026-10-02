import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { adminUnauthorizedInterceptor } from './admin-unauthorized.interceptor';
import { AdminSession } from './admin-session';

@Component({ template: '' })
class Blank {}

async function setUp() {
  const session = { expire: vi.fn() };
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(withInterceptors([adminUnauthorizedInterceptor])),
      provideHttpClientTesting(),
      provideRouter([
        { path: 'admin/login', component: Blank },
        { path: 'admin/projects', component: Blank },
      ]),
      { provide: AdminSession, useValue: session },
    ],
  });
  const router = TestBed.inject(Router);
  await router.navigateByUrl('/admin/projects?page=2');
  return {
    session,
    router,
    http: TestBed.inject(HttpClient),
    backend: TestBed.inject(HttpTestingController),
  };
}

describe('adminUnauthorizedInterceptor', () => {
  it('forgets an expired session and sends to the login with the current page', async () => {
    const { session, router, http, backend } = await setUp();

    const call = firstValueFrom(http.get('/api/admin/projects'));
    backend
      .expectOne('/api/admin/projects')
      .flush({ status: 401 }, { status: 401, statusText: 'Unauthorized' });

    await expect(call).rejects.toMatchObject({ status: 401 });
    await new Promise((resolve) => setTimeout(resolve));
    expect(session.expire).toHaveBeenCalledOnce();
    expect(router.url).toBe('/admin/login?returnUrl=%2Fadmin%2Fprojects%3Fpage%3D2');
  });

  it('leaves the other errors and the public requests to their caller', async () => {
    const { session, router, http, backend } = await setUp();

    const conflict = firstValueFrom(http.get('/api/admin/projects'));
    backend.expectOne('/api/admin/projects').flush({}, { status: 409, statusText: 'Conflict' });
    const publicCall = firstValueFrom(http.get('/api/public/profile'));
    backend.expectOne('/api/public/profile').flush({}, { status: 401, statusText: 'Unauthorized' });

    await expect(conflict).rejects.toMatchObject({ status: 409 });
    await expect(publicCall).rejects.toMatchObject({ status: 401 });
    expect(session.expire).not.toHaveBeenCalled();
    expect(router.url).toBe('/admin/projects?page=2');
  });
});
