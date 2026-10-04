import { Page } from '@playwright/test';

import {
  ADMIN_LOGIN,
  ADMIN_PASSWORD,
  NO_ADMIN_ACCOUNT,
  SESSION,
  signInToDashboard,
} from './support/admin';
import { expect, expectAccessible, test } from './support/fixtures';

// Éditeur hybride (F31, ADR 0004) : un article de recette rédigé en mode visuel, vérifié en
// Markdown, enrichi d'une image de la médiathèque, d'un bloc de code, d'une formule et d'un
// diagramme, prévisualisé, enregistré, publié, lu sur le site, puis repris et archivé. L'API ne
// supprime pas une publication (D-CU) : l'article est réutilisé ; seul le projet desktop l'écrit.
const API = '/api/admin/publications';
const RECIPE = 'Article éditeur E2E';
const RECIPE_SLUG = 'article-editeur-e2e';

async function openRecipe(page: Page): Promise<void> {
  await signInToDashboard(page);
  const all = await (await page.request.get(`${API}?type=ARTICLE&size=100`)).json();
  const existing = all.content.find((item: { slug: string }) => item.slug === RECIPE_SLUG);
  if (existing) {
    await page.goto(`/admin/publications/${existing.id}`, { waitUntil: 'networkidle' });
    return;
  }
  await page.goto('/admin/publications/new/article', { waitUntil: 'networkidle' });
  await page.getByLabel('Titre', { exact: true }).fill(RECIPE);
  await page.getByLabel('Résumé').fill('Article de recette de l’éditeur hybride.');
  await page.getByRole('button', { name: 'Créer le brouillon' }).click();
  await expect(page.locator('app-toast-region')).toContainText('Brouillon');
}

test.describe('admin editor', () => {
  test.beforeEach(({ guard }, info) => {
    test.skip(!ADMIN_LOGIN || !ADMIN_PASSWORD, NO_ADMIN_ACCOUNT);
    test.skip(info.project.name !== 'desktop', 'Article de recette unique : desktop seulement');
    guard.allowHttpError(SESSION);
  });

  test('writes visually, checks the markdown, inserts media and blocks, publishes and reads it on the site', async ({
    page,
    guard,
  }) => {
    guard.allowHttpError(`/api/public/publications/${RECIPE_SLUG}`);
    guard.allowHttpError(`/articles/${RECIPE_SLUG}`);
    // Le sélecteur affiche les vignettes de la médiathèque : la spec des médias, en parallèle, peut
    // supprimer son fichier envoyé entre la liste et la vignette (404 sur ce seul fichier)
    guard.allowHttpError(/\/api\/public\/media\/[0-9a-f]{32}\.png$/);
    await openRecipe(page);

    // Rédaction visuelle : le contenu entier est remplacé
    const surface = page.getByRole('textbox', { name: 'Contenu, édition visuelle' });
    await expect(surface).toBeVisible();
    await surface.click();
    await page.keyboard.press('ControlOrMeta+A');
    await page.keyboard.type('Une introduction écrite en mode visuel.');
    await page.keyboard.press('Enter');
    await page.keyboard.type('Mesures');
    const toolbar = page.getByRole('toolbar', { name: 'Mise en forme' });
    await toolbar.getByRole('button', { name: 'Titre', exact: true }).click();
    await page.keyboard.press('End');
    await page.keyboard.press('Enter');
    await page.keyboard.type('Un résultat ');
    await toolbar.getByRole('button', { name: 'Gras' }).click();
    await page.keyboard.type('important');
    await expectAccessible(page);

    // Bloc de code, formule, diagramme
    await page.keyboard.press('End');
    await page.keyboard.press('Enter');
    await toolbar.getByRole('button', { name: 'Bloc de code' }).click();
    const codeDialog = page.getByRole('dialog', { name: 'Insérer un bloc de code' });
    await codeDialog.getByLabel('Langage').selectOption('java');
    await codeDialog.getByRole('button', { name: 'Insérer' }).click();
    await toolbar.getByRole('button', { name: 'Formule' }).click();
    await toolbar.getByRole('button', { name: 'Diagramme' }).click();

    // Markdown : la source canonique
    await page.getByRole('tab', { name: 'Markdown' }).click();
    const source = page.getByRole('textbox', { name: 'Contenu (Markdown)' });
    await expect(source).toHaveValue(/Une introduction écrite en mode visuel\./);
    await expect(source).toHaveValue(/^## Mesures$/m);
    await expect(source).toHaveValue(/\*\*important\*\*/);
    await expect(source).toHaveValue(/```java/);
    await expect(source).toHaveValue(/\$\$\nE = mc\^2\n\$\$/);
    await expect(source).toHaveValue(/```mermaid\nflowchart LR\n {2}accTitle:/);

    // Image de la médiathèque, avec une légende : une figure sur le site
    await source.click();
    await page.keyboard.press('ControlOrMeta+End');
    await page
      .getByRole('toolbar', { name: 'Insertion' })
      .getByRole('button', { name: 'Image' })
      .click();
    const picker = page.getByRole('dialog', { name: 'Insérer une image' });
    await picker
      .getByRole('button', { name: /^Choisir/ })
      .first()
      .click();
    const describe = page.getByRole('dialog', { name: 'Décrire l’image' });
    await describe.getByLabel('Texte alternatif').fill('Capture de recette');
    await describe.getByRole('textbox', { name: 'Légende (facultatif)' }).fill('Figure de recette');
    await expectAccessible(page);
    await describe.getByRole('button', { name: 'Insérer' }).click();
    await expect(source).toHaveValue(
      /!\[Capture de recette\]\(\/api\/public\/media\/\S+ "Figure de recette"\)/,
    );
    await expect(source).toBeFocused();

    // Aperçu : le moteur du site
    await page.getByRole('tab', { name: 'Aperçu' }).click();
    const preview = page.locator('app-markdown-view');
    await expect(preview.getByRole('heading', { name: 'Mesures' })).toBeVisible();
    await expect(preview.locator('figcaption')).toContainText('Figure de recette');
    await expect(preview.locator('strong')).toHaveText('important');
    await expectAccessible(page);

    // Garde des modifications non enregistrées
    await page.getByRole('link', { name: 'Retour à la liste' }).click();
    const leave = page.getByRole('dialog', { name: /^Quitter sans enregistrer/ });
    await expect(leave).toBeVisible();
    await leave.getByRole('button', { name: 'Rester sur la page' }).click();

    // Enregistrer, publier
    await page.getByRole('button', { name: 'Enregistrer les modifications' }).click();
    await expect(page.locator('app-toast-region')).toContainText('enregistré');
    const panel = page.locator('app-publication-status-panel');
    await panel.getByRole('button', { name: /^(Publier maintenant|Republier)$/ }).click();
    await expect(panel.locator('app-status-badge')).toHaveText('Publiée');
    const editUrl = page.url();

    // Le site rend le même Markdown
    await page.goto(`/articles/${RECIPE_SLUG}`, { waitUntil: 'networkidle' });
    const article = page.locator('article');
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(RECIPE);
    await expect(article.getByRole('heading', { name: 'Mesures' })).toBeVisible();
    await expect(article.locator('strong', { hasText: 'important' })).toBeVisible();
    await expect(article.locator('figcaption')).toContainText('Figure de recette');
    await expect(article.locator('.math-display')).toHaveCount(1);
    await expect(article.getByText('java', { exact: true })).toBeVisible();

    // Reprise : le contenu revient dans l'éditeur, puis l'article quitte le site
    await page.goto(editUrl, { waitUntil: 'networkidle' });
    await expect(page.getByRole('textbox', { name: 'Contenu, édition visuelle' })).toContainText(
      'Une introduction écrite en mode visuel.',
    );
    await panel.getByRole('button', { name: 'Archiver' }).click();
    await page
      .getByRole('dialog', { name: /^Archiver/ })
      .getByRole('button', { name: 'Archiver' })
      .click();
    await expect(panel.locator('app-status-badge')).toHaveText('Archivée');
  });
});
