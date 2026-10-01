import { test as withoutGuard } from '@playwright/test';

import { expect, expectAccessible, test } from './support/fixtures';

// Backend de développement (profil `dev`) : publications et projets d'amorçage.
const API = '/api/public/search';

test.describe('search', () => {
  test('renders accented results in the server html, without calling the api again', async ({
    page,
    request,
  }) => {
    const api = await (
      await request.get(`${API}?q=${encodeURIComponent('démonstrations')}`)
    ).json();
    expect(api.totalElements).toBeGreaterThan(0);
    const html = await (
      await request.get(`/search?q=${encodeURIComponent('démonstrations')}`)
    ).text();
    expect(html).toContain('<meta name="robots" content="noindex">');

    const calls: string[] = [];
    page.on('request', (r) => r.url().includes('/api/public/') && calls.push(r.url()));
    await page.goto(`/search?q=${encodeURIComponent('démonstrations')}`, {
      waitUntil: 'networkidle',
    });

    expect(calls).toEqual([]);
    await expect(page.getByRole('status')).toHaveText(
      new RegExp(`^${api.totalElements} résultats? pour «\\u00a0démonstrations\\u00a0»$`),
    );
    await expect(page.getByRole('searchbox')).toHaveValue('démonstrations');
    const first = api.content[0];
    const path =
      first.type === 'PROJECT'
        ? `/projects/${first.slug}`
        : `/${first.type === 'NEWS' ? 'news' : 'articles'}/${first.slug}`;
    await expect(page.getByRole('heading', { level: 2 }).first().getByRole('link')).toHaveAttribute(
      'href',
      path,
    );
    await expectAccessible(page);
  });

  test('searches from the form, then opens a result', async ({ page }) => {
    await page.goto('/search', { waitUntil: 'networkidle' });
    const calls: string[] = [];
    page.on('request', (r) => r.url().includes(API) && calls.push(r.url()));

    await page.getByRole('searchbox').fill('spring');
    await page.keyboard.press('Enter');

    await expect(page).toHaveURL('/search?q=spring');
    await expect(page.getByRole('status')).toHaveText(/résultats? pour «\u00a0spring\u00a0»/);
    expect(calls).toHaveLength(1);
    const result = page.getByRole('heading', { level: 2 }).first().getByRole('link');
    const title = await result.textContent();
    await result.click();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(title ?? '');
  });

  test('says when nothing matches', async ({ page }) => {
    await page.goto('/search?q=zzzzqx');

    await expect(page.getByRole('status')).toHaveText('Aucun résultat pour « zzzzqx »');
    await expect(page.getByRole('link', { name: 'Voir les articles' })).toBeVisible();
  });

  test('asks nothing for an empty query', async ({ page }) => {
    const calls: string[] = [];
    page.on('request', (r) => r.url().includes(API) && calls.push(r.url()));

    const response = await page.goto('/search?q=', { waitUntil: 'networkidle' });

    expect(response?.status()).toBe(200);
    expect(calls).toEqual([]);
    await expect(page.getByRole('status')).toHaveText('');
    await expectAccessible(page);
  });

  test('refuses more than 200 characters', async ({ page, guard }) => {
    guard.allowHttpError('/search');
    const calls: string[] = [];
    page.on('request', (r) => r.url().includes(API) && calls.push(r.url()));

    const response = await page.goto(`/search?q=${'a'.repeat(201)}`, { waitUntil: 'networkidle' });

    expect(response?.status()).toBe(400);
    expect(calls).toEqual([]);
    const field = page.getByRole('searchbox');
    await expect(field).toHaveAttribute('aria-invalid', 'true');
    await expect(page.getByText('La recherche est limitée à 200 caractères.')).toBeVisible();

    // Le champ lui-même n'accepte pas un 201ᵉ caractère
    await field.fill('');
    await field.pressSequentially('b'.repeat(201));
    await expect(field).toHaveValue('b'.repeat(200));
  });
});

// Sans JavaScript, le navigateur bloque les scripts de la page : le garde des erreurs réseau
// les compterait, d'où le test de base de Playwright.
withoutGuard.describe('search without javascript', () => {
  withoutGuard.use({ javaScriptEnabled: false });

  withoutGuard('submits the native form and gets the results from the server', async ({ page }) => {
    await page.goto('/search');

    await page.getByRole('searchbox').fill('spring');
    await page.getByRole('button', { name: 'Rechercher' }).click();

    await expect(page).toHaveURL('/search?q=spring');
    await expect(page.getByRole('status')).toHaveText(/résultats? pour «\u00a0spring\u00a0»/);
  });
});
