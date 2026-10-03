import {
  ADMIN_LOGIN,
  ADMIN_PASSWORD,
  NO_ADMIN_ACCOUNT,
  SESSION,
  signInToDashboard,
} from './support/admin';
import { expect, expectAccessible, test } from './support/fixtures';

// Séries (F29) : une série de recette reçoit deux articles publiés libres (dans aucune série),
// réordonnés au clavier, vérifiés sur le site, puis ses chapitres sont retirés (les articles
// redeviennent libres, la série disparaît du site). Pas de suppression dans l'API (D-CV) : la série
// de recette est réutilisée. Seul le projet desktop l'enregistre.
const API = '/api/admin/series';
const RECIPE = 'Série de recette E2E';
const RECIPE_SLUG = 'serie-de-recette-e2e';

test.describe('admin series', () => {
  test.beforeEach(({ guard }) => {
    test.skip(!ADMIN_LOGIN || !ADMIN_PASSWORD, NO_ADMIN_ACCOUNT);
    guard.allowHttpError(SESSION);
  });

  test('lists the series', async ({ page }) => {
    await signInToDashboard(page);
    await page.goto('/admin/series', { waitUntil: 'networkidle' });
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Séries');
    await expect(page.locator('tbody tr').first()).toBeVisible();
    await expectAccessible(page);
  });

  test('orders two articles in the recipe series, shows them on the site, then frees them', async ({
    page,
    request,
    guard,
  }, info) => {
    test.skip(
      info.project.name !== 'desktop',
      'Série de recette unique : un seul projet l’enregistre',
    );
    guard.allowHttpError(`/api/public/series/${RECIPE_SLUG}`);
    guard.allowHttpError(`/series/${RECIPE_SLUG}`);

    // Deux articles publiés qui n'appartiennent à aucune série visible
    const series = await (await request.get('/api/public/series?size=100')).json();
    const filed = new Set<string>();
    for (const item of series.content) {
      const detail = await (await request.get(`/api/public/series/${item.slug}`)).json();
      for (const chapter of detail.chapters) {
        filed.add(chapter.slug);
      }
    }
    const articles = await (
      await request.get('/api/public/publications?type=ARTICLE&size=100')
    ).json();
    const free = articles.content.filter((article: { slug: string }) => !filed.has(article.slug));
    test.skip(free.length < 2, 'Moins de deux articles publiés hors série');
    const [first, second] = free;

    await signInToDashboard(page);
    const all = await (await page.request.get(`${API}?size=100`)).json();
    const existing = all.content.find((item: { slug: string }) => item.slug === RECIPE_SLUG);
    if (existing) {
      await page.goto(`/admin/series/${existing.id}`, { waitUntil: 'networkidle' });
    } else {
      await page.goto('/admin/series/new', { waitUntil: 'networkidle' });
      await page.getByLabel('Titre').fill(RECIPE);
      await page.getByRole('button', { name: 'Créer la série' }).click();
      await expect(page.locator('app-toast-region')).toContainText('ajoutez ses chapitres');
    }
    await expect(page.getByRole('heading', { level: 2, name: 'Chapitres' })).toBeVisible();

    const add = page.getByLabel('Ajouter un article');
    for (const article of [first, second]) {
      await add.selectOption({ label: article.title });
      await page.getByRole('button', { name: 'Ajouter', exact: true }).click();
    }
    // Le second article passe en premier, au clavier
    const up = page.getByRole('button', { name: `Monter le chapitre « ${second.title} »` });
    await up.focus();
    await page.keyboard.press('Enter');
    await expect(page.locator('app-sortable-item').first()).toContainText(second.title);
    await expectAccessible(page);
    await page.getByRole('button', { name: 'Enregistrer les chapitres' }).click();
    await expect(page.locator('app-toast-region')).toContainText('2 chapitres');

    await page.goto(`/series/${RECIPE_SLUG}`, { waitUntil: 'networkidle' });
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(RECIPE);
    const chapters = page.locator('main ol li');
    await expect(chapters.first()).toContainText(second.title);
    await expect(chapters.nth(1)).toContainText(first.title);

    // Les articles redeviennent libres : la série quitte le site
    await page.goBack({ waitUntil: 'networkidle' });
    for (const title of [second.title, first.title]) {
      await page.getByRole('button', { name: `Retirer le chapitre « ${title} »` }).click();
    }
    await page.getByRole('button', { name: 'Enregistrer les chapitres' }).click();
    await expect(page.locator('app-toast-region')).toContainText('0 chapitre');
  });
});
