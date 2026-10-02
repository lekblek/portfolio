import { adminReturnUrl } from './return-url';

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
