import { TestBed } from '@angular/core/testing';
import { provideRouter, Route, Router, UrlSegment, UrlTree } from '@angular/router';

import { adminGuard } from './admin-guard';
import { AdminSession } from './admin-session';

function guard(isOpen: () => Promise<boolean>, path: string[]) {
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: AdminSession, useValue: { isOpen } }],
  });
  const segments = path.map((part) => new UrlSegment(part, {}));
  return TestBed.runInInjectionContext(() =>
    adminGuard({} as Route, segments, {} as Parameters<typeof adminGuard>[2]),
  ) as Promise<boolean | UrlTree>;
}

describe('adminGuard', () => {
  it('lets an open session through', async () => {
    expect(await guard(async () => true, [])).toBe(true);
  });

  it('sends to the login without a session, with the requested page', async () => {
    const result = await guard(async () => false, ['projects', '12']);

    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe(
      '/admin/login?returnUrl=%2Fadmin%2Fprojects%2F12',
    );
  });

  it('omits the return address for the administration home', async () => {
    const result = await guard(async () => false, []);

    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/admin/login');
  });

  it('sends to the login when the session cannot be checked', async () => {
    const result = await guard(() => Promise.reject(new Error('réseau')), []);

    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/admin/login');
  });
});
