import { expect, expectAccessible, test } from './support/fixtures';

// Première page qui lit l'API : ces spécifications demandent le backend de développement
// (profil `dev`), comme le rendu serveur qu'elles vérifient.
const PROFILE_API = '/api/public/profile';

test.describe('about page', () => {
  test('is rendered by the server with the profile of the api', async ({ page, request }) => {
    const html = await (await request.get('/about')).text();
    const profile = await (await request.get(PROFILE_API)).json();

    expect(html).toContain(`>${profile.displayName}</h1>`);
    expect(html).toContain('"@type":"ProfilePage"');

    await page.goto('/about');
    await expect(page).toHaveTitle('À propos — Blek Ngossanga');
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(profile.displayName);
  });

  test('reuses the server response instead of calling the api again', async ({ page }) => {
    const calls: string[] = [];
    page.on('request', (request) => {
      if (request.url().includes(PROFILE_API)) {
        calls.push(request.url());
      }
    });

    // Hydratation comprise : toute requête du navigateur part pendant le démarrage
    await page.goto('/about', { waitUntil: 'networkidle' });

    expect(calls).toEqual([]);
  });

  test('is accessible and marks its navigation link as the current page', async ({ page }) => {
    await page.goto('/about');

    // Sur mobile, la liste est dans le menu dépliable
    const toggle = page.getByRole('button', { name: 'Menu' });
    if (await toggle.isVisible()) {
      await toggle.click();
    }
    const nav = page.getByRole('navigation', { name: 'Navigation principale' });
    await expect(nav.getByRole('link', { name: 'À propos' })).toHaveAttribute(
      'aria-current',
      'page',
    );
    await expect(page.getByRole('heading', { level: 1 })).toHaveCount(1);
    await expectAccessible(page);
  });

  test('never shows the public email address', async ({ page, request }) => {
    const profile = await (await request.get(PROFILE_API)).json();

    await page.goto('/about');

    await expect(page.locator('a[href^="mailto:"]')).toHaveCount(0);
    if (profile.publicEmail) {
      await expect(page.getByText(profile.publicEmail)).toHaveCount(0);
    }
  });

  test('announces loading during a client navigation', async ({ page, guard }) => {
    guard.allowHttpError('/nimporte-quoi');
    await page.goto('/nimporte-quoi');
    await page.route(`**${PROFILE_API}`, async (route) => {
      await new Promise((resolve) => setTimeout(resolve, 800));
      await route.continue();
    });

    await openAboutFromNavigation(page);

    await expect(page.getByRole('status')).toHaveText('Chargement du profil…');
    await expect(page.getByRole('heading', { level: 1 })).not.toHaveText('Page introuvable');
    await expect(page.getByRole('status')).toHaveCount(0);
  });

  test('offers to retry when the api fails', async ({ page, guard }) => {
    guard.allowHttpError('/nimporte-quoi');
    guard.allowHttpError(PROFILE_API);
    await page.goto('/nimporte-quoi');
    let failures = 1;
    await page.route(`**${PROFILE_API}`, (route) =>
      failures-- > 0
        ? route.fulfill({
            status: 500,
            contentType: 'application/problem+json',
            body: JSON.stringify({ status: 500, code: 'INTERNAL_ERROR' }),
          })
        : route.continue(),
    );

    await openAboutFromNavigation(page);

    const alert = page.getByRole('alert');
    await expect(alert).toContainText('Le profil n’a pas pu être chargé.');
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Profil indisponible');
    await expectAccessible(page);

    await alert.getByRole('button', { name: 'Réessayer' }).click();

    await expect(page.getByRole('alert')).toHaveCount(0);
    await expect(page.getByRole('heading', { level: 1 })).not.toHaveText('Profil indisponible');
  });
});

/** Suit le lien « À propos » de la navigation, en dépliant d'abord le menu sur mobile. */
async function openAboutFromNavigation(page: import('@playwright/test').Page): Promise<void> {
  const toggle = page.getByRole('button', { name: 'Menu' });
  if (await toggle.isVisible()) {
    await toggle.click();
  }
  await page
    .getByRole('navigation', { name: 'Navigation principale' })
    .getByRole('link', { name: 'À propos' })
    .click();
}
