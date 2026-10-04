# Architecture frontend — Portfolio V1

Statut : **Acceptée** (2026-09-30), à confirmer par l'étape F02 qui la met en place.

Ce document fixe l'organisation de l'application Angular (`frontend/`), la pile retenue et les règles qui s'appliquent à chaque écran. Il est normatif, au même titre que [`04-architecture-backend.md`](../04-architecture-backend.md) pour le backend.

| Document | Rôle |
|---|---|
| ce document | stack, structure, règles de dépendance, rendu, API, état, formulaires, tests |
| [`02-design-system.md`](02-design-system.md) | identité visuelle, tokens, stratégie de style, primitives |
| `00-roadmap-frontend.md` | ordre de construction, étapes F00 à F37 (non versionné, R-8) |

Hiérarchie en cas de contradiction : code du dépôt > `progress.md` et `decisions/` > ce document et `02-design-system.md` > feuille de route.

---

## 1. État de départ (relevé du 2026-09-30)

| Élément | Constat |
|---|---|
| Angular | `@angular/core` 22.1.7, CLI / build / SSR 22.1.8 (lockfile) ; 22.2.0 publiée le 2026-09-23 |
| TypeScript | 6.0.3, `strict` et `strictTemplates` (R-5) |
| Détection de changements | sans zone : `zone.js` absent des dépendances, aucun `provideZoneChangeDetection` ; `OnPush` par défaut en v22 |
| Rendu | `outputMode: server`, un seul `ServerRoute` `'**'` en `RenderMode.Server` (D22) ; `provideClientHydration()` sans option, qui active en v22 l'hydratation incrémentale et le rejeu des événements |
| Routage | `routes = []` ; aucune page |
| HTTP | aucun `provideHttpClient`, aucun appel à l'API |
| Styles | Tailwind CSS 4.3.3 par `@tailwindcss/postcss` ; `styles.css` ne contient que `@import 'tailwindcss'` ; `app.css` vide ; aucun token |
| Tests | Vitest 4.1.11 (`@angular/build:unit-test`, jsdom) ; `app.spec.ts` vérifie le titre du squelette |
| Qualité | Prettier configuré, **aucun ESLint**, aucun test de bout en bout, aucun contrôle d'accessibilité automatisé |
| Budgets | initial 500 kB / 1 MB ; style de composant 4 kB / 8 kB |
| CI | `npm ci`, `npm test -- --watch=false`, `npm run build` (Node 24) |
| Problèmes ouverts | KI-21 (`@types/node ^20` pour Node 24), KI-22 (`security.allowedHosts` vide), KI-29 (`App.title` sans usage) |

Le contrat de l'API est versionné dans [`docs/api/openapi.json`](../api/openapi.json) (D-DG). Particularités utiles au frontend :

- les chemins n'ont pas le préfixe `/api` (chemin de contexte du serveur) : `/public/profile` est servi à `/api/public/profile` ;
- les `operationId` sont génériques (`list_6`, `update_5`) : ils ne servent pas à nommer du code ;
- les schémas (`components.schemas`) sont nommés et stables : ils sont la source des types TypeScript ;
- les contenus longs arrivent en Markdown brut (`contentMarkdown`, `descriptionMarkdown`, `aboutMarkdown`) : le rendu HTML est fait par le frontend ;
- les images publiques (`PublicImage`) portent `url`, `width`, `height`, `altText` ;
- les routes d'essai `/test-errors/**` figurent dans le contrat mais ne sont jamais appelées par l'application.

---

## 2. Principes

1. **Le site public est un document avant d'être une application.** Chaque page publique est rendue par le serveur, lisible sans JavaScript, puis hydratée.
2. **Aucune abstraction sans besoin présent.** Pas de bibliothèque d'état, pas de couche « repository » générique, pas de composant créé « au cas où ».
3. **Organisation par fonctionnalité**, pas par couche technique. L'architecture hexagonale du backend n'est pas reproduite : un écran Angular n'a ni ports ni adaptateurs.
4. **La plateforme d'abord.** `<dialog>`, `popover`, `:focus-visible`, `@starting-style`, container queries : une API du navigateur passe avant une dépendance.
5. **Le contrat de l'API fait foi.** Les types viennent de `openapi.json` ; une divergence casse la compilation.
6. **Accessibilité et rendu vérifiés dans un vrai navigateur.** Une page n'est pas terminée parce que `ng build` réussit (voir la feuille de route, portes de qualité).

---

## 3. Pile retenue

| Technologie | Décision | Justification |
|---|---|---|
| Composants autonomes (standalone) | **USE** | défaut d'Angular 22 ; aucun `NgModule` |
| Signals (`signal`, `computed`, `linkedSignal`, `input`, `model`) | **USE** | état local et dérivé ; modèle réactif natif, sans zone |
| `httpResource` | **USE** | lectures de l'API sous forme de signaux (`isLoading`, `error`, `value`), annulation des requêtes dépassées, compatible SSR et intercepteurs |
| `HttpClient` | **USE** | écritures (POST, PUT, PATCH, DELETE) : `httpResource` est réservé aux lectures |
| Signal Forms (`@angular/forms/signals`) | **USE** | stables en v22, typées, validation par schéma ; premier usage : formulaire de contact (F19), puis toute l'administration |
| Reactive Forms | **DO NOT USE** | remplacés par Signal Forms pour tout nouveau formulaire |
| SSR (`@angular/ssr`, Express) | **USE** | déjà en place (D22) ; SEO, premier affichage |
| Hydratation incrémentale, rejeu d'événements | **USE** | activés par `provideClientHydration()` en v22 ; `@defer (hydrate on …)` pour les blocs lourds |
| `@defer` | **USE** | KaTeX, Mermaid, éditeur, blocs sous la ligne de flottaison ; jamais sur le contenu principal d'une page publique |
| Prérendu (`RenderMode.Prerender`) | **USE LATER** | décidé route par route en F33 (D22, étape 52.1) |
| Angular Aria (`@angular/aria`) | **USE** (F27) | combobox et liste à choix multiples (`shared/ui/multi-select.ts`) ; onglets à F31 ; chargé avec les pages d'administration seulement ; rien d'autre sans besoin |
| Angular CDK (`@angular/cdk`) | **USE** (F26) | glisser-déposer des listes ordonnées (`shared/ui/sortable-list.ts` : collections du profil, puis chapitres), en complément de commandes au clavier, et annonces (`LiveAnnouncer`) ; chargé avec les pages d'administration seulement ; rien d'autre sans besoin |
| Angular Material | **DO NOT USE** | apparence Material Design contraire à l'identité recherchée ; thème lourd à neutraliser ; les besoins réels sont couverts par la plateforme, Aria et CDK |
| `@angular/animations` | **DO NOT USE** | API historique ; les transitions CSS et `animate.enter` / `animate.leave` d'Angular suffisent |
| Tailwind CSS 4 | **USE** | déjà installé ; porte les tokens (`@theme`) et les utilitaires de mise en page ; cadré par [`02-design-system.md`](02-design-system.md) §17 |
| `openapi-typescript` | **USE** (F03) | génère des **types seulement** (aucun code exécuté) depuis `docs/api/openapi.json` ; détecte toute dérive du contrat |
| ESLint (`angular-eslint`) | **USE** (F01) | règles Angular, règles d'accessibilité des gabarits, frontières entre dossiers (`no-restricted-imports`) |
| Vitest | **USE** | déjà en place pour les tests unitaires et de composants |
| Playwright Test (`@playwright/test`) + `@axe-core/playwright` | **USE** (F04) | tests de bout en bout versionnés, contrôles d'accessibilité automatisés, captures de référence |
| Storybook (+ addon a11y) | **DO NOT USE** en V1 | un seul développeur, peu de primitives : un catalogue interne (`/_ui`, développement seulement) et Playwright couvrent le besoin sans deuxième chaîne de build ; à reconsidérer si les primitives dépassent une trentaine |
| Bibliothèque d'état (NgRx, Signal Store) | **DO NOT USE** | aucun état partagé complexe : signaux, services et URL suffisent |
| Bibliothèque d'icônes | **DO NOT USE** | quelques icônes SVG en ligne, voir `02-design-system.md` §14 |

Versions : Angular, CDK et Aria restent sur la même version mineure (`03-versions-cibles.md` §9). La montée 22.1 → 22.2 est faite en F00, avant toute installation de paquet `@angular/*`.

---

## 4. Structure des dossiers

```text
frontend/
├── e2e/                         tests Playwright (F04)
├── public/                      fichiers servis tels quels (favicon, polices si auto-hébergées hors npm)
├── proxy.conf.mjs               proxy du serveur de développement vers l'API (F02)
└── src/
    ├── styles/                  système de design global (F06)
    │   ├── tokens.css           source de vérité : @theme Tailwind (couleurs, typo, espacements…)
    │   ├── base.css             éléments HTML, focus, typographie de base, mouvement réduit
    │   ├── utilities.css        utilitaires de mise en page du projet (@utility)
    │   └── prose.css            rendu du Markdown (F11)
    ├── styles.css               point d'entrée : importe tailwindcss puis styles/*
    └── app/
        ├── app.ts / app.html    racine : <router-outlet> seulement
        ├── app.config.ts        fournisseurs navigateur + communs
        ├── app.config.server.ts fournisseurs serveur (origine de l'API, rendu)
        ├── app.routes.ts        routes de premier niveau, toutes chargées à la demande
        ├── app.routes.server.ts mode de rendu par route
        ├── core/                singletons transverses, sans écran
        │   ├── api/             types générés, alias, erreurs, pagination, intercepteurs
        │   ├── seo/             titre, meta, canonical, Open Graph, JSON-LD
        │   └── platform/        statut HTTP côté serveur (404), détection serveur/navigateur
        ├── shared/              réutilisable par plusieurs fonctionnalités, sans état métier
        │   ├── ui/              primitives du système de design (bouton, champ, dialogue…)
        │   ├── content/         entrées de contenu affichées par plusieurs pages (projet, publication, série)
        │   ├── markdown/        rendu Markdown partagé par le site et l'aperçu d'administration
        │   └── format/          formatage français (dates, durées, tailles) par Intl
        ├── layout/
        │   ├── public-shell/    en-tête, navigation, pied de page, navigation mobile
        │   └── admin-shell/     barre latérale, en-tête d'administration
        ├── features/
        │   ├── home/  about/  projects/  publications/  series/  search/  contact/  not-found/
        │   └── admin/
        │       ├── auth/        connexion, session, garde, déconnexion
        │       ├── editor/      éditeur de contenu Visuel / Markdown / Aperçu (F31, ADR 0004)
        │       ├── dashboard/  profile/  projects/  publications/  series/
        │       ├── taxonomy/    catégories, tags, technologies
        │       ├── media/  messages/
        │       └── admin.routes.ts
        └── dev/                 catalogue /_ui des primitives (développement seulement, F06)
```

Un dossier n'est créé qu'avec son premier fichier réel : la structure ci-dessus est une cible, pas un échafaudage vide.

---

## 5. Règles de dépendance

| Depuis | Peut importer | Ne peut pas importer |
|---|---|---|
| `features/<x>` | `core`, `shared`, `layout` (routes seulement) | une autre fonctionnalité (`features/<y>`) |
| `features/admin/<x>` | `core`, `shared`, `features/admin/auth`, `features/admin/editor` (éditeur de contenu, F31) | le site public (`features/<public>`) |
| `layout` | `core`, `shared` | `features` |
| `shared` | `core` (types de l'API compris) | `features`, `layout` |
| `core` | rien d'applicatif | `shared`, `features`, `layout` |

Deux fonctionnalités qui ont besoin du même composant le déplacent dans `shared/` (s'il est générique) ou dupliquent un gabarit de quelques lignes (s'il ne l'est pas) : pas d'import croisé.

Contrôle : règle ESLint locale `project/folder-boundaries` (`frontend/eslint/folder-boundaries.js`), exécutée par `npm run lint` et en CI. Elle résout chaque import relatif (statique, réexportation ou `import()` dynamique) vers sa zone (`core`, `shared`, `layout`, `dev`, `features/<x>`, `features/admin/<x>`) et applique ce tableau. `no-restricted-imports`, envisagé d'abord, ne compare que le texte de l'import : `../../b/x`, écrit depuis `features/a`, ne nomme pas le dossier visé et passait le contrôle (vérifié par mutation le 2026-09-30). Les fichiers de la racine (`app.ts`, `app.config*.ts`, `app.routes*.ts`) peuvent tout importer ; `features/admin/admin.routes.ts` assemble les sous-fonctionnalités d'administration.

---

## 6. Anatomie d'une fonctionnalité

```text
features/projects/
├── projects.routes.ts        routes de la fonctionnalité (chargées par loadChildren)
├── data/
│   └── projects.resources.ts fonctions de lecture (httpResource) et, en administration, service d'écriture
├── pages/
│   ├── project-list.ts       composant routé : lit l'URL, orchestre les ressources, fixe le SEO
│   └── project-detail.ts
└── ui/
    └── project-entry.ts      composant d'affichage : entrées (input), sorties (output), aucun appel HTTP
```

- **Page** : seul composant qui lit la route et l'API, qui gère les états (chargement, vide, erreur, introuvable) et qui fixe le titre et les métadonnées.
- **Composant d'affichage** (`ui/`) : reçoit des données typées, n'injecte aucun service d'accès aux données.
- **Données** (`data/`) : fonctions `xxxResource(params)` appelées dans un contexte d'injection, retournant un `httpResource` typé ; en administration, un service `@Service()` d'écriture par ressource.
- Nommage des fichiers selon le guide de style d'Angular 20+ : `project-list.ts`, pas `project-list.component.ts` (déjà suivi par `app.ts`).
- Préfixe des sélecteurs : `app` (`app-project-entry`, `button[appButton]`), vérifié par ESLint.

---

## 7. Routage et rendu

| Route | Fonctionnalité | Rendu | Remarque |
|---|---|---|---|
| `/` | `home` | Server | agrège profil, projets mis en avant, publications, séries |
| `/about` | `about` | Server | profil complet, CV |
| `/projects`, `/projects/:slug` | `projects` | Server | filtre `?technology=`, pagination `?page=` |
| `/articles`, `/articles/:slug` | `publications` | Server | `type=ARTICLE`, filtres `?category=`, `?tag=` |
| `/news`, `/news/:slug` | `publications` | Server | `type=NEWS` |
| `/series`, `/series/:slug` | `series` | Server | table des matières ; navigation de chapitre sur la page d'article |
| `/search` | `search` | Server | `?q=`, `?page=` ; résultat partageable par URL |
| `/contact` | `contact` | Server | formulaire hydraté |
| `/admin/login`, `/admin/**` | `admin` | **Client** | pas de SEO, cookie de session `SameSite=Strict`, aucune requête d'administration depuis le serveur Node |
| `/_ui` | `dev` | Server | catalogue du système de design, `canMatch: () => isDevMode()` : en production, la route ne correspond pas et l'adresse répond 404 (le mode `Client` servirait une coquille en 200) |
| `**` | `not-found` | Server | statut HTTP **404** posé côté serveur |
| `/sitemap.xml`, `/robots.txt` | — | — | décidés en F33 (route Express de `server.ts` ou point d'API) |

Configuration du routeur (F02) : `withComponentInputBinding()` (paramètres de route en `input()`), `withInMemoryScrolling({ scrollPositionRestoration: 'enabled', anchorScrolling: 'enabled' })`, chargement paresseux de chaque fonctionnalité, `TitleStrategy` du projet (« Titre de page — Nom du site »).

Statut 404 : une page qui reçoit une 404 de l'API (slug inconnu, contenu non public) affiche la page introuvable et pose le statut 404 de la réponse SSR (`RESPONSE_INIT`, par `core/platform/response-status.ts`), sans redirection.

Mode de rendu : `app.routes.server.ts` s'applique aux routes que l'application déclare. Tant qu'aucune route `admin` n'existe, `/admin/x` est une adresse inconnue, rendue par le serveur avec le statut 404 ; une route `admin/...` déclarée est servie en rendu client (vérifié par une route d'essai le 2026-09-30).

---

## 8. Accès à l'API

### 8.1 Types

- `npm run api:types` génère `src/app/core/api/openapi.d.ts` depuis `../docs/api/openapi.json` (`openapi-typescript`, F03). Le fichier généré est versionné et jamais modifié à la main.
- `core/api/api-types.ts` expose des alias lisibles : `export type ProjectSummary = Schemas['ProjectSummaryResponse']`.
- La CI régénère les types et échoue si le fichier diffère (`git diff --exit-code`) : un changement du contrat backend impose la mise à jour du frontend dans le même lot.
- `openapi-typescript` 7.13 déclare `typescript ^5` comme dépendance homologue ; le projet est en TypeScript 6. Un `overrides` de `package.json`, limité à ce paquet, lui fait utiliser le TypeScript du projet (génération vérifiée identique et déterministe). À retirer quand une version compatible paraîtra.
- Les schémas de réponse déclarent toutes leurs propriétés `required` (toujours présentes dans le JSON) et n'admettent `null` que pour les composantes `@Nullable` des records du backend (D-DZ, KI-34 corrigé à la source) : `endDate: string | null`, `avatar: PublicImage | null`, collections jamais absentes. Aucun `!`, cast ou `Required<>` côté frontend. Un schéma aussi reçu en corps de requête garde la sémantique de la requête (propriétés facultatives omissibles, KI-36).
- `core/api/api-error.ts` : `ApiProblem` (forme de `05-conventions-api.md` §10, absente du contrat), `ApiError` et `toApiError`. `core/api/page.ts` : `Page<T>` (métadonnées reprises du schéma `PageResponse…`), `PageRequest` et `toHttpParams`.

### 8.2 URL et origine

- Le navigateur appelle toujours des URL relatives `/api/...` : même origine en production (Caddy), proxy `proxy.conf.mjs` en développement (cible `API_ORIGIN`, défaut `http://localhost:8080`).
- Le serveur SSR n'a pas d'origine « courante » fiable : l'intercepteur commun `core/api/api-request.ts` **marque** les requêtes `/api/` (`API_REQUEST`) sans changer leur URL ; au serveur seulement, le transport `core/api/server-api-backend.ts` (`ServerApiBackend`, sous-classe de `FetchBackend`, fourni par `app.config.server.ts` avec `API_ORIGIN`, défaut `http://localhost:8080`) envoie chaque requête marquée à l'origine interne en ne gardant que son chemin et sa requête. `@angular/platform-server` rend les URL relatives absolues sur l'origine de la page, tirée de l'en-tête `Host` : cette origine est remplacée, elle ne choisit jamais la destination (pas de SSRF). La réécriture au transport, après tous les intercepteurs, garde au cache de transfert une clé relative identique au serveur et dans le navigateur (D-EB).
- `withFetch()` n'est pas appelé : Fetch est le moteur par défaut de `HttpClient` en v22 (la fonction est dépréciée).
- Cache de transfert : les GET de `/api/public/` faits pendant le rendu serveur sont transmis au navigateur (`withHttpTransferCacheOptions({ filter, includeNonCacheableRequests: true })` : Spring Security marque toute réponse `no-store`, que le cache de transfert refuse sinon). Vérifié en F12 sur le build de production : aucune requête API rejouée au premier affichage (`e2e/about.spec.ts`). Une réponse en erreur n'est pas transférée : le navigateur réessaie une fois (D-EB).

### 8.3 Lectures

```ts
// features/projects/data/projects.resources.ts
export function projectListResource(query: () => ProjectQuery) {
  return httpResource<Page<ProjectSummary>>(() => ({
    url: '/api/public/projects',
    params: toHttpParams(query()),
  }));
}
```

- Le gabarit distingue `isLoading()`, `error()`, `hasValue()` ; `value()` n'est jamais lu sans `hasValue()`.
- Le rendu serveur attend la fin des ressources avant de sérialiser la page.

### 8.4 Écritures

- `HttpClient` dans un service d'écriture (administration, contact) ; conversion en promesse au point d'appel (`firstValueFrom`) pour la soumission des Signal Forms.
- CSRF : comportement par défaut d'Angular (cookie `XSRF-TOKEN`, en-tête `X-XSRF-TOKEN`, URL relatives, méthodes non sûres), aligné sur D-CP. Aucune configuration tant que ce défaut suffit.

### 8.5 Erreurs

- `core/api/api-error.ts` convertit une `HttpErrorResponse` portant un `ProblemDetail` en `ApiError { status, code, detail, fieldErrors }` ; les codes stables (`05-conventions-api.md`) pilotent l'affichage, jamais le texte.
- Les textes de l'API sont en français (D-DJ) : `detail` et les messages de champ peuvent être affichés tels quels.
- 404 → page introuvable ; 401 en administration → retour à `/admin/login?returnUrl=…` ; 409 / 400 → message sur le champ ou sur le formulaire ; 429 → message d'attente ; 5xx et réseau → état d'erreur avec action « Réessayer ».

### 8.6 Pagination

`Page<T>` reprend `PageResponse` (`content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`). La page courante vit dans l'URL (`?page=`, base 1 côté URL, convertie vers la base de l'API dans `toHttpParams`). Taille imposée par le serveur (10 public, 20 administration, D16).

---

## 9. État

| Nature | Où |
|---|---|
| état d'un composant | `signal`, `computed`, `linkedSignal` locaux |
| données serveur | `httpResource` dans la page |
| filtres, recherche, pagination, onglets partageables | **URL** (paramètres de requête), lus en `input()` |
| session d'administration | service `AdminSession` (signal de l'utilisateur courant), `features/admin/auth` |
| préférences de confort | aucune en V1 (pas de thème sombre, D13) |

Aucune donnée d'authentification dans `localStorage` ou `sessionStorage`.

---

## 10. Formulaires

- Signal Forms : modèle signal, `form()`, schéma de validation reprenant les bornes du backend (longueurs, formats) pour un retour immédiat ; le serveur reste l'arbitre.
- Les erreurs de champ renvoyées par l'API (400 `VALIDATION_FAILED`, 409 sur un champ) sont rattachées au champ concerné par `serverFieldErrors` (`shared/forms/server-errors.ts` : champs connus, et conflits dont le code désigne un champ, comme `NAME_ALREADY_USED`) ; les autres s'affichent en tête du formulaire, annoncées par une région `aria-live`.
- Soumission : le bouton garde son libellé, indique l'envoi (« Enregistrement… »), empêche la double soumission ; le focus va à la première erreur après un échec.
- Modifications non enregistrées : garde `unsavedChangesGuard` (`shared/forms/unsaved-changes.guard.ts`) sur les formulaires d'administration ; la page implémente `canLeave()` (dialogue de confirmation si la saisie a changé) et déclenche l'avertissement du navigateur (`beforeunload`) pour une fermeture ou un rechargement.
- Motif des écrans d'administration (F24) : liste en tableau, formulaire de création ou de modification sur une page séparée, suppression par dialogue de confirmation, résultat en notification ; retour à la liste après un enregistrement.

---

## 11. Administration

- Connexion par session serveur (D-CO) : `POST /api/admin/session`, lecture par `GET /api/admin/session` (200 avec `login`, sinon 401), déconnexion par `DELETE /api/admin/session` (204, filtre de Spring Security : absente de `openapi.json`, présente dans `SecurityConfiguration`).
- `canMatch` sur `/admin` (hors `/admin/login`) : la session est vérifiée une fois puis gardée dans `AdminSession` ; sans session (ou serveur injoignable), retour à la connexion avec `returnUrl`, limité aux pages `/admin/**` (`adminReturnUrl`, aucune redirection ouverte). Une 401 ultérieure de `/api/admin/` (session expirée après 30 minutes, D-CR) la vide et renvoie à la connexion avec la page en cours en `returnUrl` : intercepteur fourni par la route d'administration seulement, avant ceux de l'application (D-EP).
- Jeton CSRF : le cookie `XSRF-TOKEN` est au chemin `/` (lisible par `document.cookie` depuis `/admin/**`) et renouvelé dans la réponse de la connexion (D-EO) ; la première requête de la page de connexion (`GET /api/admin/session`) le dépose.
- Page de connexion : D-EN (F22). Cadre : `layout/admin-shell` (présentation) et `features/admin/admin-frame.ts` (session, déconnexion, `ADMIN_NAVIGATION`, une entrée par écran existant) ; tableau de bord : D-EP (F23).
- Rendu client uniquement ; le lot d'administration n'est jamais chargé par le site public (chargement paresseux, aucune importation depuis `features/<public>`).
- Aperçu d'un brouillon (D07) : même composant de rendu Markdown que le site public (`shared/markdown`).

---

## 12. Markdown

Le Markdown est la source canonique (`01-perimetre-v1.md` §9) et arrive brut de l'API. `shared/markdown` le transforme en HTML :

- rendu côté serveur pour le site public (le HTML fait partie de la page servie) ;
- GFM, titres ancrés (identifiants stables, `scroll-margin-top`), table des matières, coloration syntaxique, liens externes marqués ;
- **assainissement obligatoire** du HTML produit avant insertion (le contenu vient de l'administrateur, mais une faille XSS stockée ne doit pas dépendre de cette confiance) ;
- KaTeX et Mermaid chargés à la demande, seulement sur les pages qui en contiennent (F15) ;
- bibliothèques et sûreté : [ADR 0003](../decisions/0003-rendu-markdown.md) — `markdown-it` (HTML brut échappé, liens limités à http(s), mailto et adresses relatives), `highlight.js` (noyau et langages choisis, couleurs par les tokens), sûreté par construction sans bibliothèque d'assainissement ; `shared/markdown/markdown-renderer.ts` (moteur, synchrone, identique au serveur et au navigateur), `markdown-view.ts` (affichage, seul contournement de l'assainisseur d'Angular, qui retire les `id` des titres), `toc.ts` (identifiants de titres) ; titres du contenu décalés sous le `<h1>` de la page, ancres préfixées par le chemin de la page ; le moteur vit dans un lot chargé à la demande (≈ 58 kB transférés), jamais dans le lot initial.
- F14 : une page qui a besoin des titres (sommaire) rend le Markdown une fois et passe le résultat à la vue (`MarkdownView.rendered`) ; option `codeToolbar` (langage et bouton « Copier », activé par le conteneur `app-code-copy`) ; notes (`markdown-it-footnote`, rendu du projet) placées après le bloc qui les appelle, en marge sous `prose-margin-notes` (D-EE).

---

## 13. SEO

`core/seo` fournit un service unique (`Seo.set({ title, description, path, type, image, noindex })`) : titre « Titre — Blek Ngossanga » (ou le titre de référence « Blek Ngossanga — Software Engineering, AI Vision & Research »), `meta description` (description par défaut du site à défaut), `link rel="canonical"` absolu sans requête ni ancre, Open Graph (`og:title`, `og:description`, `og:type`, `og:url`, `og:site_name`, `og:locale`, `og:image`), `twitter:card`, `robots: noindex`. Chaque appel remplace les valeurs de la page précédente. La stratégie de titre du routeur l'appelle à chaque navigation avec les valeurs de la route (`title`, `data: { noindex: true }`) ; une page qui charge son contenu l'appelle ensuite avec les siennes (les publications fournissent `seoTitle` et `seoDescription`, D-AJ).

JSON-LD : `WebSite` posé par le shell public ; `Person`, `Article`, `BreadcrumbList` avec leurs pages. Sérialisation par `serializeJsonLd` (`<`, `>`, `&`, U+2028, U+2029 échappés : un titre saisi ne peut pas fermer la balise `<script>`).

Origine publique des adresses absolues : `SITE_URL`, variable d'environnement du serveur SSR, **obligatoire en production** (le serveur refuse de démarrer sans elle), jamais déduite de l'en-tête `Host` ; dans le navigateur, l'origine de la page. Sitemap et `robots.txt` : F33 (D15).

---

## 14. Accessibilité

Cible WCAG 2.2 AA (D17). Règles d'architecture :

- HTML sémantique d'abord (`<nav>`, `<main>`, `<article>`, `<button>`, `<a>`), un seul `<h1>` par page, hiérarchie de titres continue ;
- lien d'évitement vers `<main>`, focus déplacé vers le titre de la page après une navigation client (annonce du changement de page) ;
- tout motif interactif suit l'APG WAI-ARIA ; Angular Aria pour les motifs composites ;
- contrôle automatisé : règles d'accessibilité d'ESLint sur les gabarits (F01), axe dans les tests Playwright (F04) ;
- contrôle manuel à chaque porte de qualité : clavier seul, zoom 200 %, mouvement réduit.

---

## 15. Performance

- Budgets de `angular.json` conservés ; budget par route suivi en F34.
- Images : `NgOptimizedImage` avec `width` / `height` venant de l'API (aucun décalage de mise en page), ou `fill` dans un emplacement de taille fixe recadré par `object-fit` (portrait du profil, D-EB) ; `priority` pour l'image principale au-dessus de la ligne de flottaison seulement.
- Polices auto-hébergées, sous-ensemble latin, `font-display: swap`, préchargement de la seule police du texte courant.
- `@defer` pour tout bloc lourd non essentiel au premier affichage.
- Mesures : Core Web Vitals en laboratoire (Lighthouse, traces de performance Chrome) en F34 ; cibles : LCP < 2,5 s, INP < 200 ms, CLS < 0,1 sur mobile simulé.

---

## 16. Sécurité

- Aucune insertion de HTML non assaini (`[innerHTML]` seulement avec la sortie de `shared/markdown`) ; aucun `bypassSecurityTrust*` hors de ce module, justifié en commentaire.
- Politique de sécurité du contenu (CSP) du site : F35 (`security.autoCsp` d'Angular, en-têtes posés par le serveur SSR ou Caddy) ; `security.allowedHosts` renseigné (KI-22).
- Aucun secret dans le frontend ; configuration d'exécution du serveur SSR par variables d'environnement (`API_ORIGIN`, `PORT`).
- Liens externes : `rel="noopener noreferrer"` lorsqu'ils ouvrent un nouvel onglet (à éviter par défaut).

---

## 17. Tests

| Niveau | Outil | Fichiers | Ce qu'il prouve |
|---|---|---|---|
| unitaire | Vitest | `*.spec.ts` à côté du code | fonctions pures (conversion d'erreurs, paramètres de pagination, formatage) |
| composant | Vitest + `TestBed` | `*.spec.ts` | rendu d'un composant d'affichage selon ses entrées ; états d'une page avec `provideHttpClientTesting` ; jsdom complété par `src/test-setup.ts` (dialogue modal et `ResizeObserver` minimaux, le comportement réel étant vérifié en E2E) |
| bout en bout | Playwright Test | `e2e/*.spec.ts` | parcours réels dans un navigateur, statut HTTP SSR, accessibilité (axe), clavier |
| visuel | Playwright (captures) | `e2e/visual/*.spec.ts` | non-régression des primitives et des pages stabilisées (F36) |

- Les tests `*.spec.ts` n'ont besoin ni du backend ni de Docker (même règle que les `*Test` du backend).
- Les tests de bout en bout locaux utilisent le backend de développement (profil `dev`, données d'amorçage) ; leur exécution en CI est ajoutée en F36 avec l'environnement Compose. Dès F04, la CI lance les spécifications marquées `@no-api` (sans backend).
- Banc Playwright (`frontend/playwright.config.ts`) : projets `desktop` (1440 × 900) et `mobile` (390 × 844, tactile), serveur de développement démarré ou réutilisé (`webServer`). Toute spécification importe `test` et `expect` de `e2e/support/fixtures.ts` : la fixture automatique `guard` fait échouer le test sur une erreur ou un avertissement de console, une exception, une requête en échec ou une réponse HTTP ≥ 400 non déclarée (`guard.allowHttpError(…)`) ; `expectAccessible(page)` exige zéro violation axe (WCAG 2.0, 2.1, 2.2, A et AA). Captures et traces vont dans `frontend/test-results/` (ignoré).
- Ce qui n'est pas testé : les détails de style (couverts par les captures et la revue visuelle), les composants triviaux sans logique.

---

## 18. Décisions

| Réf | Décision | Statut |
|---|---|---|
| FA01 | Organisation par fonctionnalité (`core`, `shared`, `layout`, `features`), sans reproduction de l'architecture hexagonale du backend | Acceptée |
| FA02 | Frontières entre dossiers vérifiées par ESLint (règle locale qui résout les chemins, §5 ; `no-restricted-imports` ne détecte pas les imports relatifs) | Acceptée, mise en œuvre (F01) |
| FA03 | Lectures par `httpResource`, écritures par `HttpClient`, pas de bibliothèque d'état | Acceptée |
| FA04 | Types générés depuis `docs/api/openapi.json` par `openapi-typescript` (types seulement), contrôle de dérive en CI | Acceptée, mise en œuvre (F03) |
| FA05 | URL relatives `/api` dans le navigateur ; origine interne `API_ORIGIN` pour le rendu serveur, jamais déduite de la requête | Acceptée, mise en œuvre (F02) |
| FA06 | Site public en `RenderMode.Server` (D22), administration en `RenderMode.Client`, 404 réelle côté serveur | Acceptée, mise en œuvre (F02) |
| FA07 | Signal Forms pour tout formulaire | Acceptée |
| FA08 | Pas d'Angular Material ; plateforme web d'abord, Angular Aria et CDK à la demande | Acceptée |
| FA09 | Pas de Storybook en V1 ; catalogue interne `/_ui` en développement seulement | Acceptée |
| FA10 | Playwright Test et axe pour les tests de bout en bout et d'accessibilité | Acceptée, mise en œuvre (F04) |
| FA11 | Filtres, recherche et pagination dans l'URL | Acceptée |
| FA12 | Rendu Markdown partagé, rendu serveur, HTML sûr par construction ; bibliothèques choisies par ADR (0003) | Acceptée, mise en œuvre (F11) ; KaTeX, Mermaid et notes à venir (F14, F15) |

## 19. Points ouverts

| Sujet | Traité en |
|---|---|
| Route publique d'envoi d'un message (`POST /api/public/contact-messages`, piège à robots, limitation de débit) | F19 (backend puis frontend) |
| Filtre `featured` des projets pour l'accueil | F17 |
| Liste publique des technologies, catégories et tags (filtres) | F13, F14, si l'écran les affiche |
| Contenu de `/admin/settings` | retiré de la V1 par le propriétaire (D25, 2026-10-02) |
| Sitemap et `robots.txt` : serveur SSR ou API | F33 |
| Suppression d'un message de contact avant la mise en ligne | F30 (backend puis frontend) |
