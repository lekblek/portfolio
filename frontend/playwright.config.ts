import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env['E2E_BASE_URL'] ?? 'http://localhost:4200';

/**
 * Tests de bout en bout (docs/frontend/01-architecture.md §17).
 * Les spécifications marquées `@no-api` n'ont pas besoin du backend : `npm run e2e:no-api`.
 */
export default defineConfig({
  testDir: 'e2e',
  outputDir: 'test-results',
  fullyParallel: true,
  forbidOnly: !!process.env['CI'],
  retries: 0,
  reporter: process.env['CI'] ? [['github'], ['list']] : [['list']],
  use: {
    baseURL,
    locale: 'fr-FR',
    timezoneId: 'Europe/Paris',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'desktop',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } },
    },
    {
      name: 'mobile',
      use: { ...devices['Pixel 7'], viewport: { width: 390, height: 844 } },
    },
  ],
  // Serveur de développement avec rendu serveur ; celui déjà lancé à la main est réutilisé en local.
  // Prêt quand un fichier statique répond : une page peut légitimement répondre 404.
  webServer: {
    command: 'npm start',
    url: `${baseURL}/favicon.ico`,
    reuseExistingServer: !process.env['CI'],
    timeout: 120_000,
  },
});
