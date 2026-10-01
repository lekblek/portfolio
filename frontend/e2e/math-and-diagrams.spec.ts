import { expect, expectAccessible, test } from './support/fixtures';

// Publication de démonstration du profil `dev` (PublicationSeeder, ajoutée même à une base existante).
const DEMO = '/articles/demonstration-formules-diagramme-et-code';

test.describe('math and diagrams', () => {
  test('renders the formulas on the server, with MathML and the KaTeX stylesheet', async ({
    request,
  }) => {
    const html = await (await request.get(DEMO)).text();

    expect(html).toContain('class="katex"');
    expect(html).toContain('<math');
    expect(html).toContain('/katex/katex.min.css');
    // Diagramme : son source, en repli, dans le HTML serveur
    expect(html).toContain('data-diagram');
    expect(html).toContain('accTitle: Chaîne de traitement de démonstration');
  });

  test('adds no KaTeX stylesheet to a page without formulas', async ({ request }) => {
    const html = await (await request.get('/about')).text();

    expect(html).not.toContain('katex');
  });

  test('draws the diagram with its accessible title once it is reached', async ({ page }) => {
    // Après l'hydratation, qui réattache le contenu rendu
    await page.goto(DEMO, { waitUntil: 'networkidle' });

    const figure = page.locator('figure[data-diagram]');
    await figure.scrollIntoViewIfNeeded();
    await expect(figure).toHaveAttribute('data-diagram-state', 'drawn', { timeout: 20_000 });
    await expect(figure.locator('svg title')).toHaveText('Chaîne de traitement de démonstration');
    await expect(figure.getByText('Source du diagramme')).toBeVisible();
    await expectAccessible(page);
  });

  test('scrolls a long formula inside its block, never the page', async ({ page }) => {
    await page.setViewportSize({ width: 360, height: 780 });
    await page.goto(DEMO);

    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
      360,
    );
    await expect
      .poll(() =>
        page
          .locator('.math-display')
          .nth(1)
          .evaluate((block) => block.scrollWidth > block.clientWidth),
      )
      .toBe(true);
  });
});
