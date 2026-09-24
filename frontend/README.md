# Frontend — Portfolio

Application Angular 22 avec rendu serveur (SSR) et rendu hybride. Styles : Tailwind CSS 4. Tests : Vitest via `@angular/build:unit-test`.

Documentation générale, prérequis et démarrage : [README principal](../README.md).

## Commandes

| Commande | Rôle |
|---|---|
| `npm ci` | installe les dépendances exactes du `package-lock.json` |
| `npm start` | serveur de développement sur `http://localhost:4200` |
| `npm test -- --watch=false` | tests unitaires, une exécution |
| `npm run build` | build de production (navigateur + serveur SSR) dans `dist/frontend/` |
| `npm run serve:ssr:frontend` | sert le build SSR (`PORT`, défaut 4000) |

## État

Squelette initial (étape 06) : aucune route, aucun appel API. L'organisation cible (`core/`, `shared/`, `layout/`, `features/…`) sera posée à l'étape 37.

Configuration de rendu : `src/app/app.routes.server.ts` — rendu serveur à la demande pour toutes les routes (décision D22 ; prérendu décidé route par route à l'étape 52.1).
