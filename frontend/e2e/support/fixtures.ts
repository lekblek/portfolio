import AxeBuilder from '@axe-core/playwright';
import { test as base, expect, Page, Request } from '@playwright/test';

/** Critères vérifiés par axe : WCAG 2.0, 2.1 et 2.2, niveaux A et AA (D17). */
const WCAG_TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];

export interface PageGuard {
  /** Déclare une réponse HTTP en erreur attendue (chemin exact, ou motif sur l'URL complète). */
  allowHttpError(url: string | RegExp): void;
}

/**
 * `guard`, automatique : le test échoue sur toute erreur ou tout avertissement de la console,
 * toute exception de la page, toute requête qui n'aboutit pas et toute réponse HTTP ≥ 400
 * qui n'a pas été déclarée attendue.
 */
export const test = base.extend<{ guard: PageGuard }>({
  guard: [
    async ({ page }, use) => {
      const allowed: (string | RegExp)[] = [];
      const problems: string[] = [];
      const isAllowed = (url: string) =>
        allowed.some((expected) =>
          typeof expected === 'string' ? new URL(url).pathname === expected : expected.test(url),
        );

      page.on('console', (message) => {
        if (message.type() !== 'error' && message.type() !== 'warning') {
          return;
        }
        // Le navigateur signale aussi en console la réponse en erreur déjà contrôlée ci-dessous
        const resource = message.location().url;
        if (message.text().startsWith('Failed to load resource') && isAllowed(resource)) {
          return;
        }
        // KI-38 (connu, mesuré en F27) : les médias n'existent qu'en taille d'origine, et Angular le
        // signale en développement pour chaque vignette (NG0913) ; ce seul avertissement est toléré
        if (message.type() === 'warning' && message.text().startsWith('NG0913')) {
          return;
        }
        problems.push(`console ${message.type()} : ${message.text()} (${resource})`);
      });
      page.on('pageerror', (error) => problems.push(`exception : ${error.message}`));
      // Requêtes dont la réponse est arrivée : le navigateur signale parfois leur fin comme une
      // annulation (`net::ERR_ABORTED`) juste après ; la réponse est contrôlée ci-dessous.
      const answered = new WeakSet<Request>();
      page.on('requestfailed', (request) => {
        if (request.failure()?.errorText === 'net::ERR_ABORTED' && answered.has(request)) {
          return;
        }
        problems.push(`requête en échec : ${request.url()} (${request.failure()?.errorText})`);
      });
      page.on('response', (response) => {
        answered.add(response.request());
        if (response.status() >= 400 && !isAllowed(response.url())) {
          problems.push(`HTTP ${response.status()} : ${response.url()}`);
        }
      });

      await use({ allowHttpError: (url) => allowed.push(url) });

      expect(problems, 'console et réseau sans erreur inattendue').toEqual([]);
    },
    { auto: true },
  ],
});

export { expect };

/** Aucune violation axe des critères WCAG A et AA sur l'état courant de la page. */
export async function expectAccessible(page: Page): Promise<void> {
  // Un élément encore en fondu fausserait le calcul des contrastes : mouvements terminés d'abord,
  // y compris ceux qui démarrent à l'image suivante (entrée d'un élément qui vient d'apparaître)
  await page.evaluate(async () => {
    await new Promise((resolve) => requestAnimationFrame(resolve));
    await Promise.all(document.getAnimations().map((motion) => motion.finished));
  });
  const { violations } = await new AxeBuilder({ page }).withTags(WCAG_TAGS).analyze();
  const summary = violations.map(
    (violation) =>
      `${violation.id} (${violation.impact}) : ${violation.help} — ${violation.nodes
        .map((node) => node.target.join(' '))
        .join(', ')}`,
  );
  expect(summary, 'violations d’accessibilité (axe)').toEqual([]);
}
