import { Page } from '@playwright/test';

import { expect, expectAccessible, test } from './support/fixtures';

// Backend de développement avec un compte administrateur : ADMIN_USERNAME et ADMIN_PASSWORD lus
// dans l'environnement (ceux de deploy/.env), jamais écrits dans le dépôt ni dans les journaux.
const SESSION = '/api/admin/session';
const LOGIN = process.env['ADMIN_USERNAME'] ?? '';
const PASSWORD = process.env['ADMIN_PASSWORD'] ?? '';

/**
 * Adresse de client propre au test, transmise comme le ferait le mandataire inverse : la limite de
 * 5 échecs par adresse en 15 minutes (D-CQ) ne dépend pas des exécutions précédentes.
 */
async function signInFromItsOwnAddress(page: Page): Promise<void> {
  const address = `203.0.113.${Math.floor(Math.random() * 250) + 1}`;
  await page.route(`**${SESSION}`, (route) =>
    route.request().method() === 'POST'
      ? route.continue({ headers: { ...route.request().headers(), 'x-forwarded-for': address } })
      : route.continue(),
  );
}

async function openLogin(page: Page): Promise<void> {
  await page.goto('/admin', { waitUntil: 'networkidle' });
  await expect(page).toHaveURL('/admin/login');
  await expect(page.getByLabel('Identifiant')).toBeFocused();
}

async function signIn(page: Page, login: string, password: string): Promise<void> {
  await page.getByLabel('Identifiant').fill(login);
  await page.getByLabel('Mot de passe').fill(password);
  await page.getByLabel('Mot de passe').press('Enter');
}

test.describe('admin authentication', () => {
  test.beforeEach(({ guard }) => {
    test.skip(!LOGIN || !PASSWORD, 'ADMIN_USERNAME et ADMIN_PASSWORD absents de l’environnement');
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

    await signIn(page, LOGIN, `${PASSWORD}-faux`);

    await expect(page.getByRole('alert')).toContainText('Identifiant ou mot de passe incorrect');
    await expect(page.getByLabel('Identifiant')).toHaveValue(LOGIN);
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

    await signIn(page, LOGIN, 'peu importe');

    await expect(page.getByRole('alert')).toContainText('Réessayez dans 14 minutes.');
  });

  test('opens the session with the csrf token, then closes it', async ({ page, context }) => {
    await openLogin(page);
    await signInFromItsOwnAddress(page);
    const post = page.waitForRequest(
      (request) => request.url().endsWith(SESSION) && request.method() === 'POST',
    );

    await signIn(page, LOGIN, PASSWORD);

    expect((await post).headers()['x-xsrf-token']).toBeTruthy();
    await expect(page).toHaveURL('/admin');
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Tableau de bord');
    await expect(page.getByRole('main')).toContainText(`Connecté en tant que ${LOGIN}.`);
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
    await signIn(page, LOGIN, PASSWORD);
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
