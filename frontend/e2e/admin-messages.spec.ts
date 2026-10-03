import {
  ADMIN_LOGIN,
  ADMIN_PASSWORD,
  NO_ADMIN_ACCOUNT,
  SESSION,
  signInToDashboard,
} from './support/admin';
import { expect, expectAccessible, test } from './support/fixtures';

// Messages de contact (F30) : un message reçu par l'API publique (adresse de client propre au
// test, comme derrière le mandataire), lu, avancé, puis supprimé définitivement ; chaque test
// efface le message qu'il crée.
const PUBLIC_API = '/api/public/contact-messages';

test.describe('admin messages', () => {
  test.beforeEach(({ guard }) => {
    test.skip(!ADMIN_LOGIN || !ADMIN_PASSWORD, NO_ADMIN_ACCOUNT);
    guard.allowHttpError(SESSION);
  });

  test('reads a message, moves it forward, then deletes it for good', async ({
    page,
    request,
    guard,
  }, info) => {
    const subject = `Recette ${info.project.name} ${Date.now().toString(36)}`;
    const sent = await request.post(PUBLIC_API, {
      data: {
        name: 'Alice Recette',
        email: 'alice.recette@example.test',
        subject,
        message: 'Bonjour,\n\nCeci est un message de recette.',
      },
      headers: { 'X-Forwarded-For': `198.51.100.${Math.floor(Math.random() * 250) + 1}` },
    });
    expect(sent.ok()).toBe(true);

    await signInToDashboard(page);
    await page.getByRole('link', { name: 'Messages non lus' }).click();
    await expect(page).toHaveURL('/admin/messages?status=NEW');
    await expectAccessible(page);
    await page.getByRole('link', { name: subject }).click();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(subject);
    await expect(page.locator('.message-body')).toContainText('Ceci est un message de recette.');
    await expect(page.getByRole('link', { name: /^Répondre/ })).toHaveAttribute(
      'href',
      /^mailto:alice\.recette@example\.test\?subject=/,
    );
    await expectAccessible(page);

    await page.getByRole('button', { name: 'Marquer comme lu' }).click();
    await expect(page.locator('app-status-badge')).toHaveText('Lu');
    await page.getByRole('button', { name: 'Archiver' }).click();
    await expect(page.locator('app-status-badge')).toHaveText('Archivé');
    await expect(page.getByRole('button', { name: 'Marquer comme lu' })).toHaveCount(0);

    const url = page.url();
    await page.getByRole('button', { name: 'Supprimer définitivement' }).click();
    const dialog = page.getByRole('dialog', { name: /Supprimer le message de Alice Recette/ });
    await expect(dialog.getByRole('button', { name: 'Annuler' })).toBeFocused();
    await dialog.getByRole('button', { name: 'Supprimer définitivement' }).click();
    await expect(page).toHaveURL('/admin/messages');
    await expect(page.locator('app-toast-region')).toContainText('supprimé définitivement');

    guard.allowHttpError(/\/api\/admin\/contact-messages\/\d+$/);
    await page.goto(url, { waitUntil: 'networkidle' });
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Message introuvable');
  });
});
