import { Page } from '@playwright/test';

import {
  ADMIN_LOGIN,
  ADMIN_PASSWORD,
  NO_ADMIN_ACCOUNT,
  SESSION,
  signInToDashboard,
} from './support/admin';
import { expect, expectAccessible, test } from './support/fixtures';

// Projets (F27) : liste filtrée et paginée (jeu de démonstration du profil dev, D-EU), formulaire
// refusé tant qu'il est incomplet, puis cycle de vie réel d'un projet de recette : publié, visible
// sur /projects, archivé, retiré du site. L'API ne supprime pas un projet (D-CX) : le projet de
// recette est réutilisé d'une exécution à l'autre et reste archivé. Seul le projet desktop
// l'enregistre ; répéter ce test (`--repeat-each`) demande `--workers=1`.
const API = '/api/admin/projects';
const RECIPE = 'Projet de recette E2E';
const RECIPE_SLUG = 'projet-de-recette-e2e';

async function openProjects(page: Page, query = ''): Promise<void> {
  await signInToDashboard(page);
  await page.goto(`/admin/projects${query}`, { waitUntil: 'networkidle' });
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Projets');
}

test.describe('admin projects', () => {
  test.beforeEach(({ guard }) => {
    test.skip(!ADMIN_LOGIN || !ADMIN_PASSWORD, NO_ADMIN_ACCOUNT);
    guard.allowHttpError(SESSION);
  });

  test('pages through the projects and filters them, keeping the filters in the address', async ({
    page,
  }) => {
    await openProjects(page);
    const total = (await (await page.request.get(`${API}?size=1`)).json()).totalElements as number;
    test.skip(total <= 20, 'Moins de 21 projets : jeu de démonstration du profil dev absent');

    await expect(page.getByRole('status').filter({ hasText: 'projets' })).toHaveText(
      `${total} projets`,
    );
    await expect(page.locator('tbody tr')).toHaveCount(20);
    const pages = page.getByRole('navigation', { name: 'Pages des projets' });
    await expect(pages.getByRole('link', { name: 'Page 1' })).toHaveAttribute(
      'aria-current',
      'page',
    );
    await expectAccessible(page);

    await pages.getByRole('link', { name: 'Suivante' }).focus();
    await page.keyboard.press('Enter');
    await expect(page).toHaveURL('/admin/projects?page=2');
    await expect(pages.getByRole('link', { name: 'Page 2' })).toHaveAttribute(
      'aria-current',
      'page',
    );
    await expect(page.locator('tbody tr')).toHaveCount(total - 20);
    await expect(pages.getByRole('link', { name: 'Suivante' })).toHaveCount(0);

    // Filtrer depuis la page 2 ramène à la première page
    await page.getByLabel('Visibilité').selectOption('PUBLISHED');
    await page.getByLabel('Technologie').selectOption('java');
    await page.getByRole('button', { name: 'Filtrer' }).click();
    await expect(page).toHaveURL('/admin/projects?visibility=PUBLISHED&technology=java');
    await expect(page.getByRole('status').filter({ hasText: 'projets' })).toContainText(
      'correspondant aux filtres',
    );
    for (const badge of await page.locator('tbody app-status-badge').allInnerTexts()) {
      expect(badge.trim()).toBe('Publié');
    }
    await expectAccessible(page);

    await page.getByRole('link', { name: 'Effacer les filtres' }).click();
    await expect(page).toHaveURL('/admin/projects');
    await expect(page.getByLabel('Visibilité')).toHaveValue('');
  });

  test('refuses an incomplete project and asks a completed one for its end date', async ({
    page,
  }) => {
    let sent = false;
    await page.route(`**${API}`, (route) => {
      sent ||= route.request().method() === 'POST';
      return route.continue();
    });
    await openProjects(page);
    await page.getByRole('link', { name: 'Nouveau projet' }).click();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Nouveau projet');

    await page.getByLabel('Terminé').check();
    await page.getByRole('button', { name: 'Créer le projet' }).click();

    await expect(page.getByLabel('Titre')).toBeFocused();
    await expect(page.getByLabel('Titre')).toHaveAccessibleDescription('Indiquez le titre.');
    await expect(page.locator('#projet-fin')).toHaveAccessibleDescription(
      'Indiquez la date de fin d’un projet terminé.',
    );
    await page.getByLabel('En cours').check();
    await expect(page.locator('#projet-fin')).toHaveCount(0);
    expect(sent).toBe(false);
    expect(await page.evaluate(() => document.documentElement.scrollWidth - innerWidth)).toBe(0);
    await expectAccessible(page);
  });

  test('publishes the recipe project, shows it on the site, then archives it', async ({
    page,
    guard,
  }, info) => {
    test.skip(
      info.project.name !== 'desktop',
      'Projet de recette unique : un seul projet l’enregistre',
    );
    guard.allowHttpError(`/api/public/projects/${RECIPE_SLUG}`);
    guard.allowHttpError(`/projects/${RECIPE_SLUG}`);
    await openProjects(page);
    const all = await (await page.request.get(`${API}?size=100`)).json();
    const existing = all.content.find((project: { slug: string }) => project.slug === RECIPE_SLUG);

    if (existing) {
      await page.goto(`/admin/projects/${existing.id}`, { waitUntil: 'networkidle' });
      await expect(
        page.getByRole('button', { name: 'Enregistrer les modifications' }),
      ).toBeVisible();
    } else {
      await page.getByRole('link', { name: 'Nouveau projet' }).click();
      await page.getByLabel('Titre').fill(RECIPE);
      await page.getByLabel('Résumé').fill('Projet de recette des tests de bout en bout.');
      await page.getByLabel('Début').fill('2026-01-05');
    }

    await page.getByLabel('Publié').check();
    // Technologies au clavier : saisir, parcourir, cocher
    const technologies = page.getByRole('combobox', { name: 'Technologies' });
    await technologies.fill('');
    await technologies.pressSequentially('pyth', { delay: 30 });
    await expect(page.getByRole('option', { name: 'Python' })).toBeVisible();
    const python = page.getByRole('option', { name: 'Python' });
    if ((await python.getAttribute('aria-selected')) !== 'true') {
      await python.click();
    }
    await expect(python).toHaveAttribute('aria-selected', 'true');
    await page.keyboard.press('Escape');
    await expect(page.getByRole('list', { name: 'Technologies choisies' })).toContainText('Python');
    await expectAccessible(page);

    await page
      .getByRole('button', { name: /Créer le projet|Enregistrer les modifications/ })
      .click();
    await expect(page.locator('app-toast-region')).toContainText(`Projet «\u00a0${RECIPE}\u00a0»`);
    await expect(page).toHaveURL('/admin/projects');

    await page.goto(`/projects/${RECIPE_SLUG}`, { waitUntil: 'networkidle' });
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(RECIPE);

    // Archivage : le projet quitte le site, son slug figé est expliqué
    const saved = await (
      await page.request.get(`${API}?visibility=PUBLISHED&technology=python`)
    ).json();
    const recipe = saved.content.find((project: { slug: string }) => project.slug === RECIPE_SLUG);
    await page.goto(`/admin/projects/${recipe.id}`, { waitUntil: 'networkidle' });
    await expect(page.getByLabel('Slug')).toHaveAttribute('readonly', '');
    await expect(page.getByLabel('Slug')).toHaveAccessibleDescription(/Figé/);
    await page.getByLabel('Archivé').check();
    await page.getByRole('button', { name: 'Enregistrer les modifications' }).click();
    await expect(page.locator('app-toast-region')).toContainText('enregistré');

    const response = await page.goto(`/projects/${RECIPE_SLUG}`);
    expect(response?.status()).toBe(404);
  });
});
