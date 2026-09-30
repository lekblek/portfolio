# Frontend — Portfolio

Application Angular 22 avec rendu serveur (SSR) et rendu hybride. Styles : Tailwind CSS 4. Tests unitaires : Vitest via `@angular/build:unit-test`. Tests de bout en bout : Playwright et axe.

Documentation générale, prérequis et démarrage : [README principal](../README.md). Architecture : [`docs/frontend/01-architecture.md`](../docs/frontend/01-architecture.md).

## Commandes

| Commande | Rôle |
|---|---|
| `npm ci` | installe les dépendances exactes du `package-lock.json` |
| `npm start` | serveur de développement sur `http://localhost:4200` (rendu serveur, proxy `/api`) |
| `npm run lint` | ESLint : règles Angular, accessibilité des gabarits, frontières entre dossiers |
| `npm run lint:styles` | refuse valeurs arbitraires de Tailwind, couleurs littérales et `!important` hors de `src/styles/tokens.css` |
| `npm run format` / `npm run format:check` | Prettier : corrige / vérifie le formatage |
| `npm test -- --watch=false` | tests unitaires, une exécution |
| `npm run api:types` | régénère `src/app/core/api/openapi.d.ts` depuis `docs/api/openapi.json` |
| `npm run build` | build de production (navigateur + serveur SSR) dans `dist/frontend/` |
| `npm run serve:ssr:frontend` | sert le build SSR (`PORT`, défaut 4000) |
| `npm run e2e` | tous les tests de bout en bout (backend de développement requis) |
| `npm run e2e:no-api` | tests de bout en bout marqués `@no-api`, sans backend (exécutés en CI) |

Vérification de référence avant un commit : [`docs/conventions-git.md`](../docs/conventions-git.md) §21.

## API en développement

Le navigateur appelle toujours `/api/...` en relatif. En développement, `proxy.conf.mjs` transmet ces appels au backend désigné par `API_ORIGIN` (défaut `http://localhost:8080`) :

```bash
API_ORIGIN=http://localhost:8081 npm start
```

Pendant le rendu serveur, les appels `/api/...` sont préfixés par la même variable `API_ORIGIN`, lue dans l'environnement du serveur SSR ; elle n'est jamais déduite de la requête reçue.

## Servir le build SSR en local

Variables d'environnement du serveur SSR :

| Variable | Rôle |
|---|---|
| `SITE_URL` | origine publique du site (`https://…`) pour les adresses canoniques et Open Graph ; **obligatoire** : le serveur refuse de démarrer sans elle |
| `API_ORIGIN` | origine interne de l'API pour le rendu serveur (défaut `http://localhost:8080`) |
| `NG_ALLOWED_HOSTS` | hôtes acceptés dans l'en-tête `Host` ; la liste `security.allowedHosts` d'`angular.json` est encore vide (KI-22), le serveur refuse donc tout autre hôte |
| `PORT` | port d'écoute (défaut 4000) |

```bash
NG_ALLOWED_HOSTS=localhost SITE_URL=http://localhost:4000 npm run serve:ssr:frontend
```

PowerShell : `$env:NG_ALLOWED_HOSTS='localhost'; $env:SITE_URL='http://localhost:4000'; npm run serve:ssr:frontend`.

Le serveur de développement (`npm start`) n'exige pas `SITE_URL` : il prend `http://localhost:4200`.

## Système de design

- `src/styles/tokens.css` : tokens (`@theme` de Tailwind 4, thème par défaut retiré) ; seule source des valeurs. `base.css` : éléments HTML nus, focus, mouvement réduit. `utilities.css` : `page-container`, `stack-*`, `cluster-*`.
- Polices auto-hébergées par `@fontsource-variable` : Schibsted Grotesk, Literata, JetBrains Mono.
- Primitives : `src/app/shared/ui` (`button[appButton]`, `a[appButton]`, `app-icon`).
- Catalogue `http://localhost:4200/_ui` : tokens appliqués, contrastes calculés, composition, primitives, états d'écran. Spécimen du rendu Markdown : `http://localhost:4200/_ui/prose`. Développement seulement : en production, ces adresses répondent 404.
- Rendu Markdown : `src/app/shared/markdown` (`markdown-it`, `highlight.js`, [ADR 0003](../docs/decisions/0003-rendu-markdown.md)) ; styles de lecture `src/styles/prose.css`.
- Documentation : [`docs/frontend/02-design-system.md`](../docs/frontend/02-design-system.md). Notices des polices et des icônes : [`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md).

## Tests de bout en bout

Prérequis, une fois par poste : `npx playwright install chromium` (version fixée par `@playwright/test`).

- `playwright.config.ts` : projets `desktop` (1440 × 900) et `mobile` (390 × 844, tactile) ; le serveur de développement est démarré, ou réutilisé s'il tourne déjà.
- Toute spécification importe `test`, `expect` et `expectAccessible` de `e2e/support/fixtures.ts` : un test échoue sur une erreur de console, une requête en échec, une réponse HTTP en erreur non déclarée (`guard.allowHttpError('/chemin')`) ou une violation axe (WCAG 2.2 A et AA).
- Une spécification qui n'a pas besoin du backend porte l'étiquette `@no-api`.
- Captures, traces et rapports : `test-results/`, `playwright-report/` (ignorés par Git).

## État

Angular 22.2 (versions : [`docs/03-versions-cibles.md`](../docs/03-versions-cibles.md) §11.2). Socle en place : HTTP, routeur, titre des pages, mode de rendu par route (`app.routes.server.ts`), page introuvable avec statut 404 réel, types de l'API générés. Système de design exécutable : tokens, polices, styles de base, utilitaires de composition, primitives d'action. Shell public (en-tête, navigation, pied de page, lien d'évitement, focus après navigation), service SEO (titre, description, canonical, Open Graph, JSON-LD), rendu Markdown. Aucune page métier : la navigation reste vide tant qu'aucune page publique n'existe.

Lot initial du navigateur (build de production, 2026-09-30) : 234,16 kB bruts / 65,43 kB transférés pour le squelette nettoyé ; voir `docs/progress.md` pour le relevé après le socle.
