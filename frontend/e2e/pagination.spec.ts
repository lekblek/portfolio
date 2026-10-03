import { expect, expectAccessible, test } from './support/fixtures';

// Pagination réelle des listes publiques (F27, D-EU) : le jeu de démonstration du profil dev
// franchit deux pages de 10 ; sans lui, ces tests sont ignorés (les cas limites sont couverts par
// des réponses simulées dans projects.spec.ts et publications.spec.ts).
const LISTS = [
  { path: '/projects', api: '/api/public/projects', label: 'projets' },
  { path: '/articles', api: '/api/public/publications?type=ARTICLE', label: 'articles' },
  { path: '/news', api: '/api/public/publications?type=NEWS', label: 'actualités' },
];

test.describe('real pagination', () => {
  for (const list of LISTS) {
    test(`pages through the ${list.label}, first to last, then past the end`, async ({
      page,
      request,
      guard,
    }) => {
      const first = await (await request.get(list.api)).json();
      test.skip(
        first.totalPages < 2,
        `Une seule page de ${list.label} : jeu de démonstration absent`,
      );
      const last = first.totalPages;

      await page.goto(list.path, { waitUntil: 'networkidle' });
      // Première action du visiteur : la mesure de l'image LCP s'arrête (les images révélées en
      // faisant défiler la page ne sont plus candidates)
      await page.keyboard.press('Shift');
      const pages = page.getByRole('navigation', { name: 'Pagination' });
      await expect(pages.getByRole('link', { name: 'Page 1' })).toHaveAttribute(
        'aria-current',
        'page',
      );
      await expect(pages.getByRole('link', { name: 'Précédente' })).toHaveCount(0);
      await expect(page.locator('main article, main li h2, main li h3').first()).toBeVisible();

      await pages.getByRole('link', { name: 'Suivante' }).click();
      await expect(page).toHaveURL(`${list.path}?page=2`);
      await expect(pages.getByRole('link', { name: 'Page 2' })).toHaveAttribute(
        'aria-current',
        'page',
      );
      if (last === 2) {
        await expect(pages.getByRole('link', { name: 'Suivante' })).toHaveCount(0);
      }
      await expectAccessible(page);

      await pages.getByRole('link', { name: 'Précédente' }).click();
      await expect(page).toHaveURL(list.path);

      guard.allowHttpError(list.api.split('?')[0]);
      guard.allowHttpError(list.path);
      const response = await page.goto(`${list.path}?page=${last + 1}`);
      expect(response?.status()).toBe(404);
    });
  }

  test('a technology filter starts again from the first page', async ({ page, request }) => {
    const second = await (await request.get('/api/public/projects?page=1')).json();
    test.skip(
      second.content.length === 0,
      'Une seule page de projets : jeu de démonstration absent',
    );
    const project = second.content[0];
    const technology = project.technologies[0];

    await page.goto('/projects?page=2', { waitUntil: 'networkidle' });
    await page
      .getByRole('list', { name: `Technologies de ${project.title}` })
      .getByRole('link', { name: technology.name })
      .click();
    await expect(page).toHaveURL(`/projects?technology=${technology.slug}`);
  });
});
