import { Page } from '@playwright/test';

import { expect, test } from './support/fixtures';
import { recordMotionFromLoad, recordTransitions } from './support/motion';

// Mouvement du site public (F20, 02-design-system §11) : rien ne bouge au chargement d'une page ;
// navigation mobile et messages d'issue entrent en 200 ms ; rien ne se déplace sous mouvement réduit.
const CONTACT_API = '/api/public/contact-messages';
const NAV_LIST = '#navigation-principale ul';

async function failContactMessage(page: Page): Promise<void> {
  await page.route(`**${CONTACT_API}`, (route) =>
    route.fulfill({ status: 503, json: { status: 503, code: 'SERVICE_UNAVAILABLE' } }),
  );
  await page.getByLabel('Nom').fill('Camille Martin');
  await page.getByLabel('Adresse électronique').fill('camille@example.com');
  await page.getByLabel('Sujet').fill('Essai sans envoi');
  await page.getByLabel('Message').fill('Bonjour.');
  await page.getByRole('button', { name: 'Envoyer le message' }).click();
  await expect(page.getByRole('alert')).toBeVisible();
}

test.describe('motion', () => {
  test(
    'moves nothing while a page without api loads',
    { tag: '@no-api' },
    async ({ page, guard }) => {
      guard.allowHttpError('/nimporte-quoi');
      const motion = await recordMotionFromLoad(page);

      await page.goto('/nimporte-quoi', { waitUntil: 'networkidle' });
      await page.goto('/contact', { waitUntil: 'networkidle' });

      expect(await motion()).toEqual([]);
    },
  );

  test('moves nothing while the pages of the api load or follow each other', async ({ page }) => {
    const motion = await recordMotionFromLoad(page);

    for (const path of ['/', '/projects', '/articles', '/series', '/about']) {
      await page.goto(path, { waitUntil: 'networkidle' });
    }
    await page.goto('/', { waitUntil: 'networkidle' });
    await page.getByRole('contentinfo').getByRole('link', { name: 'Articles' }).click();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Articles');

    expect(await motion()).toEqual([]);
  });

  test(
    'reveals the mobile navigation, and closes it at once',
    { tag: '@no-api' },
    async ({ page, guard }, info) => {
      test.skip(info.project.name !== 'mobile', 'navigation dépliable sous 64 rem seulement');
      guard.allowHttpError('/nimporte-quoi');
      await page.goto('/nimporte-quoi', { waitUntil: 'networkidle' });
      const toggle = page.getByRole('button', { name: 'Menu' });
      const nav = page.getByRole('navigation', { name: 'Navigation principale' });
      const transitions = await recordTransitions(page, NAV_LIST);

      await toggle.click();
      await expect(nav).toBeVisible();
      await expect.poll(transitions).toEqual(['opacity', 'transform']);

      // Ouvrir et fermer vite : l'état affiché suit toujours le bouton
      for (let i = 0; i < 5; i++) {
        await toggle.click();
      }
      await expect(toggle).toHaveAttribute('aria-expanded', 'false');
      await expect(nav).toBeHidden();
      await toggle.click();
      await expect(toggle).toHaveAttribute('aria-expanded', 'true');
      await expect(nav).toBeVisible();

      await page.keyboard.press('Escape');
      await expect(nav).toBeHidden();
      await expect(toggle).toBeFocused();
    },
  );

  test('brings in the outcome of the contact form', { tag: '@no-api' }, async ({ page, guard }) => {
    guard.allowHttpError(CONTACT_API);
    await page.goto('/contact', { waitUntil: 'networkidle' });
    const transitions = await recordTransitions(page, 'app-alert');

    await failContactMessage(page);

    await expect.poll(transitions).toEqual(['opacity', 'transform']);
  });

  test.describe('with reduced motion', () => {
    test.use({ reducedMotion: 'reduce' });

    test(
      'opens the mobile navigation without moving it',
      { tag: '@no-api' },
      async ({ page, guard }, info) => {
        test.skip(info.project.name !== 'mobile', 'navigation dépliable sous 64 rem seulement');
        guard.allowHttpError('/nimporte-quoi');
        await page.goto('/nimporte-quoi', { waitUntil: 'networkidle' });
        const transitions = await recordTransitions(page, NAV_LIST);

        await page.getByRole('button', { name: 'Menu' }).click();

        await expect(page.getByRole('navigation', { name: 'Navigation principale' })).toBeVisible();
        expect(await transitions()).toEqual([]);
        await expect(page.locator(NAV_LIST)).toHaveCSS('opacity', '1');
        await expect(page.locator(NAV_LIST)).toHaveCSS('transform', 'none');
      },
    );

    test(
      'shows the outcome of the contact form without moving it',
      { tag: '@no-api' },
      async ({ page, guard }) => {
        guard.allowHttpError(CONTACT_API);
        await page.goto('/contact', { waitUntil: 'networkidle' });
        const transitions = await recordTransitions(page, 'app-alert, button');

        await failContactMessage(page);

        expect(await transitions()).toEqual([]);
        await expect(page.locator('app-alert')).toHaveCSS('transform', 'none');
      },
    );
  });
});
