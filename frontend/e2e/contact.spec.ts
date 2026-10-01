import { Page } from '@playwright/test';

import { expect, expectAccessible, test } from './support/fixtures';

// Backend de développement (profil `dev`) et Mailpit (deploy/compose.dev.yaml, interface sur 8025).
const API = '/api/public/contact-messages';
const MAILPIT = 'http://localhost:8025';

/**
 * Adresse de client propre au test, transmise comme le ferait le mandataire inverse : la limite de
 * 5 messages par heure et par adresse (D-EJ) ne dépend pas des exécutions précédentes.
 */
async function sendFromItsOwnAddress(page: Page): Promise<void> {
  const address = `198.51.100.${Math.floor(Math.random() * 250) + 1}`;
  await page.route(`**${API}`, (route) =>
    route.continue({ headers: { ...route.request().headers(), 'x-forwarded-for': address } }),
  );
}

async function fill(page: Page, subject: string): Promise<void> {
  await page.getByLabel('Nom').fill('Camille Martin');
  await page.getByLabel('Adresse électronique').fill('camille@example.com');
  await page.getByLabel('Sujet').fill(subject);
  await page.getByLabel('Message').fill('Bonjour,\nune question sur le portfolio.');
}

test.describe('contact', () => {
  test('renders a labelled form in the server html, with the trap hidden', async ({
    page,
    request,
  }) => {
    const html = await (await request.get('/contact')).text();
    expect(html).toContain('for="contact-nom"');
    expect(html).toContain('autocomplete="email"');

    await page.goto('/contact', { waitUntil: 'networkidle' });

    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Écrire un message');
    await expect(page.getByRole('textbox')).toHaveCount(4);
    await expect(page.locator('#contact-site')).not.toBeInViewport();
    await expect(page.locator('#contact-site')).toHaveAttribute('tabindex', '-1');
    await expectAccessible(page);
  });

  test('moves the focus to the first error, from the keyboard', async ({ page }) => {
    const calls: string[] = [];
    page.on('request', (r) => r.url().includes(API) && calls.push(r.url()));
    await page.goto('/contact', { waitUntil: 'networkidle' });

    await page.getByLabel('Adresse électronique').fill('camille');
    await page.getByRole('button', { name: 'Envoyer le message' }).focus();
    await page.keyboard.press('Enter');

    await expect(page.getByLabel('Nom')).toBeFocused();
    await expect(page.getByLabel('Nom')).toHaveAttribute('aria-invalid', 'true');
    await expect(page.getByText('Indiquez une adresse complète')).toBeVisible();
    expect(calls).toEqual([]);
    await expectAccessible(page);
  });

  test('sends the message, confirms it and notifies the administrator', async ({
    page,
    request,
  }) => {
    const subject = `Essai de bout en bout ${Date.now()}`;
    await page.goto('/contact', { waitUntil: 'networkidle' });
    await sendFromItsOwnAddress(page);

    await fill(page, subject);
    await page.getByRole('button', { name: 'Envoyer le message' }).click();

    await expect(page.getByText('Message envoyé')).toBeVisible();
    await expect(page.locator('app-alert')).toBeFocused();
    await expect
      .poll(async () => {
        const search = await request.get(
          `${MAILPIT}/api/v1/search?query=${encodeURIComponent(`subject:"${subject}"`)}`,
        );
        return (await search.json()).messages_count;
      })
      .toBe(1);
    await expectAccessible(page);

    await page.getByRole('button', { name: 'Écrire un autre message' }).click();
    await expect(page.getByLabel('Nom')).toBeFocused();
    await expect(page.getByLabel('Nom')).toHaveValue('');
  });

  test('keeps the text and says how long to wait after too many messages', async ({
    page,
    guard,
  }) => {
    guard.allowHttpError(API);
    await page.goto('/contact', { waitUntil: 'networkidle' });
    await page.route(`**${API}`, (route) =>
      route.fulfill({
        status: 429,
        headers: { 'Retry-After': '600' },
        json: { status: 429, code: 'TOO_MANY_CONTACT_MESSAGES' },
      }),
    );

    await fill(page, 'Refus attendu');
    await page.getByRole('button', { name: 'Envoyer le message' }).click();

    await expect(page.getByRole('alert')).toContainText('réessayez dans 10 minutes');
    await expect(page.getByLabel('Sujet')).toHaveValue('Refus attendu');
    await expectAccessible(page);
  });
});
