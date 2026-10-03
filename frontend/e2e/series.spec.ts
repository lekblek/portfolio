import { expect, expectAccessible, test } from './support/fixtures';
import { isDataCall } from './support/api-calls';

// Série du profil `dev` (SeriesSeeder) : au moins deux chapitres visibles.
const API = '/api/public/series';

interface Series {
  title: string;
  slug: string;
  chapters: { position: number; title: string; slug: string }[];
}

async function firstSeries(request: import('@playwright/test').APIRequestContext): Promise<Series> {
  const [summary] = (await (await request.get(API)).json()).content;
  return (await request.get(`${API}/${summary.slug}`)).json();
}

test.describe('series', () => {
  test('lists the series and their chapters in the server html, without calling the api again', async ({
    page,
    request,
  }) => {
    const series = await firstSeries(request);
    const html = await (await request.get(`/series/${series.slug}`)).text();
    for (const chapter of series.chapters) {
      expect(html).toContain(`href="/articles/${chapter.slug}"`);
    }

    const calls: string[] = [];
    page.on('request', (r) => isDataCall(r.url()) && calls.push(r.url()));
    await page.goto(`/series/${series.slug}`, { waitUntil: 'networkidle' });

    expect(calls).toEqual([]);
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(series.title);
    await expectAccessible(page);

    await page.goto('/series', { waitUntil: 'networkidle' });
    expect(calls).toEqual([]);
    await expect(page.getByRole('link', { name: series.title })).toBeVisible();
    await expectAccessible(page);
  });

  test('walks through a series from chapter to chapter with the keyboard', async ({
    page,
    request,
  }) => {
    const series = await firstSeries(request);
    test.skip(series.chapters.length < 2, 'série dev de moins de deux chapitres');
    const [first, second] = series.chapters;
    await page.goto(`/articles/${first.slug}`);

    const block = page.getByRole('navigation', { name: `Série «\u00a0${series.title}\u00a0»` });
    await expect(block.locator('a[rel="prev"]')).toHaveCount(0);
    const next = block.locator('a[rel="next"]');
    await expect(next).toHaveText(second.title);
    await next.focus();
    await page.keyboard.press('Enter');

    await expect(page).toHaveURL(`/articles/${second.slug}`);
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(second.title);
    await expect(block.locator('a[rel="prev"]')).toHaveText(first.title);
    await expect(page.getByText(`Chapitre ${second.position} sur`).first()).toBeVisible();
    await expectAccessible(page);
  });

  test('does not ask again whether an article outside any series belongs to one', async ({
    page,
  }) => {
    const calls: string[] = [];
    page.on(
      'request',
      (r) => r.url().includes('/series') && r.url().includes('/api/') && calls.push(r.url()),
    );

    await page.goto('/articles/demonstration-formules-diagramme-et-code', {
      waitUntil: 'networkidle',
    });

    expect(calls).toEqual([]);
    await expect(page.getByText('de la série')).toHaveCount(0);
  });

  test('answers an unknown series with a real 404', async ({ page, guard }) => {
    guard.allowHttpError('/series/serie-inexistante');
    guard.allowHttpError(`${API}/serie-inexistante`);

    const response = await page.goto('/series/serie-inexistante');

    expect(response?.status()).toBe(404);
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Série introuvable');
  });
});
