import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { adminReturnUrl, loginTree } from './return-url';

describe('adminReturnUrl', () => {
  it('keeps a page of the administration, with its query and anchor', () => {
    expect(adminReturnUrl('/admin')).toBe('/admin');
    expect(adminReturnUrl('/admin/projects?page=2#liste')).toBe('/admin/projects?page=2#liste');
  });

  it('falls back to the administration home without a usable address', () => {
    expect(adminReturnUrl(undefined)).toBe('/admin');
    expect(adminReturnUrl(null)).toBe('/admin');
    expect(adminReturnUrl('')).toBe('/admin');
    expect(adminReturnUrl('/admin/login')).toBe('/admin');
  });

  it('never leaves the site nor the administration', () => {
    for (const outside of [
      'https://exemple.invalid/admin',
      '//exemple.invalid/admin',
      '/\\exemple.invalid/admin',
      '/\t/exemple.invalid/admin',
      'javascript:alert(1)',
      'admin',
      '/articles',
      '/administration',
      '/admin/../articles',
    ]) {
      expect(adminReturnUrl(outside), outside).toBe('/admin');
    }
  });
});

describe('loginTree', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideRouter([])] }));

  function serialize(requested: string): string {
    const router = TestBed.inject(Router);
    return router.serializeUrl(loginTree(router, requested));
  }

  it('keeps the requested page of the administration as the return address', () => {
    expect(serialize('/admin/projects?page=2')).toBe(
      '/admin/login?returnUrl=%2Fadmin%2Fprojects%3Fpage%3D2',
    );
  });

  it('omits the return address for the home of the administration or an outside page', () => {
    expect(serialize('/admin')).toBe('/admin/login');
    expect(serialize('/articles')).toBe('/admin/login');
  });
});
