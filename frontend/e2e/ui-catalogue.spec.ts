import { expect, expectAccessible, test } from './support/fixtures';

test.describe('ui catalogue', () => {
  test('shows the design tokens accessibly', { tag: '@no-api' }, async ({ page }) => {
    await page.goto('/_ui');

    await expect(page).toHaveTitle('Catalogue de l’interface — Blek Ngossanga');
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Catalogue de l’interface');
    // Contrastes calculés dans le navigateur à partir des tokens appliqués
    await expect(page.getByRole('row', { name: /ink paper .* 15,90:1 AAA/ })).toBeVisible();
    await expectAccessible(page);
  });

  test('never scrolls horizontally', { tag: '@no-api' }, async ({ page }) => {
    await page.goto('/_ui');

    for (const width of [360, 390, 768, 1024, 1280, 1440, 1920]) {
      await page.setViewportSize({ width, height: 900 });
      const scrollWidth = await page.evaluate(() => document.documentElement.scrollWidth);
      expect(scrollWidth, `largeur ${width}`).toBeLessThanOrEqual(width);
    }
  });

  test('moves nothing when reduced motion is requested', { tag: '@no-api' }, async ({ page }) => {
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await page.goto('/_ui');

    const mark = page.locator('.catalogue-mark').last();
    await expect(mark).toHaveCSS('transition-duration', '0.001s');
  });
});
