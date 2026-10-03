import { Page } from '@playwright/test';

import {
  ADMIN_LOGIN,
  ADMIN_PASSWORD,
  NO_ADMIN_ACCOUNT,
  SESSION,
  signInToDashboard,
} from './support/admin';
import { expect, expectAccessible, test } from './support/fixtures';

// Profil (F26) : enregistrement réel avec le backend de développement, vérifié sur « À propos »,
// puis profil d'origine rétabli quoi qu'il arrive ; erreurs, sélecteur de médias et sortie sans
// enregistrer. Le profil est unique : seul le projet desktop l'enregistre, les autres tests
// n'envoient rien ; répéter ce test (`--repeat-each`) demande `--workers=1`, sinon deux exécutions
// rétablissent chacune un état que l'autre a modifié. Le nom, le titre et la présentation courte
// ne changent jamais (pages publiques testées en parallèle).
const API = '/api/admin/profile';

async function openProfile(page: Page): Promise<void> {
  await signInToDashboard(page);
  await page.goto('/admin/profile', { waitUntil: 'networkidle' });
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Profil');
  await expect(page.getByRole('button', { name: 'Enregistrer le profil' })).toBeVisible();
}

/** Requête d'écriture de l'API, avec le jeton CSRF de la session du navigateur. */
async function xsrfHeaders(page: Page): Promise<Record<string, string>> {
  const cookies = await page.context().cookies();
  const token = cookies.find((cookie) => cookie.name === 'XSRF-TOKEN')?.value ?? '';
  return { 'X-XSRF-TOKEN': token };
}

test.describe('admin profile', () => {
  test.beforeEach(({ guard }) => {
    test.skip(!ADMIN_LOGIN || !ADMIN_PASSWORD, NO_ADMIN_ACCOUNT);
    guard.allowHttpError(SESSION);
  });

  test('adds a link, moves it up with the keyboard and publishes it on the about page', async ({
    page,
  }, info) => {
    test.skip(info.project.name !== 'desktop', 'Profil unique : un seul projet l’enregistre');
    await signInToDashboard(page);
    const original = await (await page.request.get(API)).json();
    const label = `E2E ${Date.now().toString(36)}`;

    try {
      await page.getByRole('link', { name: 'Profil' }).click();
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Profil');
      await expect(page.getByRole('button', { name: 'Enregistrer le profil' })).toBeVisible();
      const links = page.locator('#profil-liens');
      const before = await links.getByRole('listitem').count();

      await page.getByRole('button', { name: 'Ajouter un lien' }).click();
      const added = links.getByRole('listitem').last();
      await expect(added.getByRole('textbox', { name: 'Libellé' })).toBeFocused();
      await page.keyboard.type(label);
      await added.getByRole('textbox', { name: 'Adresse' }).fill('https://example.test/e2e');

      if (before > 0) {
        const up = page.getByRole('button', { name: `Monter le lien «\u00a0${label}\u00a0»` });
        await up.focus();
        await page.keyboard.press('Enter');
        await expect(links.getByRole('listitem').nth(before - 1)).toContainText(
          `${before} / ${before + 1}`,
        );
        await expect(
          links
            .getByRole('listitem')
            .nth(before - 1)
            .getByRole('textbox', { name: 'Libellé' }),
        ).toHaveValue(label);
        await expect(up).toBeFocused();
      }
      await expectAccessible(page);

      await page.getByRole('button', { name: 'Enregistrer le profil' }).click();
      await expect(page.locator('app-toast-region')).toContainText('Profil enregistré.');

      await page.goto('/about', { waitUntil: 'networkidle' });
      const published = page.getByRole('list', { name: 'Liens professionnels' }).getByRole('link');
      await expect(published.nth(Math.max(before - 1, 0))).toHaveText(`${label} (site externe)`);
    } finally {
      const restored = await page.request.put(API, {
        data: original,
        headers: await xsrfHeaders(page),
      });
      expect(restored.status()).toBe(200);
    }
  });

  test('ties each error to its field and sends nothing while the form is invalid', async ({
    page,
  }) => {
    let sent = false;
    await page.route(`**${API}`, (route) => {
      sent ||= route.request().method() === 'PUT';
      return route.continue();
    });
    await openProfile(page);
    await expectAccessible(page);

    await page.getByRole('button', { name: 'Ajouter une expérience' }).click();
    const entry = page.locator('#profil-experiences').getByRole('listitem').last();
    await expect(entry.getByRole('textbox', { name: 'Intitulé du poste' })).toBeFocused();
    await entry.getByLabel('Début').fill('2024-05-01');
    await entry.getByLabel('Fin').fill('2023-01-01');
    await page.getByRole('button', { name: 'Enregistrer le profil' }).click();

    await expect(entry.getByLabel('Fin')).toHaveAttribute('aria-invalid', 'true');
    await expect(entry.getByLabel('Fin')).toHaveAccessibleDescription(/La fin précède le début\./);
    await expect(entry.getByRole('textbox', { name: 'Intitulé du poste' })).toBeFocused();
    expect(sent).toBe(false);
    await expectAccessible(page);
    expect(await page.evaluate(() => document.documentElement.scrollWidth - innerWidth)).toBe(0);
  });

  test('dismisses the media picker, then asks before leaving with unsaved changes', async ({
    page,
  }) => {
    await openProfile(page);
    const choose = page.getByRole('button', { name: /^(Choisir|Changer) l’avatar$/ });

    await choose.click();
    const dialog = page.getByRole('dialog', { name: 'Choisir l’avatar' });
    await expect(dialog).toBeVisible();
    await expect(
      dialog.getByRole('link', { name: 'Envoyer un fichier dans la médiathèque' }),
    ).toBeVisible();
    await expectAccessible(page);
    await page.keyboard.press('Escape');
    await expect(dialog).toBeHidden();
    await expect(choose).toBeFocused();

    await page.locator('#profil-lieu').fill('Quelque part');
    await page.getByRole('link', { name: 'À propos' }).first().click();
    const confirm = page.getByRole('dialog', { name: /^Quitter sans enregistrer/ });
    await expect(confirm).toBeVisible();
    await confirm.getByRole('button', { name: 'Rester sur la page' }).click();
    await expect(page).toHaveURL(/\/admin\/profile/);
    await page.getByRole('link', { name: 'À propos' }).first().click();
    await confirm.getByRole('button', { name: 'Quitter sans enregistrer' }).click();
    await expect(page).toHaveURL('/about');
  });
});
