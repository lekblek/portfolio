import { expect, expectAccessible, test } from './support/fixtures';

test.describe('not found page', () => {
  test(
    'answers an unknown address with a real 404',
    { tag: '@no-api' },
    async ({ page, guard }) => {
      guard.allowHttpError('/nimporte-quoi');

      const response = await page.goto('/nimporte-quoi');

      expect(response?.status()).toBe(404);
      await expect(page).toHaveTitle('Page introuvable — Blek Ngossanga');
      await expect(page.getByRole('heading', { level: 1, name: 'Page introuvable' })).toBeVisible();
      await expect(page.locator('html')).toHaveAttribute('lang', 'fr');
      await expectAccessible(page);
    },
  );
});
