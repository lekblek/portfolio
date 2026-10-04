import { Page } from '@playwright/test';

import {
  ADMIN_LOGIN,
  ADMIN_PASSWORD,
  NO_ADMIN_ACCOUNT,
  SESSION,
  signInToDashboard,
} from './support/admin';
import { expect, expectAccessible, test } from './support/fixtures';

// Publications (F28) : parcours critique (étape 52.4) sur un article de recette : brouillon,
// publication, vérification sur /articles/:slug, archivage, 404 publique. L'API ne supprime pas
// une publication (D-CU) : l'article de recette est réutilisé d'une exécution à l'autre et reste
// archivé ; seul le projet desktop l'enregistre (`--repeat-each` demande `--workers=1`).
const API = '/api/admin/publications';
const RECIPE = 'Article de recette E2E';
const RECIPE_SLUG = 'article-de-recette-e2e';

async function openPublications(page: Page, query = ''): Promise<void> {
  await signInToDashboard(page);
  await page.goto(`/admin/publications${query}`, { waitUntil: 'networkidle' });
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Publications');
}

test.describe('admin publications', () => {
  test.beforeEach(({ guard }) => {
    test.skip(!ADMIN_LOGIN || !ADMIN_PASSWORD, NO_ADMIN_ACCOUNT);
    guard.allowHttpError(SESSION);
  });

  test('lists the publications by type and status, keeping the filters in the address', async ({
    page,
  }) => {
    await openPublications(page);
    await expect(page.locator('tbody tr').first()).toBeVisible();
    await expectAccessible(page);

    await page.getByLabel('Type').selectOption('NEWS');
    await page.getByLabel('Statut').selectOption('PUBLISHED');
    await page.getByRole('button', { name: 'Filtrer' }).click();
    await expect(page).toHaveURL('/admin/publications?type=NEWS&status=PUBLISHED');
    for (const badge of await page.locator('tbody app-status-badge').allInnerTexts()) {
      expect(badge.trim()).toBe('Publiée');
    }
    for (const type of await page.locator('tbody td:nth-child(2)').allInnerTexts()) {
      expect(type.trim()).toBe('Actualité');
    }
    await expectAccessible(page);
  });

  test('refuses a past date to schedule a draft', async ({ page }) => {
    await openPublications(page, '?status=DRAFT');
    await page
      .locator('tbody tr')
      .first()
      .getByRole('link', { name: /^Modifier/ })
      .click();
    await page.getByRole('button', { name: 'Programmer' }).click();
    const when = page.getByLabel('Date et heure de publication');
    await expect(when).toBeFocused();
    await when.fill('2020-01-01T10:00');
    await page
      .locator('app-publication-status-panel')
      .getByRole('button', { name: 'Programmer' })
      .click();
    await expect(when).toHaveAccessibleDescription(/Choisissez un moment à venir\./);
    await expectAccessible(page);
    await page.getByRole('button', { name: 'Annuler' }).click();
  });

  test('publishes the recipe article, shows it on the site, then archives it', async ({
    page,
    guard,
  }, info) => {
    test.skip(
      info.project.name !== 'desktop',
      'Article de recette unique : un seul projet l’enregistre',
    );
    guard.allowHttpError(`/api/public/publications/${RECIPE_SLUG}`);
    guard.allowHttpError(`/articles/${RECIPE_SLUG}`);
    await openPublications(page);
    const all = await (await page.request.get(`${API}?type=ARTICLE&size=100`)).json();
    const existing = all.content.find((item: { slug: string }) => item.slug === RECIPE_SLUG);

    if (existing) {
      await page.goto(`/admin/publications/${existing.id}`, { waitUntil: 'networkidle' });
    } else {
      await page.getByRole('link', { name: 'Nouvel article' }).click();
      await page.getByLabel('Titre', { exact: true }).fill(RECIPE);
      await page.getByLabel('Résumé').fill('Article de recette des tests de bout en bout.');
      await page.getByRole('tab', { name: 'Markdown' }).click();
      await page
        .getByRole('textbox', { name: 'Contenu (Markdown)' })
        .fill('## Recette\n\nUn paragraphe et `du code`.');
      await page.getByRole('button', { name: 'Créer le brouillon' }).click();
      await expect(page.locator('app-toast-region')).toContainText('Brouillon');
    }
    const panel = page.locator('app-publication-status-panel');
    await expect(panel.getByRole('heading', { name: 'Statut' })).toBeVisible();
    await expectAccessible(page);

    // Publier (depuis un brouillon) ou republier (depuis l'archive)
    await panel.getByRole('button', { name: /^(Publier maintenant|Republier)$/ }).click();
    await expect(panel.locator('app-status-badge')).toHaveText('Publiée');
    await page.goto(`/articles/${RECIPE_SLUG}`, { waitUntil: 'networkidle' });
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(RECIPE);

    await page.goBack({ waitUntil: 'networkidle' });
    await expect(page.getByLabel('Slug')).toHaveAttribute('readonly', '');
    await panel.getByRole('button', { name: 'Archiver' }).click();
    const dialog = page.getByRole('dialog', { name: /^Archiver/ });
    await expect(dialog.getByRole('button', { name: 'Annuler' })).toBeFocused();
    await dialog.getByRole('button', { name: 'Archiver' }).click();
    await expect(panel.locator('app-status-badge')).toHaveText('Archivée');

    const response = await page.goto(`/articles/${RECIPE_SLUG}`);
    expect(response?.status()).toBe(404);
  });
});
