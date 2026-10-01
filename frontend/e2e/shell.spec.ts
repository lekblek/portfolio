import { expect, expectAccessible, test } from './support/fixtures';

// Toute adresse inconnue est rendue dans le shell public : page sans API, pour la CI sans backend.
const PAGE = '/nimporte-quoi';

test.describe('public shell', () => {
  test.beforeEach(async ({ page, guard }) => {
    guard.allowHttpError(PAGE);
    await page.goto(PAGE);
  });

  test(
    'frames the page with its landmarks and one heading',
    { tag: '@no-api' },
    async ({ page }) => {
      await expect(page.getByRole('banner')).toContainText('Blek Ngossanga');
      await expect(page.getByRole('main')).toBeVisible();
      await expect(page.getByRole('contentinfo')).toContainText('Blek Ngossanga');
      await expect(page.getByRole('heading', { level: 1 })).toHaveCount(1);
      await expectAccessible(page);
    },
  );

  test(
    'lists the available pages, none of them current on an unknown address',
    { tag: '@no-api' },
    async ({ page }) => {
      const toggle = page.getByRole('button', { name: 'Menu' });
      if (await toggle.isVisible()) {
        await toggle.click();
      }
      const nav = page.getByRole('navigation', { name: 'Navigation principale' });

      await expect(nav.getByRole('link')).toHaveText([
        'Projets',
        'Articles',
        'Actualités',
        'À propos',
      ]);
      await expect(nav.getByRole('link', { name: 'À propos' })).toHaveAttribute('href', '/about');
      await expect(nav.locator('[aria-current]')).toHaveCount(0);
      await expect(
        page.getByRole('banner').getByRole('link', { name: 'Blek Ngossanga' }),
      ).toHaveCount(0);
      await expect(
        page.getByRole('contentinfo').getByRole('link', { name: 'À propos' }),
      ).toBeVisible();
    },
  );

  test('skips to the content from the keyboard', { tag: '@no-api' }, async ({ page }) => {
    await page.keyboard.press('Tab');
    const skip = page.getByRole('link', { name: 'Aller au contenu' });
    await expect(skip).toBeFocused();
    await expect(skip).toBeInViewport();

    await page.keyboard.press('Enter');

    await expect(page).toHaveURL(new RegExp(`${PAGE}#contenu$`));
    await expect(page.getByRole('main')).toBeFocused();
  });

  test(
    'opens and closes the mobile navigation from the keyboard',
    { tag: '@no-api' },
    async ({ page }, info) => {
      test.skip(info.project.name !== 'mobile', 'navigation dépliable sous 64 rem seulement');
      const toggle = page.getByRole('button', { name: 'Menu' });

      await toggle.focus();
      await page.keyboard.press('Enter');
      await expect(toggle).toHaveAttribute('aria-expanded', 'true');
      await expect(page.getByRole('navigation', { name: 'Navigation principale' })).toBeVisible();
      await expectAccessible(page);

      await page.keyboard.press('Tab');
      await page.keyboard.press('Escape');
      await expect(toggle).toHaveAttribute('aria-expanded', 'false');
      await expect(toggle).toBeFocused();
    },
  );
});
