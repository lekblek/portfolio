import { Page } from '@playwright/test';

import {
  ADMIN_LOGIN,
  ADMIN_PASSWORD,
  NO_ADMIN_ACCOUNT,
  SESSION,
  signInToDashboard,
} from './support/admin';
import { expect, expectAccessible, test } from './support/fixtures';

// Médiathèque (F25) : envoi réel avec le backend de développement, refus local et du serveur,
// texte alternatif, suppression refusée puis réelle. Chaque test supprime les médias qu'il envoie.
const API = '/api/admin/media';

/** PNG valide d'un pixel : le serveur en lit les dimensions dans l'en-tête. */
const PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
  'base64',
);
const PDF = Buffer.from('%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n');

function unique(name: string, project: string): string {
  return `e2e-${project}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}-${name}`;
}

async function openMedia(page: Page): Promise<void> {
  await signInToDashboard(page);
  await page.goto('/admin/media', { waitUntil: 'networkidle' });
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Médias');
}

async function send(page: Page, files: { name: string; mimeType: string; buffer: Buffer }[]) {
  await page.locator('input[type="file"]').setInputFiles(files);
  await expect(page.getByRole('button', { name: 'Effacer la liste des envois' })).toBeVisible();
}

async function remove(page: Page, name: string): Promise<void> {
  await page.getByRole('button', { name: `Supprimer le média «\u00a0${name}\u00a0»` }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Supprimer le média' }).click();
  await expect(page.locator('app-toast-region')).toContainText(
    `Média «\u00a0${name}\u00a0» supprimé.`,
  );
}

test.describe('admin media', () => {
  test.beforeEach(({ guard }) => {
    test.skip(!ADMIN_LOGIN || !ADMIN_PASSWORD, NO_ADMIN_ACCOUNT);
    guard.allowHttpError(SESSION);
    // Les tests tournent en parallèle sur la même médiathèque : la liste peut montrer un média
    // qu'un autre test vient de supprimer, dont la vignette répond alors 404
    guard.allowHttpError(/\/api\/public\/media\//);
  });

  test('sends an image and a pdf, refuses the rest, then describes the image', async ({
    page,
    guard,
  }, info) => {
    guard.allowHttpError(API);
    const image = unique('photo.png', info.project.name);
    const pdf = unique('cv.pdf', info.project.name);
    await openMedia(page);
    await expectAccessible(page);

    await send(page, [
      { name: image, mimeType: 'image/png', buffer: PNG },
      { name: pdf, mimeType: 'application/pdf', buffer: PDF },
      { name: 'faux.png', mimeType: 'image/png', buffer: Buffer.from('pas une image') },
      { name: 'notes.txt', mimeType: 'text/plain', buffer: Buffer.from('texte') },
    ]);

    const uploads = page.locator('[aria-labelledby="envois"]');
    await expect(uploads).toContainText('Format refusé');
    await expect(uploads).toContainText('le contenu n’est ni une image PNG, JPEG, WebP, ni un PDF');
    await expect(page.locator('app-toast-region')).toContainText('2 fichiers envoyés.');
    const imageRow = page.getByRole('row', { name: new RegExp(image) });
    await expect(imageRow).toContainText('Manquant');
    await expect(page.getByRole('row', { name: new RegExp(pdf) })).toContainText('PDF');
    await expectAccessible(page);

    await uploads.getByRole('link', { name: `Écrire le texte alternatif de ${image}` }).click();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(
      `Média «\u00a0${image}\u00a0»`,
    );
    await expectAccessible(page);
    await page.getByLabel('Texte alternatif').fill('Un point de couleur, image de démonstration.');
    await page.getByRole('button', { name: 'Enregistrer le texte alternatif' }).click();

    await expect(page).toHaveURL('/admin/media');
    await expect(page.locator('app-toast-region')).toContainText('Texte alternatif de');
    await expect(page.getByRole('row', { name: new RegExp(image) })).toContainText(
      'Un point de couleur',
    );
    await remove(page, image);
    await remove(page, pdf);
  });

  test('says when the server finds a file too heavy', async ({ page, guard }) => {
    guard.allowHttpError(API);
    await openMedia(page);
    await page.route(`**${API}`, (route) =>
      route.request().method() === 'POST'
        ? route.fulfill({ status: 413, json: { status: 413, code: 'MEDIA_TOO_LARGE' } })
        : route.continue(),
    );

    await send(page, [{ name: 'lourd.png', mimeType: 'image/png', buffer: PNG }]);

    await expect(page.locator('[aria-labelledby="envois"]')).toContainText(
      'fichier trop lourd pour son format',
    );
  });

  test('keeps a medium still used by a content, and says why', async ({ page, guard }, info) => {
    guard.allowHttpError(/\/api\/admin\/media\/\d+$/);
    const image = unique('utilisee.png', info.project.name);
    await openMedia(page);
    await send(page, [{ name: image, mimeType: 'image/png', buffer: PNG }]);
    await expect(page.getByRole('row', { name: new RegExp(image) })).toBeVisible();
    await page.route(/\/api\/admin\/media\/\d+$/, (route) =>
      route.request().method() === 'DELETE'
        ? route.fulfill({ status: 409, json: { status: 409, code: 'MEDIA_STILL_REFERENCED' } })
        : route.continue(),
    );

    await page.getByRole('button', { name: `Supprimer le média «\u00a0${image}\u00a0»` }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Supprimer le média' }).click();

    await expect(page.locator('app-toast-region')).toContainText(
      'il est encore utilisé par un contenu',
    );
    await expect(page.getByRole('row', { name: new RegExp(image) })).toBeVisible();
    await page.unroute(/\/api\/admin\/media\/\d+$/);
    await page
      .locator('.toast-danger')
      .getByRole('button', { name: 'Fermer la notification' })
      .click();
    await remove(page, image);
  });
});
