import { Page } from '@playwright/test';

import {
  ADMIN_LOGIN,
  ADMIN_PASSWORD,
  NO_ADMIN_ACCOUNT,
  SESSION,
  signInToDashboard,
  unfoldMenuIfFolded,
} from './support/admin';
import { expect, expectAccessible, test } from './support/fixtures';

// Taxonomie (F24) : liste, création, modification, suppression confirmée, refus d'un terme utilisé,
// modifications non enregistrées. Chaque test crée ses propres termes et les supprime.

function uniqueName(prefix: string, project: string): string {
  return `${prefix} ${project} ${Date.now()} ${Math.random().toString(36).slice(2, 8)}`;
}

/** Champ Nom du formulaire (le libellé « Nom » est aussi celui d'une colonne de la liste). */
function nameField(page: Page) {
  return page.getByRole('textbox', { name: 'Nom', exact: true });
}

async function openTags(page: Page): Promise<void> {
  await signInToDashboard(page);
  await page.goto('/admin/taxonomy/tags', { waitUntil: 'networkidle' });
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Tags');
  await expect(page.getByRole('region', { name: 'Tags' })).toBeVisible();
}

async function createTag(page: Page, name: string): Promise<void> {
  await page.getByRole('link', { name: 'Nouveau tag' }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Nouveau tag');
  await nameField(page).fill(name);
  await nameField(page).press('Enter');
  await expect(page).toHaveURL('/admin/taxonomy/tags');
  // Liste rechargée avant toute autre action : la quitter plus tôt annulerait sa requête
  await expect(page.getByRole('row', { name: new RegExp(name) })).toBeVisible();
}

async function deleteTag(page: Page, name: string): Promise<void> {
  await page.getByRole('button', { name: `Supprimer le tag «\u00a0${name}\u00a0»` }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Supprimer le tag' }).click();
}

test.describe('admin taxonomy', () => {
  test.beforeEach(({ guard }) => {
    test.skip(!ADMIN_LOGIN || !ADMIN_PASSWORD, NO_ADMIN_ACCOUNT);
    guard.allowHttpError(SESSION);
  });

  test('creates, renames and deletes a tag, with a notification each time', async ({
    page,
  }, info) => {
    const name = uniqueName('Essai e2e', info.project.name);
    await openTags(page);
    await expect(page.getByRole('region', { name: 'Tags' })).toBeVisible();
    await expectAccessible(page);

    await createTag(page, name);
    await expect(page.locator('app-toast-region')).toContainText(`Tag «\u00a0${name}\u00a0» créé.`);
    const row = page.getByRole('row', { name: new RegExp(name) });
    await expect(row.getByRole('cell').first()).toHaveText(/^essai-e2e-/);

    await row.getByRole('link', { name: `Modifier le tag «\u00a0${name}\u00a0»` }).click();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(
      `Modifier le tag «\u00a0${name}\u00a0»`,
    );
    await expect(nameField(page)).toHaveValue(name);
    await expectAccessible(page);
    await nameField(page).fill(`${name} bis`);
    await page.getByRole('button', { name: 'Enregistrer les modifications' }).click();
    await expect(page).toHaveURL('/admin/taxonomy/tags');
    await expect(page.locator('app-toast-region')).toContainText(
      `Tag «\u00a0${name} bis\u00a0» enregistré.`,
    );

    await page.getByRole('button', { name: `Supprimer le tag «\u00a0${name} bis\u00a0»` }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText(`Supprimer le tag «\u00a0${name} bis\u00a0»\u202f?`);
    await expect(dialog.getByRole('button', { name: 'Annuler' })).toBeFocused();
    await expectAccessible(page);
    await dialog.getByRole('button', { name: 'Supprimer le tag' }).click();

    await expect(page.locator('app-toast-region')).toContainText(
      `Tag «\u00a0${name} bis\u00a0» supprimé.`,
    );
    await expect(page.getByRole('row', { name: new RegExp(name) })).toHaveCount(0);
    await expect(page.getByRole('heading', { level: 1 })).toBeFocused();
  });

  test('closes the dialog with escape and gives the focus back', async ({ page }) => {
    await openTags(page);
    const trigger = page.getByRole('button', { name: /^Supprimer le tag/ }).first();

    await trigger.focus();
    await page.keyboard.press('Enter');
    await expect(page.getByRole('dialog')).toBeVisible();
    await page.keyboard.press('Escape');

    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(trigger).toBeFocused();
  });

  test('refuses to delete a tag used by a publication, and says why', async ({ page, guard }) => {
    guard.allowHttpError(/\/api\/admin\/tags\/\d+$/);
    const publications = await (await page.request.get('/api/public/publications')).json();
    const used = publications.content.flatMap((item: { tags: { name: string }[] }) => item.tags)[0];
    test.skip(!used, 'aucun tag utilisé dans la base de développement');
    await openTags(page);

    await deleteTag(page, used.name);

    await expect(page.locator('app-toast-region')).toContainText(
      'est encore utilisé par une publication',
    );
    await expect(page.getByRole('row', { name: new RegExp(used.name) }).first()).toBeVisible();
  });

  test('shows a name already used under the name field', async ({ page, guard }, info) => {
    guard.allowHttpError('/api/admin/tags');
    const name = uniqueName('Essai doublon', info.project.name);
    await openTags(page);
    await createTag(page, name);

    await page.getByRole('link', { name: 'Nouveau tag' }).click();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Nouveau tag');
    await nameField(page).fill(name.toUpperCase());
    await page.getByRole('button', { name: 'Créer le tag' }).click();

    await expect(nameField(page)).toBeFocused();
    await expect(nameField(page)).toHaveAttribute('aria-invalid', 'true');
    await expect(page.locator('#terme-nom-erreur')).toContainText('Ce nom est déjà pris');
    await expectAccessible(page);

    await page.getByRole('link', { name: 'Annuler' }).click();
    // La saisie n'a pas été enregistrée : la page demande confirmation avant de partir
    await page
      .getByRole('dialog')
      .getByRole('button', { name: 'Quitter sans enregistrer' })
      .click();
    await expect(page).toHaveURL('/admin/taxonomy/tags');
    await deleteTag(page, name);
    await expect(page.locator('app-toast-region')).toContainText('supprimé.');
  });

  test('keeps unsaved changes when asked to stay', async ({ page }) => {
    await openTags(page);
    await page.getByRole('link', { name: 'Nouveau tag' }).click();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Nouveau tag');
    await nameField(page).fill('Saisie en cours');

    await unfoldMenuIfFolded(page);
    await page
      .getByRole('navigation', { name: 'Administration' })
      .getByRole('link', { name: 'Tableau de bord' })
      .click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText('Quitter sans enregistrer');
    await dialog.getByRole('button', { name: 'Rester sur la page' }).click();

    await expect(page).toHaveURL('/admin/taxonomy/tags/new');
    await expect(nameField(page)).toHaveValue('Saisie en cours');
  });
});
