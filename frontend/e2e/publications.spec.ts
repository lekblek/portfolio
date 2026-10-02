import { Page } from '@playwright/test';

import { expect, expectAccessible, PageGuard, test } from './support/fixtures';
import { recordTransitions } from './support/motion';

// Rendu serveur, filtres et redirections : backend de développement (profil `dev`). Sommaire et
// bouton Copier : article long simulé dans le navigateur, après une navigation client.
const API = '/api/public/publications';

interface Summary {
  type: 'ARTICLE' | 'NEWS';
  title: string;
  slug: string;
  category: { name: string; slug: string } | null;
  tags: { name: string; slug: string }[];
}

const LONG_ARTICLE = {
  contentMarkdown: [
    'Introduction.',
    '',
    '## Contexte',
    '',
    'Texte du contexte[^1].',
    '',
    '## Réalisation',
    '',
    '```java',
    'record Detection(String zone) {}',
    '```',
    '',
    '[^1]: Une note de démonstration.',
  ].join('\n'),
  seoTitle: null,
  seoDescription: null,
};

async function articles(request: import('@playwright/test').APIRequestContext): Promise<Summary[]> {
  return (await (await request.get(`${API}?type=ARTICLE`)).json()).content;
}

async function openFirstArticleWithLongContent(
  page: Page,
  first: Summary,
  guard: PageGuard,
): Promise<void> {
  // Navigation dans le navigateur : un article hors série reçoit la 404 attendue de l'API (D-BL)
  guard.allowHttpError(`${API}/${first.slug}/series`);
  // Liste hydratée : le clic est une navigation dans le navigateur (réponse simulée), pas un
  // rechargement rendu par le serveur
  await page.goto('/articles', { waitUntil: 'networkidle' });
  await page.route(`**${API}/${first.slug}`, async (route) => {
    const real = await (await route.fetch()).json();
    await route.fulfill({ json: { ...real, ...LONG_ARTICLE } });
  });
  await page.getByRole('link', { name: first.title }).click();
  await expect(page.getByRole('heading', { level: 2, name: 'Contexte' })).toBeVisible();
}

test.describe('publications', () => {
  test('lists the articles of the api in the server html, without calling it again', async ({
    page,
    request,
  }) => {
    const list = await articles(request);
    const html = await (await request.get('/articles')).text();
    for (const article of list) {
      expect(html).toContain(`href="/articles/${article.slug}"`);
    }

    const calls: string[] = [];
    page.on('request', (r) => r.url().includes('/api/public/') && calls.push(r.url()));
    await page.goto('/articles', { waitUntil: 'networkidle' });

    expect(calls).toEqual([]);
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Articles');
    await expect(page.getByRole('heading', { level: 2 })).toHaveCount(list.length);
    await expectAccessible(page);
  });

  test('filters by category, then by tag, and removes each filter alone', async ({
    page,
    request,
  }) => {
    const article = (await articles(request)).find((a) => a.category && a.tags.length > 0);
    test.skip(!article, 'aucun article dev avec catégorie et tags');
    const { category, tags } = article as Summary & { category: { name: string; slug: string } };
    await page.goto('/articles');

    // La page filtrée doit être reçue avant le clic suivant, qui annulerait sa requête
    const filtered = page.waitForResponse((r) => r.url().includes(`category=${category.slug}`));
    await page
      .getByRole('link', { name: `Catégorie : ${category.name}` })
      .first()
      .click();
    await filtered;
    await expect(page).toHaveURL(`/articles?category=${category.slug}`);
    await expect(page.getByRole('list', { name: 'Filtres actifs' })).toContainText(category.name);
    await page
      .getByRole('list', { name: `Tags de ${article?.title}` })
      .getByRole('link', { name: tags[0].name })
      .click();
    await expect(page).toHaveURL(`/articles?tag=${tags[0].slug}`);
    await expect(page.getByRole('list', { name: 'Filtres actifs' })).toContainText(tags[0].name);
    await expectAccessible(page);

    await page.getByRole('link', { name: 'Retirer ce filtre de tag' }).click();
    await expect(page).toHaveURL('/articles');
  });

  test('shows news under their own address', async ({ page, request }) => {
    const news: Summary[] = (await (await request.get(`${API}?type=NEWS`)).json()).content;
    await page.goto('/news');

    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Actualités');
    await expect(page.getByRole('heading', { level: 2 })).toHaveCount(news.length);
    if (news.length > 0) {
      await expect(page.getByRole('link', { name: news[0].title })).toHaveAttribute(
        'href',
        `/news/${news[0].slug}`,
      );
    }
  });

  test('renders an article in the server html, without calling the api again', async ({
    page,
    request,
  }) => {
    const [first] = await articles(request);
    const html = await (await request.get(`/articles/${first.slug}`)).text();
    expect(html).toContain(`>${first.title}</h1>`);
    expect(html).toContain('"@type":"Article"');

    const calls: string[] = [];
    page.on('request', (r) => r.url().includes('/api/public/') && calls.push(r.url()));
    await page.goto(`/articles/${first.slug}`, { waitUntil: 'networkidle' });

    expect(calls).toEqual([]);
    await expect(page.getByText('min de lecture').first()).toBeAttached();
    await expectAccessible(page);
  });

  test('redirects a publication opened under the wrong type', async ({ page, request }) => {
    const news: Summary[] = (await (await request.get(`${API}?type=NEWS`)).json()).content;
    test.skip(news.length === 0, 'aucune actualité dev');

    const response = await request.get(`/articles/${news[0].slug}`, { maxRedirects: 0 });
    expect(response.status()).toBe(301);
    expect(response.headers()['location']).toBe(`/news/${news[0].slug}`);

    await page.goto(`/articles/${news[0].slug}`);
    await expect(page).toHaveURL(`/news/${news[0].slug}`);
  });

  test('answers an unknown publication with a real 404', async ({ page, guard }) => {
    guard.allowHttpError('/articles/publication-inexistante');
    guard.allowHttpError(`${API}/publication-inexistante`);

    const response = await page.goto('/articles/publication-inexistante');

    expect(response?.status()).toBe(404);
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Article introuvable');
  });

  test('reaches the sections from the table of contents with the keyboard', async ({
    page,
    request,
    guard,
  }, info) => {
    const [first] = await articles(request);
    await openFirstArticleWithLongContent(page, first, guard);

    if (info.project.name === 'mobile') {
      const summary = page.locator('summary', { hasText: 'Sommaire' });
      await summary.focus();
      await page.keyboard.press('Enter');
    }
    const toc = page.getByRole('navigation', { name: `Sommaire de «\u00a0${first.title}\u00a0»` });
    const link = toc.getByRole('link', { name: 'Réalisation' });
    await link.focus();
    await page.keyboard.press('Enter');

    await expect(page).toHaveURL(new RegExp(`/articles/${first.slug}#realisation$`));
    await expect(page.getByRole('heading', { level: 2, name: 'Réalisation' })).toBeInViewport();
    await expect(page.getByRole('note')).toContainText('Une note de démonstration.');
    await expectAccessible(page);
  });

  test('copies a code block and announces it', async ({ page, request, context, guard }) => {
    await context.grantPermissions(['clipboard-read', 'clipboard-write']);
    const [first] = await articles(request);
    await openFirstArticleWithLongContent(page, first, guard);
    const transitions = await recordTransitions(page, '.code-copy');

    await page.getByRole('button', { name: 'Copier le code' }).click();

    await expect(page.getByText('Code copié dans le presse-papiers.')).toBeAttached();
    // Retour de pression des boutons (F08, F20)
    await expect.poll(transitions).toEqual(['transform']);
    expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(
      'record Detection(String zone) {}',
    );
  });

  test.describe('with reduced motion', () => {
    test.use({ reducedMotion: 'reduce' });

    test('copies a code block without moving its button', async ({ page, request, guard }) => {
      const [first] = await articles(request);
      await openFirstArticleWithLongContent(page, first, guard);
      const button = page.getByRole('button', { name: 'Copier le code' });
      const transitions = await recordTransitions(page, '.code-copy');

      await button.hover();
      await page.mouse.down();
      await expect(button).toHaveCSS('transform', 'none');
      await page.mouse.up();

      expect(await transitions()).toEqual([]);
    });
  });
});
