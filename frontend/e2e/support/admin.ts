import { Page } from '@playwright/test';

import { expect } from './fixtures';

// Backend de développement avec un compte administrateur : ADMIN_USERNAME et ADMIN_PASSWORD lus
// dans l'environnement (ceux de deploy/.env), jamais écrits dans le dépôt ni dans les journaux.
export const SESSION = '/api/admin/session';
export const ADMIN_LOGIN = process.env['ADMIN_USERNAME'] ?? '';
export const ADMIN_PASSWORD = process.env['ADMIN_PASSWORD'] ?? '';
export const NO_ADMIN_ACCOUNT = 'ADMIN_USERNAME et ADMIN_PASSWORD absents de l’environnement';

/**
 * Adresse de client propre au test, transmise comme le ferait le mandataire inverse : la limite de
 * 5 échecs par adresse en 15 minutes (D-CQ) ne dépend pas des exécutions précédentes.
 */
export async function signInFromItsOwnAddress(page: Page): Promise<void> {
  const address = `203.0.113.${Math.floor(Math.random() * 250) + 1}`;
  await page.route(`**${SESSION}`, (route) =>
    route.request().method() === 'POST'
      ? route.continue({ headers: { ...route.request().headers(), 'x-forwarded-for': address } })
      : route.continue(),
  );
}

/** Page de connexion atteinte depuis `/admin`, sans session ; le champ Identifiant a le focus. */
export async function openLogin(page: Page): Promise<void> {
  await page.goto('/admin', { waitUntil: 'networkidle' });
  await expect(page).toHaveURL('/admin/login');
  await expect(page.getByLabel('Identifiant')).toBeFocused();
}

/** Saisie au clavier, validée par `Entrée`. */
export async function signIn(page: Page, login: string, password: string): Promise<void> {
  await page.getByLabel('Identifiant').fill(login);
  await page.getByLabel('Mot de passe').fill(password);
  await page.getByLabel('Mot de passe').press('Enter');
}

/** Connexion complète jusqu'au tableau de bord. */
export async function signInToDashboard(page: Page): Promise<void> {
  await openLogin(page);
  await signInFromItsOwnAddress(page);
  await signIn(page, ADMIN_LOGIN, ADMIN_PASSWORD);
  await expect(page).toHaveURL('/admin');
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Tableau de bord');
}

/** Sous 64 rem, la navigation et la session sont repliées derrière « Menu ». */
export async function unfoldMenuIfFolded(page: Page): Promise<void> {
  const toggle = page.getByRole('button', { name: 'Menu' });
  if (await toggle.isVisible()) {
    await toggle.click();
  }
}
