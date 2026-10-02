import {
  ADMIN_LOGIN,
  ADMIN_PASSWORD,
  NO_ADMIN_ACCOUNT,
  openLogin,
  SESSION,
  signIn,
  signInFromItsOwnAddress,
  unfoldMenuIfFolded,
} from './support/admin';
import { expect, expectAccessible, test } from './support/fixtures';

// Connexion, refus, déconnexion et cloisonnement du lot d'administration (F22), avec le backend de
// développement et un compte administrateur (support/admin.ts).

test.describe('admin authentication', () => {
  test.beforeEach(({ guard }) => {
    test.skip(!ADMIN_LOGIN || !ADMIN_PASSWORD, NO_ADMIN_ACCOUNT);
    // Sans session, GET /api/admin/session répond 401 : c'est la réponse attendue
    guard.allowHttpError(SESSION);
  });

  test('sends to the login page, which is not indexed', async ({ page }) => {
    await openLogin(page);

    await expect(page).toHaveTitle('Connexion — Blek Ngossanga');
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute('content', 'noindex');
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Connexion');
    await expect(page.getByLabel('Mot de passe')).toHaveAttribute('type', 'password');
    await expectAccessible(page);
  });

  test('refuses wrong credentials from the keyboard', async ({ page }) => {
    await openLogin(page);
    await signInFromItsOwnAddress(page);

    await signIn(page, ADMIN_LOGIN, `${ADMIN_PASSWORD}-faux`);

    await expect(page.getByRole('alert')).toContainText('Identifiant ou mot de passe incorrect');
    await expect(page.getByLabel('Identifiant')).toHaveValue(ADMIN_LOGIN);
    await expect(page.getByLabel('Mot de passe')).toHaveValue('');
    await expect(page.getByLabel('Mot de passe')).toBeFocused();
    await expect(page).toHaveURL('/admin/login');
    await expectAccessible(page);
  });

  test('says how long to wait after too many attempts', async ({ page }) => {
    await openLogin(page);
    await page.route(`**${SESSION}`, (route) =>
      route.request().method() === 'POST'
        ? route.fulfill({
            status: 429,
            headers: { 'Retry-After': '840' },
            json: { status: 429, code: 'TOO_MANY_LOGIN_ATTEMPTS' },
          })
        : route.continue(),
    );

    await signIn(page, ADMIN_LOGIN, 'peu importe');

    await expect(page.getByRole('alert')).toContainText('Réessayez dans 14 minutes.');
  });

  test('opens the session with the csrf token, then closes it', async ({ page, context }) => {
    await openLogin(page);
    await signInFromItsOwnAddress(page);
    const post = page.waitForRequest(
      (request) => request.url().endsWith(SESSION) && request.method() === 'POST',
    );

    await signIn(page, ADMIN_LOGIN, ADMIN_PASSWORD);

    expect((await post).headers()['x-xsrf-token']).toBeTruthy();
    await expect(page).toHaveURL('/admin');
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Tableau de bord');
    await unfoldMenuIfFolded(page);
    await expect(page.getByRole('banner')).toContainText(
      `Connecté en tant que ${ADMIN_LOGIN}, depuis le`,
    );
    const sessionCookie = (await context.cookies()).find((cookie) => cookie.name === 'JSESSIONID');
    expect(sessionCookie?.httpOnly).toBe(true);
    expect(sessionCookie?.sameSite).toBe('Strict');
    await expectAccessible(page);

    await page.getByRole('button', { name: 'Se déconnecter' }).click();

    await expect(page).toHaveURL('/admin/login');
    await expect(page.locator('app-alert')).toContainText('Vous êtes déconnecté');
    await expect(page.locator('app-alert')).toBeFocused();
    await expectAccessible(page);
    await page.goto('/admin');
    await expect(page).toHaveURL('/admin/login');
  });

  test('asks to sign in again once the session is gone', async ({ page, context }) => {
    await openLogin(page);
    await signInFromItsOwnAddress(page);
    await signIn(page, ADMIN_LOGIN, ADMIN_PASSWORD);
    await expect(page).toHaveURL('/admin');

    // Session expirée ou supprimée par le serveur : le cookie n'est plus valable
    await context.clearCookies({ name: 'JSESSIONID' });
    await page.reload();

    await expect(page).toHaveURL('/admin/login');
    await expect(page.getByLabel('Identifiant')).toBeFocused();
  });

  test('keeps the administration out of the public pages', async ({ page }) => {
    const scripts: Promise<string>[] = [];
    const adminCalls: string[] = [];
    page.on('response', (response) => {
      if (response.request().resourceType() === 'script') {
        scripts.push(response.text());
      }
    });
    page.on('request', (request) => {
      if (request.url().includes('/api/admin/')) {
        adminCalls.push(request.url());
      }
    });

    await page.goto('/', { waitUntil: 'networkidle' });
    await page.getByRole('contentinfo').getByRole('link', { name: 'Articles' }).click();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Articles');

    expect(adminCalls).toEqual([]);
    for (const body of await Promise.all(scripts)) {
      expect(body).not.toContain('connexion-identifiant');
    }
  });
});
