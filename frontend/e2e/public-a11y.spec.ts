import { APIRequestContext, Page } from '@playwright/test';

import { expect, expectAccessible, test } from './support/fixtures';

// Audit des pages publiques (F21) : chaque route et chacun de ses états, avec le backend de
// développement (profil `dev`). Statut HTTP du rendu serveur, axe, un seul `<h1>`, repères, et
// aucun défilement horizontal de 320 à 1920 px.
const WIDTHS = [320, 360, 768, 1024, 1280, 1920];

interface Route {
  name: string;
  path: string;
  status: number;
}

async function firstSlug(request: APIRequestContext, path: string): Promise<string> {
  const page = await (await request.get(`/api/public/${path}`)).json();
  expect(page.content.length, `contenu d'amorçage pour ${path}`).toBeGreaterThan(0);
  return page.content[0].slug;
}

async function routes(request: APIRequestContext): Promise<Route[]> {
  const [project, article, news, series] = await Promise.all([
    firstSlug(request, 'projects'),
    firstSlug(request, 'publications?type=ARTICLE'),
    firstSlug(request, 'publications?type=NEWS'),
    firstSlug(request, 'series'),
  ]);
  return [
    { name: 'accueil', path: '/', status: 200 },
    { name: 'projets', path: '/projects', status: 200 },
    { name: 'projets filtrés sans résultat', path: '/projects?technology=inconnue', status: 200 },
    { name: 'projets au-delà de la dernière page', path: '/projects?page=99', status: 404 },
    { name: 'projet', path: `/projects/${project}`, status: 200 },
    { name: 'projet inconnu', path: '/projects/projet-inconnu', status: 404 },
    { name: 'articles', path: '/articles', status: 200 },
    { name: 'article', path: `/articles/${article}`, status: 200 },
    { name: 'article inconnu', path: '/articles/article-inconnu', status: 404 },
    { name: 'actualités', path: '/news', status: 200 },
    { name: 'actualité', path: `/news/${news}`, status: 200 },
    { name: 'séries', path: '/series', status: 200 },
    { name: 'série', path: `/series/${series}`, status: 200 },
    { name: 'série inconnue', path: '/series/serie-inconnue', status: 404 },
    { name: 'à propos', path: '/about', status: 200 },
    { name: 'contact', path: '/contact', status: 200 },
    { name: 'recherche vide', path: '/search', status: 200 },
    { name: 'recherche', path: `/search?q=${encodeURIComponent('démonstration')}`, status: 200 },
    { name: 'recherche sans résultat', path: '/search?q=zzzzzz', status: 200 },
    { name: 'recherche trop longue', path: `/search?q=${'a'.repeat(201)}`, status: 400 },
    { name: 'adresse inconnue', path: '/nimporte-quoi', status: 404 },
  ];
}

async function expectSoundStructure(page: Page): Promise<void> {
  await expect(page.getByRole('heading', { level: 1 })).toHaveCount(1);
  await expect(page.getByRole('banner')).toHaveCount(1);
  await expect(page.getByRole('main')).toHaveCount(1);
  await expect(page.getByRole('contentinfo')).toHaveCount(1);
  // Repliée sous 64 rem (hors de l'arbre d'accessibilité tant que le menu est fermé)
  await expect(page.locator('nav#navigation-principale')).toBeAttached();
  await expectAccessible(page);
}

async function horizontalOverflows(page: Page): Promise<string[]> {
  const overflows: string[] = [];
  for (const width of WIDTHS) {
    await page.setViewportSize({ width, height: 900 });
    const extra = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    if (extra > 0) {
      overflows.push(`${width} px : ${extra} px de trop`);
    }
  }
  return overflows;
}

test.describe('public pages', () => {
  test('meet WCAG 2.2 AA in every state, from 320 to 1920 px', async ({
    page,
    request,
    guard,
  }, info) => {
    test.slow();
    // Contenu inconnu : une erreur n'est pas transférée, le navigateur repose la question (D-EB)
    guard.allowHttpError(/\/api\/public\/(projects|publications|series)\/[a-z]+-inconnue?$/);
    for (const route of await routes(request)) {
      await test.step(route.name, async () => {
        if (route.status >= 400) {
          guard.allowHttpError(new RegExp(route.path.replace(/[?.*+^$()[\]{}|\\]/g, '\\$&')));
        }
        const response = await page.goto(route.path, { waitUntil: 'networkidle' });

        expect(response?.status(), route.path).toBe(route.status);
        await expectSoundStructure(page);
        // Les largeurs sont parcourues une fois, par le projet bureau
        if (info.project.name === 'desktop') {
          expect(await horizontalOverflows(page), route.path).toEqual([]);
          await page.setViewportSize({ width: 1440, height: 900 });
        }
      });
    }
  });

  test('stay accessible when the api fails during a navigation', async ({ page, guard }) => {
    guard.allowHttpError('/nimporte-quoi');
    guard.allowHttpError(/\/api\/public\//);
    await page.goto('/nimporte-quoi', { waitUntil: 'networkidle' });
    await page.route('**/api/public/**', (route) =>
      route.fulfill({
        status: 500,
        contentType: 'application/problem+json',
        body: JSON.stringify({ status: 500, code: 'INTERNAL_ERROR' }),
      }),
    );

    const footer = page.getByRole('contentinfo');
    for (const link of ['Projets', 'Articles', 'À propos']) {
      await test.step(link, async () => {
        await footer.getByRole('link', { name: link }).click();
        await expect(page.getByRole('alert')).toBeVisible();
        await expectSoundStructure(page);
      });
    }
    await test.step('accueil', async () => {
      await page.getByRole('banner').getByRole('link', { name: 'Blek Ngossanga' }).click();
      await expect(page.getByRole('alert').first()).toBeVisible();
      await expectSoundStructure(page);
    });
  });
});
