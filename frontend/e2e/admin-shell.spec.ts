import { Page } from '@playwright/test';

import {
  ADMIN_LOGIN,
  ADMIN_PASSWORD,
  NO_ADMIN_ACCOUNT,
  openLogin,
  SESSION,
  signIn,
  signInFromItsOwnAddress,
  signInToDashboard,
  unfoldMenuIfFolded,
} from './support/admin';
import { expect, expectAccessible, test } from './support/fixtures';

// Shell d'administration et tableau de bord (F23), avec le backend de développement et un compte
// administrateur (support/admin.ts).

const LISTS = ['contact-messages', 'publications', 'projects', 'series', 'media'];

/**
 * Totaux reçus par la page elle-même, liste par liste (réponses réelles transmises telles quelles) :
 * d'autres tests, en parallèle, peuvent ajouter des messages entre deux lectures de l'API.
 */
async function recordTotals(page: Page): Promise<Map<string, string>> {
  const totals = new Map<string, string>();
  for (const list of LISTS) {
    await page.route(`**/api/admin/${list}?**`, async (route) => {
      const response = await route.fetch();
      const body = await response.json();
      totals.set(list, String(body.totalElements));
      await route.fulfill({ response, json: body });
    });
  }
  return totals;
}

test.describe('admin shell', () => {
  test.beforeEach(({ guard }) => {
    test.skip(!ADMIN_LOGIN || !ADMIN_PASSWORD, NO_ADMIN_ACCOUNT);
    // Sans session, GET /api/admin/session répond 401 : c'est la réponse attendue
    guard.allowHttpError(SESSION);
  });

  test('frames the administration and reads the tally from the lists', async ({ page }) => {
    const totals = await recordTotals(page);
    await signInToDashboard(page);

    await expect(page).toHaveTitle('Tableau de bord — Blek Ngossanga');
    await expect(page.getByRole('main')).toHaveCount(1);
    await expect(page.getByRole('heading', { level: 1 })).toHaveCount(1);
    const rows = page.locator('.dashboard-row');
    await expect(rows).toHaveCount(5);
    await expect(rows).toHaveText([
      `Messages non lus${totals.get('contact-messages')}`,
      `Publications${totals.get('publications')}`,
      `Projets${totals.get('projects')}`,
      `Séries${totals.get('series')}`,
      `Médias${totals.get('media')}`,
    ]);
    await unfoldMenuIfFolded(page);
    const nav = page.getByRole('navigation', { name: 'Administration' });
    await expect(nav.getByRole('link', { name: 'Tableau de bord' })).toHaveAttribute(
      'aria-current',
      'page',
    );
    await expect(nav.getByRole('link', { name: 'Voir le site' })).toHaveAttribute('href', '/');
    await expect(page.getByRole('banner')).toContainText(`Connecté en tant que ${ADMIN_LOGIN}`);
    await expectAccessible(page);
  });

  test('skips to the content and reaches the navigation from the keyboard', async ({
    page,
  }, info) => {
    test.skip(info.project.name !== 'desktop', 'barre latérale dès 64 rem');
    await signInToDashboard(page);
    await page.locator('body').focus();

    await page.keyboard.press('Tab');
    const skip = page.getByRole('link', { name: 'Aller au contenu' });
    await expect(skip).toBeFocused();
    await expect(skip).toBeInViewport();
    await page.keyboard.press('Tab');
    await expect(page.getByRole('link', { name: 'Blek Ngossanga' })).toBeFocused();
    await page.keyboard.press('Tab');
    await expect(page.getByRole('button', { name: 'Se déconnecter' })).toBeFocused();
    await page.keyboard.press('Tab');
    await expect(page.getByRole('link', { name: 'Tableau de bord' })).toBeFocused();

    await skip.focus();
    await page.keyboard.press('Enter');
    await expect(page.getByRole('main')).toBeFocused();
  });

  test('folds the navigation and the session behind the menu on a phone', async ({
    page,
  }, info) => {
    test.skip(info.project.name !== 'mobile', 'navigation repliée sous 64 rem');
    await signInToDashboard(page);
    const toggle = page.getByRole('button', { name: 'Menu' });
    const nav = page.getByRole('navigation', { name: 'Administration' });
    await expect(nav).toBeHidden();
    await expect(page.getByRole('button', { name: 'Se déconnecter' })).toBeHidden();

    await toggle.focus();
    await page.keyboard.press('Enter');
    await expect(toggle).toHaveAttribute('aria-expanded', 'true');
    await expect(nav).toBeVisible();
    await expect(page.getByRole('button', { name: 'Se déconnecter' })).toBeVisible();
    await expectAccessible(page);

    await page.keyboard.press('Tab');
    await page.keyboard.press('Escape');
    await expect(toggle).toHaveAttribute('aria-expanded', 'false');
    await expect(toggle).toBeFocused();
    await expect(nav).toBeHidden();
  });

  test('sends back to the login when the session expires during work', async ({ page, guard }) => {
    guard.allowHttpError('/api/admin/media');
    await openLogin(page);
    await signInFromItsOwnAddress(page);
    // Session refusée par le serveur pendant le chargement du tableau de bord, puis à la
    // vérification de la page de connexion : comme une session expirée entre deux requêtes
    const refused = { status: 401, json: { status: 401, code: 'AUTHENTICATION_REQUIRED' } };
    // Refus après les autres compteurs : quitter la page annulerait sinon leurs requêtes
    const others = Promise.all(
      ['contact-messages', 'publications', 'projects', 'series'].map((list) =>
        page.waitForResponse((response) => response.url().includes(`/api/admin/${list}?`)),
      ),
    );
    await page.route('**/api/admin/media?**', async (route) => {
      await others;
      await route.fulfill(refused);
    });
    await page.route(`**${SESSION}`, (route) =>
      route.request().method() === 'GET' ? route.fulfill(refused) : route.fallback(),
    );

    await signIn(page, ADMIN_LOGIN, ADMIN_PASSWORD);

    await expect(page).toHaveURL('/admin/login');
    const notice = page.locator('app-alert');
    await expect(notice).toContainText('Session expirée');
    await expect(notice).toBeFocused();
    await expectAccessible(page);

    await page.unroute('**/api/admin/media?**');
    await page.unroute(`**${SESSION}`);
    await signInFromItsOwnAddress(page);
    await signIn(page, ADMIN_LOGIN, ADMIN_PASSWORD);
    await expect(page).toHaveURL('/admin');
    await expect(page.locator('.dashboard-row')).toHaveCount(5);
  });
});
