import { expect, expectAccessible, test } from './support/fixtures';

// Backend de développement (profil `dev`) : profil, projets mis en avant, publications et série.
test.describe('home', () => {
  test('renders every zone in the server html, without calling the api again', async ({
    page,
    request,
  }) => {
    const profile = await (await request.get('/api/public/profile')).json();
    const featured = await (await request.get('/api/public/projects?featured=true&size=3')).json();
    const html = await (await request.get('/')).text();
    expect(html).toContain(`>${profile.displayName}</h1>`);
    for (const project of featured.content as { slug: string }[]) {
      expect(html).toContain(`href="/projects/${project.slug}"`);
    }
    for (const path of ['/projects', '/articles', '/series', '/news']) {
      expect(html).toContain(`href="${path}"`);
    }
    expect(html).not.toContain(profile.publicEmail ?? 'aucune adresse publique');

    const calls: string[] = [];
    page.on('request', (r) => r.url().includes('/api/public/') && calls.push(r.url()));
    await page.goto('/', { waitUntil: 'networkidle' });

    expect(calls).toEqual([]);
    await expect(page).toHaveTitle('Blek Ngossanga — Software Engineering, AI Vision & Research');
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(profile.displayName);
    await expect(page.getByRole('table', { name: 'En bref' })).toBeVisible();
    await expect(
      page.getByRole('heading', { level: 2, name: 'Projets mis en avant' }),
    ).toBeVisible();
    await expect(page.getByRole('heading', { level: 2, name: 'Derniers articles' })).toBeVisible();
    await expectAccessible(page);
  });

  test('leads home from the site name, then to a full list', async ({ page }) => {
    await page.goto('/about', { waitUntil: 'networkidle' });

    const home = page.getByRole('banner').getByRole('link', { name: 'Blek Ngossanga' });
    await home.click();

    await expect(page).toHaveURL('/');
    await expect(home).toHaveAttribute('aria-current', 'page');
    await page.getByRole('link', { name: /^Tous les articles/ }).click();
    await expect(page).toHaveURL('/articles');
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Articles');
  });
});
