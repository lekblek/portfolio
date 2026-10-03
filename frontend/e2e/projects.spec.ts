import { Page } from '@playwright/test';

import { expect, expectAccessible, test } from './support/fixtures';
import { isDataCall } from './support/api-calls';

// Rendu serveur et détail : backend de développement (profil `dev`). Pagination au-delà des
// données d'amorçage : réponses simulées dans le navigateur, après une navigation client.
const LIST_API = '/api/public/projects';

interface Summary {
  title: string;
  slug: string;
  technologies: { name: string; slug: string }[];
}

async function openMenu(page: Page): Promise<void> {
  const toggle = page.getByRole('button', { name: 'Menu' });
  if (await toggle.isVisible()) {
    await toggle.click();
  }
}

function fakePage(pageNumber: number, totalPages: number) {
  const content = Array.from({ length: 10 }, (_, index) => ({
    title: `Projet simulé ${pageNumber * 10 + index + 1}`,
    slug: `projet-simule-${pageNumber * 10 + index + 1}`,
    shortDescription: 'Projet simulé pour la pagination.',
    stage: 'COMPLETED',
    startDate: '2024-01-01',
    endDate: '2024-06-30',
    featured: false,
    cover: null,
    technologies: [{ name: 'Java', slug: 'java' }],
  }));
  return {
    content,
    page: pageNumber,
    size: 10,
    totalElements: totalPages * 10,
    totalPages,
    first: pageNumber === 0,
    last: pageNumber === totalPages - 1,
  };
}

test.describe('projects', () => {
  test('lists the projects of the api in the server html, without calling it again', async ({
    page,
    request,
  }) => {
    const api = await (await request.get(LIST_API)).json();
    const html = await (await request.get('/projects')).text();
    for (const project of api.content as Summary[]) {
      expect(html).toContain(`href="/projects/${project.slug}"`);
    }

    const calls: string[] = [];
    page.on('request', (r) => isDataCall(r.url()) && calls.push(r.url()));
    await page.goto('/projects', { waitUntil: 'networkidle' });

    expect(calls).toEqual([]);
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Projets');
    await expect(page.getByRole('heading', { level: 2 })).toHaveCount(api.content.length);
    await openMenu(page);
    await expect(
      page.getByRole('navigation', { name: 'Navigation principale' }).getByRole('link', {
        name: 'Projets',
      }),
    ).toHaveAttribute('aria-current', 'page');
    await expectAccessible(page);
  });

  test('filters by a technology of a project, then removes the filter', async ({
    page,
    request,
  }) => {
    const api = await (await request.get(LIST_API)).json();
    const technology = (api.content as Summary[])[0].technologies[0];
    const filtered = await (await request.get(`${LIST_API}?technology=${technology.slug}`)).json();
    await page.goto('/projects', { waitUntil: 'networkidle' });

    await page
      .getByRole('list', { name: `Technologies de ${api.content[0].title}` })
      .getByRole('link', { name: technology.name })
      .click();

    await expect(page).toHaveURL(`/projects?technology=${technology.slug}`);
    await expect(page.getByText(`Technologie : ${technology.name}`)).toBeVisible();
    await expect(page.getByRole('heading', { level: 2 })).toHaveCount(filtered.content.length);
    await expectAccessible(page);

    await page.getByRole('link', { name: 'Retirer le filtre' }).click();

    await expect(page).toHaveURL('/projects');
    await expect(page.getByRole('heading', { level: 2 })).toHaveCount(api.content.length);
  });

  test('keeps the filter across pages, from the keyboard', async ({ page }) => {
    await page.goto('/projects', { waitUntil: 'networkidle' });
    await page.route(`**${LIST_API}?**`, (route) => {
      const url = new URL(route.request().url());
      expect(url.searchParams.get('technology')).toBe('java');
      return route.fulfill({ json: fakePage(Number(url.searchParams.get('page')), 3) });
    });

    await page.getByRole('link', { name: 'Java' }).first().click();
    await expect(page).toHaveURL('/projects?technology=java');
    const pagination = page.getByRole('navigation', { name: 'Pagination' });
    await expect(pagination.getByRole('link', { name: 'Page 1' })).toHaveAttribute(
      'aria-current',
      'page',
    );

    await pagination.getByRole('link', { name: 'Page 2' }).focus();
    await page.keyboard.press('Enter');

    await expect(page).toHaveURL('/projects?technology=java&page=2');
    await expect(page.getByRole('heading', { name: 'Projet simulé 11' })).toBeVisible();
    await expect(pagination.getByRole('link', { name: 'Page 2' })).toHaveAttribute(
      'aria-current',
      'page',
    );
    await expect(pagination.getByRole('link', { name: 'Précédente' })).toBeVisible();
    await expect(pagination.getByRole('link', { name: 'Suivante' })).toBeVisible();
    await expectAccessible(page);

    await pagination.getByRole('link', { name: 'Page 3' }).click();
    await expect(page).toHaveURL('/projects?technology=java&page=3');
    await expect(pagination.getByRole('link', { name: 'Suivante' })).toHaveCount(0);
  });

  test('answers a page past the end with a real 404', async ({ page, guard }) => {
    guard.allowHttpError('/projects');

    const response = await page.goto('/projects?page=99');

    expect(response?.status()).toBe(404);
    await expect(page.getByText('Cette page n’existe pas')).toBeVisible();
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute('content', 'noindex');
  });

  test('shows a project from the server html, without calling the api again', async ({
    page,
    request,
  }) => {
    const api = await (await request.get(LIST_API)).json();
    const slug = (api.content as Summary[])[0].slug;
    const project = await (await request.get(`${LIST_API}/${slug}`)).json();
    const html = await (await request.get(`/projects/${slug}`)).text();
    expect(html).toContain(`>${project.title}</h1>`);
    expect(html).toContain('"@type":"CreativeWork"');

    const calls: string[] = [];
    page.on('request', (r) => isDataCall(r.url()) && calls.push(r.url()));
    await page.goto(`/projects/${slug}`, { waitUntil: 'networkidle' });

    expect(calls).toEqual([]);
    await expect(page).toHaveTitle(`${project.title} — Blek Ngossanga`);
    await expect(page.getByRole('complementary', { name: 'Fiche du projet' })).toBeVisible();
    await expect(page.getByRole('link', { name: /Voir le code source/ })).toHaveCount(
      project.repositoryUrl ? 1 : 0,
    );
    await expect(page.getByRole('link', { name: /Voir la démonstration/ })).toHaveCount(
      project.demoUrl ? 1 : 0,
    );
    await expectAccessible(page);
  });

  test('answers an unknown project with a real 404', async ({ page, guard }) => {
    guard.allowHttpError('/projects/projet-inexistant');
    guard.allowHttpError(`${LIST_API}/projet-inexistant`);

    const response = await page.goto('/projects/projet-inexistant');

    expect(response?.status()).toBe(404);
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Projet introuvable');
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute('content', 'noindex');
    await expectAccessible(page);
  });
});
