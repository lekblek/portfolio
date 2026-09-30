import { expect, expectAccessible, test } from './support/fixtures';

test.describe('markdown rendering', () => {
  test('renders the specimen accessibly', { tag: '@no-api' }, async ({ page }) => {
    const response = await page.goto('/_ui/prose');

    expect(response?.status()).toBe(200);
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Rendu Markdown');
    await expect(page.locator('.prose h2#une-erreur-un-format')).toBeVisible();
    await expect(page.locator('.prose script')).toHaveCount(0);
    await expectAccessible(page);
  });

  test(
    'scrolls long code and tables inside their block only',
    { tag: '@no-api' },
    async ({ page }) => {
      await page.setViewportSize({ width: 360, height: 780 });
      await page.goto('/_ui/prose');

      const scrollWidth = await page.evaluate(() => document.documentElement.scrollWidth);
      expect(scrollWidth).toBeLessThanOrEqual(360);
      const codeScrolls = await page
        .locator('.prose pre.code')
        .nth(2)
        .evaluate((block) => block.scrollWidth > block.clientWidth);
      expect(codeScrolls).toBe(true);
    },
  );
});
