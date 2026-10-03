# Suivi de progression

Ce fichier est la **seule** source de vérité sur l'avancement. Il ne répète pas le contenu des autres documents : il pointe vers eux.

Dernière mise à jour : 2026-10-03 (F00 à F23 poussées, CI verte ; F24 à F26 commitées, à pousser ; F27 vérifiée)

---

## 1. État courant

```text
Dernière étape terminée   : F27 — Projets (administration) et jeu de démonstration visuel (à committer) ; F24 à F26 commitées (284e518 à 9d3f2f8), à pousser ; F00 à F23 poussées, CI verte (run 36952913904)
Étape en cours            : aucune
Prochaine étape prévue    : F28 — Publications (administration)
État                      : F27 : commits proposés
Branche                   : develop
Vérification              : backend : 405 tests (unitaires et d'intégration, mvnw verify), contrat OpenAPI régénéré (filtres de la liste d'administration) ; frontend : lint, lint:styles, format, 315 tests unitaires, types de l'API régénérés, build de production (Aria dans un lot paresseux ; lot initial 402,68 kB bruts / 111,17 kB transférés) ; 171 tests de bout en bout (7 ignorés) en développement, nouveaux scénarios stables sur 5 exécutions, 155 en production derrière mandataire sur 3 exécutions ; pagination réelle (projets, articles, actualités, administration) ; administration des projets et 12 pages publiques sans violation axe ni débordement à 390 et 1440 px ; découpage des commits rejoué dans un clone jetable
```

## 2. Prochaine action

1. Committer F27 (commandes de la fin de tranche), pousser avec F24 à F26, vérifier la CI, remplacer les 🟡 par ✅.
2. Avant la mise en production : traiter KI-38 (variantes d'images), mesuré en F27 (D-EV) : 1,1 Mo d'images sur l'accueil, y compris en 390 px.
3. Renseigner `ADMIN_USERNAME` et `ADMIN_PASSWORD` dans `deploy/.env` (compte administrateur de développement, mot de passe de 15 caractères au moins) : nécessaires pour utiliser l'administration et pour `e2e/admin-auth.spec.ts`.
4. Vérification avec un lecteur d'écran (NVDA) du site public (audit F21, constat A7). Plus tard : page de mentions légales avant la mise en production (D-DW).
5. Étape 36, close (rappel du découpage) :

   ```text
   36.1  Taxonomie : catégories et tags                                    ✔ poussée
   36.2  Médias : envoi (multipart 10 Mio), liste, texte alternatif, suppression contrôlée   ✔ poussée
   36.3  Publications : liste tous statuts, création, modification (SLUG_LOCKED), statut, termes, couverture   ✔ poussée
   36.4  Séries : création, chapitres remplacés d'un bloc (NEWS_CANNOT_JOIN_SERIES, ARTICLE_ALREADY_IN_SERIES)   ✔ poussée
   36.5  Projets : technologies, projets, couverture et captures, stabilité du slug   ✔ poussée
   36.6  Profil : profil et collections, avatar et CV   ✔ poussée
   36.7  Messages de contact : liste, détail, changement de statut   ✔ poussée
   ```
6. Confirmer ou infirmer D-T (cohérence `stage` ⇔ `endDate`) : `docs/decisions/registre-implementation.md`.

---

## 3. Étapes

Légende : ✅ terminée et poussée · 🟡 terminée et vérifiée, commits à faire ou à pousser · ⏳ à faire

| Étape | Titre | Statut | Commits |
|---|---|---|---|
| 01 | Définir précisément la V1 | ✅ | `7758c17` |
| 02 | Définir le modèle métier global | ✅ | `7758c17` |
| 03 | Préparer le repository Git | ✅ | `7758c17` |
| 04 | Vérifier l'environnement de développement | ✅ | `341d601` |
| 05 | Initialiser Spring Boot | ✅ | `42bd757` |
| 06 | Initialiser Angular avec SSR et rendu hybride | ✅ | `010e4b6`, `db4bfe3` |
| 07 | Créer PostgreSQL avec Docker Compose | ✅ | `d29bdf9`, `a982b1d` |
| 08 | Connecter Spring Boot à PostgreSQL | ✅ | `27243bb` |
| 09 | Installer et configurer Flyway | ✅ | `b6b5d48`, `6a60505` |
| 10 | Créer la structure modulaire du backend | ✅ | `ffc21ac`, `4bd383b`, `8af0fd5`, `3fc3c33` |
| 11 | Définir les conventions API | ✅ | `b621da5`, `ce311ad`, `ec2b24d` |
| 12 | Créer la gestion globale des erreurs | ✅ | `4f08f8a`, `14d3ad0`, `3468adc` |
| 13 | Mettre en place l'infrastructure de tests backend | ✅ | `89011cf`, `8eb3712`, `63ee610` |
| 14.1 | Schéma PostgreSQL du profil | ✅ | `08659b9` |
| 14.2 | Entités JPA Profile et ProfessionalLink | ✅ | `1177490`, `776aae1` |
| 14.3 | Repository et service de lecture du profil | ✅ | `9633987` |
| 14.4 | DTO et PublicProfileController | ✅ | `30a582b` (+ `PublicProfileIT` ajouté à la consolidation) |
| 15.1 | Schéma et entité Skill | ✅ | `dfd61b6` |
| 15.2 | Chargement multi-collections et exposition publique | ✅ | `394597c` |
| 16.1 + 16.2 | Schéma des trois collections ; entités, ordre d'affichage et agrégat | ✅ | `0b188cd` |
| — | Consolidation du 2026-09-24 | ✅ | `4da7b57`, `ede3eb5`, `4f4c5e2`, `623e934`, `b4b3d74`, `9c7353f`, `df536aa` |
| 16.3.1 | Parcours dans le domaine, lecture en nombre constant de requêtes | ✅ | `0f65a25` |
| 16.3.2 | Exposition publique du parcours | ✅ | `c9237e7`, `e19431c` (CI : run 36005370667) |
| 17.1 | `DateRange` et pagination partagés (`shared.domain.model`) | ✅ | `d992faf`, `668d0e2` |
| 17.2 | Schéma `V004` | ✅ | `992a957` |
| 17.3 | Domaine, port, persistance, cas d'usage, seed | ✅ | `f7fb962` |
| 17.4 | API publique paginée | ✅ | `1b7ab39`, `e81f4f4` (CI : run 36042341685) |
| — | Outillage local hors dépôt (R-7) | ✅ | `6a5c7ae` |
| 18.1 | Schéma `V005` : vocabulaire et association | ✅ | `b72c9d6` |
| 18.2 | Technologies des projets, filtre `?technology=` | ✅ | `b06e5fc`, `950f6ae` (CI : run 36044315331) |
| 19.1 | Horloge applicative et schéma `V006` | ✅ | `cf1ac20` |
| 19.2 | Domaine, persistance, visibilité, cas d'usage, seed | ✅ | `2501559` |
| 19.3 | API publique des publications | ✅ | `66a3ac4`, `fe85dfe` |
| 20.1 | Module `taxonomy` : schéma `V007`, domaine, façade `TaxonomyQueryService`, seed | ✅ | `b362243`, `afb2196` |
| 20.2 | Règle ArchUnit de communication entre modules (ADR 0002) | ✅ | `959344b` |
| 20.3 | Publications classées : `V008`, filtres `?category=` / `?tag=`, `Specification` | ✅ | `c744161`, `5b5d171`, `978830e` (CI : run 36049720014) |
| 21.1 | Date obligatoire pour une publication archivée (`V009`) | ✅ | `1af1e67` |
| 21.2 | Machine à états, cas d'usage `ChangePublicationStatusUseCase` (sans route HTTP avant l'étape 36) | ✅ | `29a043d`, `417d940` (CI : run 36052194346) |
| 22.1 | Graphe des dépendances entre modules vérifié par ArchUnit (audit A04) | ✅ | `20c0f7c`, `27661c2`, `607f820` |
| 22.2 | Objet de valeur `Slug` : format, génération, collisions | ✅ | `338a042` |
| 22.3 | `Slug` dans les modèles ; première publication (`V010`) ; slug stable après publication (D11) | ✅ | `b6f257c` |
| — | Corrections documentaires de l'audit du 2026-09-25 | ✅ | `124d9cc` (CI : run 36451053100) |
| 23.1 | Schéma `V011` : séries, articles d'une série, NEWS exclues par clé étrangère composite | ✅ | `631df54` |
| 23.2 | Façade `PublicationQueryService` ; domaine, persistance, cas d'usage et seed des séries | ✅ | `6131151`, `3b1a378` |
| 23.3 | API publique des séries et table des matières | ✅ | `9834423`, `97f0568` (CI : run 36458752968) |
| 24.1 | Navigation dans le domaine : position publique, voisins visibles | ✅ | `b368ffb` |
| 24.2 | `GET /api/public/publications/{slug}/series` : navigation et progression depuis un article | ✅ | `4d974c0`, `b2fb6c4`, `262925c` (CI : run 36460488555) |
| 25 | Port `MediaStorage` (lecture), `LocalMediaStorage`, `GET /api/public/media/{storageKey}` | ✅ | `dbfe4ee`, `d1d1825` (CI : run 36461821408) |
| 26 | Envoi validé (signature, taille par format), écriture atomique ; 415 / 413 | ✅ | `3d26cf9`, `05d0e0c`, `5127e41` (CI : run 36462754639) |
| 27.1 | Catalogue `media` (`V012`), dimensions des images, envoi transactionnel | ✅ | `2e55819`, `85ac21e`, `b3e051d` (CI : run 36464422452) |
| 27.2 | Couverture et captures des projets (`V013`), façade `MediaQueryService`, suppression contrôlée | ✅ | `3266bec`, `9afc18c`, `2689282`, `0a9e707` (CI : run 36466269701) |
| 27.3 | Avatar et CV du profil (`V014`), `PublicDocument` | ✅ | `ae74c63`, `05a6e53`, `ae3820c`, `685760c` (CI : run 36467187039) |
| 27.4 | Couvertures des publications (`V015`) et des séries (`V016`) | ✅ | `4a09d87`, `31ca538`, `a024051` (CI : run 36469195120) |
| 28.1 | Configuration `french_unaccent` (`V017`) ; documents de recherche pondérés et index GIN des publications (`V018`) et des projets (`V019`) | ✅ | `a4a4465` |
| 28.2 | Recherche classée par les modules propriétaires (façades), cas d'usage `SearchPublicContentUseCase` | ✅ | `e80b3d2`, `a776fc9`, docs `de9829f` (CI : run 36559169148). Poussés dans le désordre : `e80b3d2` et `de9829f` ne compilent pas seuls (CI rouge : run 36557442643), l'arbre est correct à partir de `a776fc9` |
| 29 | `GET /api/public/search` ; `ProblemDetail` des exceptions de Spring sans corps désormais codé | ✅ | `b4a3f26`, `81ff5fc`, `45c23d8` (CI : run 36560472534) |
| 30 | Module `contact` : `V020`, cycle de statut, cas d'usage sans route, seed `dev` | ✅ | `12201b6`, `d3a767e` (CI : run 36562833516) |
| 31 | Notification de l'administrateur après enregistrement d'un message (SMTP, après validation), Mailpit en développement | ✅ | `a21346b`, `51d4ce7` (CI : run 36565149687) |
| 32 | Spring Security : routes publiques, d'administration et refusées ; refus codés (401, 403, 400) ; springdoc en `dev` seulement | ✅ | `ae5bd94`, `fa8ebd8` (CI : run 36578007365) |
| 33 | Compte administrateur unique (`V021`), initialisé depuis la configuration, empreinte bcrypt seule | ✅ | `5786680`, `a9f3e80` (CI : run 36580781605) |
| 34.1 | Connexion par session (`/api/admin/session`), CSRF d'application monopage, date de dernière connexion | ✅ | `72fda46`, `dbcbafa` (CI : run 36595141519) |
| 34.2 | Limite des essais de connexion (5 échecs par adresse en 15 minutes, 429) | ✅ | `72fda46` (commit commun avec 34.1) |
| 35 | Durcissement HTTP : attributs des cookies, en-têtes de sécurité, séparateur encodé (KI-33) | ✅ | `7e58f5e`, `4f41b01` (CI : run 36596138213) |
| 36.1 | Administration de la taxonomie (catégories, tags) | ✅ | `abc2da9`, `7690e24` (CI : run 36597416828) |
| 36.2 | Administration des médias (envoi multipart, liste, texte alternatif, suppression contrôlée) | ✅ | `b5e2bd8`, `bdb4178` (CI : run 36599044081) |
| 36.3 | Administration des publications (brouillon, saisie, slug, cycle éditorial, références vérifiées) ; contenu borné (`V022`) | ✅ | `06dfef8`, `1cdec2a` (CI : run 36602134647) |
| 36.4 | Administration des séries (saisie, slug, chapitres remplacés d'un bloc) | ✅ | `508313a`, `4ce7cf8` (CI : run 36603771411) |
| 36.5 | Administration des technologies et des projets ; mémoire de publication et description bornée (`V023`) | ✅ | `7a24c9a`, `4bad8b8` (CI : run 36605811512) |
| 36.6 | Administration du profil (remplacement complet, collections comprises) | ✅ | `3203055`, `c9b3297` (CI : run 36607262554) |
| 36.7 | Administration des messages de contact | ✅ | `77ff96b`, `23aa08b` (CI : run 36608187827) |
| P-B01 → P-B09 | Professionnalisation du backend : construction reproductible, diagnostic sûr, mandataire inverse, configuration de production explicite, cohérence des fichiers médias, journal de sécurité, contrat OpenAPI versionné, contraintes par nom structuré, Dependabot | ✅ | `aa5ed7f` … `bec418a` (CI : run 36915786497) |
| P-B10, P-B11, P-B14 | Textes de l'API en français quel que soit le client ; bcrypt de coût 12 avec mise à niveau au démarrage ; actions GitHub épinglées par empreinte | ✅ | `04fba3c`, `77acf2a`, `1f1e55d`, `9e67629` (CI : run 36915786497) |
| — | Conception du frontend : architecture (FA01 à FA12), système de design (DS01 à DS08), organisation de la documentation (R-8) | ✅ | `ebdaefc`, `cefb019`, `4ead1d3` (CI : run 36915786497) |
| F00 | Socle Angular : 22.2, `@types/node` 24, squelette nettoyé (KI-21, KI-29) ; lot initial 234,16 kB bruts / 65,43 kB transférés | ✅ | `ebbd9dc`, `b6934c9` (CI : run 36915786497) |
| F01 | ESLint (Angular, accessibilité des gabarits, frontières par règle locale), Prettier, CI (D-DN) | ✅ | `9e59f7c` (CI : run 36915786497) |
| F02 | HTTP, routeur, titre, rendu par route, `API_ORIGIN` en SSR, proxy, 404 réelle (D-DO) ; lot initial 258,59 kB bruts / 73,10 kB transférés | ✅ | `a243c20` (CI : run 36915786497) |
| F03 | Types de l'API générés, `ApiError`, pagination, contrôle de dérive en CI (D-DP) | ✅ | `3f29de6` (CI : run 36915786497) |
| F04 | Playwright, axe, garde console et réseau, E2E de la 404, job CI sans API, CLI Playwright (D-DQ) | ✅ | `429a01a`, `bf1e41e`, `e240f0f` (documentation F00 à F04) (CI : run 36915786497) |
| F05 | Direction visuelle : « Planche technique » choisie par le propriétaire parmi trois prototypes (hors dépôt, captures de 320 à 1920 px, zoom 200 %, axe sans violation) ; notes marginales de « Monographie » pour les articles ; DS01 acceptée (D-DR) | ✅ | `b5f2222` (documentation) (CI : run 36915786497) |
| F06 | Tokens (`@theme`, thème par défaut retiré), polices auto-hébergées, styles de base, catalogue `/_ui`, `lint:styles` en CI (D-DS) ; CSS global 25,97 kB bruts / 5,02 kB transférés | ✅ | `5a71e88`, `9210757`, `9d37575` (CI : run 36915786497) |
| F07 | Utilitaires `page-container`, `stack-*`, `cluster-*` (D-DT) | ✅ | `5a71e88` (CI : run 36915786497) |
| F08 | Bouton (`button[appButton]`, `a[appButton]`), icônes (`menu`, `close`), styles de lien ; retour de pression seul mouvement (D-DU) ; lot initial 298,36 kB bruts / 82,22 kB transférés | ✅ | `b4c0e5a` (CI : run 36915786497) |
| F09 | Shell public : en-tête « Blek Ngossanga » et signature, navigation (vide tant qu'aucune page n'existe), pied de page, lien d'évitement, focus après navigation, états vide et erreur, 404 stylée (D-DV, D-DW) ; lot initial 333,30 kB bruts / 92,02 kB transférés | ✅ | `9d9a518`, docs `77ab4bd` (CI : run 36915786497) |
| F10 | Service SEO : titre, description, canonical absolu, Open Graph, robots, JSON-LD `WebSite` ; `SITE_URL` exigée au démarrage du serveur de production (D-DX) | ✅ | `9d9a518` (CI : run 36915786497) |
| F11 | Rendu Markdown : ADR 0003 (`markdown-it`, `highlight.js`, sûreté par construction), moteur, affichage, styles `prose`, spécimen `/_ui/prose` (D-DY) ; moteur dans un lot paresseux (58,35 kB transférés), lot initial 350,77 kB bruts / 93,99 kB transférés | ✅ | `815c2aa`, `c99fe60` (CI : run 36915786497) |
| KI-34 | Contrat OpenAPI des réponses dérivé du code : propriétés `required`, `null` seulement pour les composantes `@Nullable` (JSpecify) ; types frontend régénérés (D-DZ) | ✅ | `485c12d` (CI : run 36915786497) |
| F12 | Page À propos : profil rendu par le serveur, aucun appel rejoué par le navigateur (cache de transfert corrigé : `no-store` de Spring Security, clé relative, transport serveur `ServerApiBackend`), états (non publié 404, erreur 503 et « Réessayer », chargement), `ProfilePage` en JSON-LD, lien « À propos » ; `publicEmail` jamais affiché (D-EA, D-EB) ; lot initial 370,34 kB bruts / 101,80 kB transférés (+ `resource` / `httpResource` d'Angular), page dans un lot paresseux de 4,52 kB transférés | ✅ | `ee02d08`, `3555e4b`, `ff140d8`, docs `44bb72c` ; revue visuelle `frontend-design` : `fef2dba` (CI : run 36915786497) |
| KI-37 | Contrôleur de test hors du contrat OpenAPI : `@TestComponent` (exclu de l'analyse des composants), importé par le seul `GlobalExceptionHandlerTest` ; contrat et types régénérés (170 et 419 lignes retirées) | ✅ | `927c5c7` (CI : run 36915786497) |
| F13 | Projets : registre paginé filtré par technologie (URL `?page=`, `?technology=`), détail en planche (fiche, couverture, Markdown, figures), vraies 404 (slug inconnu, page au-delà de la dernière), `CreativeWork`, lien « Projets » (D-ED) ; vérifié : 130 tests unitaires, E2E dév. et production derrière mandataire, HTML serveur sans appel rejoué, axe sans violation de 320 à 1920 px et à 640 / 720 px sur 13 états ; lot initial 371,18 kB bruts / 101,95 kB transférés, pages en lots paresseux (liste 3,09 kB, détail 2,84 kB transférés) | ✅ | `e9cb714`, `0bc7380`, docs `c86abe3` (CI : run 36915786497) |
| F14 | Publications : `/articles`, `/news` (registres paginés, filtres catégorie et tag dans l'URL), page d'article en planche (sommaire en marge ou repliable, fiche, notes marginales, code avec « Copier »), redirection 301 vers le bon type, vraies 404, `Article` / `NewsArticle` (D-EE) ; vérifié : 146 tests unitaires, 59 tests de bout en bout (1 ignoré), production derrière mandataire (49), HTML serveur sans appel rejoué, axe sans violation de 320 à 1920 px et à 640 / 720 px ; lot initial 375,41 kB bruts / 102,69 kB transférés, moteur Markdown paresseux 58,76 kB | ✅ | `1b15f09`, `0bc7380`, docs `c86abe3` (CI : run 36915786497) |
| F15 | Formules (KaTeX au serveur, transmises : aucun JavaScript de KaTeX au premier affichage) et diagrammes (Mermaid 11.17.2 à l'approche, `strict`, titre et description accessibles), feuille de style de KaTeX sur les seules pages concernées, publication de démonstration ajoutée au seed `dev` même dans une base existante (D-EF) ; vérifié : 158 tests unitaires, E2E (8), lots paresseux mesurés sur le build de production ; lot initial 378,36 kB bruts / 103,49 kB transférés | ✅ | `b7e9032`, `c66cdd2`, docs `b5b1e34` (CI : run 36915786497) |
| F16 | Séries : `/series`, `/series/:slug` (table des chapitres, fiche, `CreativeWorkSeries`), bloc de série de l'article (contexte, précédent / suivant avec `rel`), absence de série transmise par le rendu serveur (aucun appel rejoué), grille de registre commune `styles/register.css` (D-EG) ; vérifié : 167 tests unitaires, 75 tests de bout en bout (1 ignoré) stables sur 4 exécutions complètes, axe sans violation de 320 à 1920 px et à 640 / 720 px | ✅ | `29fb26a`, `d1deb36`, `88febb4`, docs `b5b1e34` (CI : run 36915786497) |
| F17 | Accueil : filtre `featured` des projets (backend, tests unitaires et d'intégration, contrat, types), page `/` (énoncé, cartouche, zones projets, articles, séries, actualités ; six requêtes parallèles reprises du cache de transfert), nom du site lié à l'accueil, liens professionnels sur l'accueil (D-EH) | ✅ | `72b785c`, `47405ad`, `d777661`, `88febb4`, docs `b5b1e34` (CI : run 36915786497) |
| F18 | Recherche : lien « Recherche » dans la navigation, `/search` (formulaire `GET` natif, rendu serveur des résultats, navigation interne dans le navigateur, résultats mixtes vers la page de leur type, pagination, nombre annoncé, aucun résultat, requête vide sans appel, 201 caractères refusés avant l'API, `noindex`) (D-EI) | ✅ | `df1ad88`, `88febb4`, docs `b5b1e34` (CI : run 36915786497) |
| F19 | Contact : route publique `POST /api/public/contact-messages` (validation, piège à robots, 5 messages par heure et par adresse, 202, notification existante ; fenêtre glissante partagée avec la connexion), contrat et types ; primitives `field` et `alert`, contrôles natifs ; page `/contact` (Signal Forms, erreurs du serveur rattachées, focus, 429, succès focalisé), lien « Contact », zone Contact de l'accueil (D-EJ, D-EK) ; mention sur l'usage des données validée par le propriétaire (2026-10-02) | ✅ | `4b9bbaa`, `ab377c3`, `f5d8f7b`, `35369d4`, `3b4c567`, `3bcf65e`, `faa83f3`, `fe8e47f`, docs `252be22` (CI : run 36948186371) |
| F20 | Mouvement du site public (choix délégués par le propriétaire) : ouverture de la navigation mobile (200 ms), pression du bouton « Copier » (100 ms), entrée des messages d'issue (200 ms) ; neuf candidats refusés ; rien au chargement ni sous mouvement réduit ; tests de mouvement (D-EL) | ✅ | `4063a0d`, docs `252be22` (CI : run 36948186371) |
| F21 | Audit public : axe sur 21 états et 4 pages en erreur, clavier, 320 à 1920 px, captures comparées, revue d'interface ; corrections typographiques et `autocomplete` ; rapport `audits/2026-10-02-audit-frontend-public.md` (D-EM) | ✅ | `d1b1362`, `2da7edc`, docs `252be22` (CI : run 36948186371) |
| F22 | Authentification d'administration : `/admin/login`, session (`AdminSession`), garde, `returnUrl` limité à l'administration, déconnexion, accueil minimal ; jeton CSRF lisible et renouvelé (backend) ; intercepteur des 401 reporté à F23 (D-EN, D-EO) | ✅ | `98dc531`, `f1c682a`, `282db16`, docs `252be22` (CI : run 36948186371) |
| F23 | Shell d'administration (en-tête, barre latérale repliée en menu sous 64 rem, lien d'évitement) et tableau de bord (relevé : messages non lus, publications, projets, séries, médias, lus dans les listes existantes) ; session expirée pendant le travail → connexion avec `returnUrl` ; `/admin/settings` retiré de la V1 (D25) ; brouillons reportés à F28 (D-EP) | ✅ | `7f162fa`, `c75762e`, docs `d66883b` (CI : run 36952913904) |
| F24 | Taxonomie (une entrée, trois sous-sections : catégories, tags, technologies) et primitives d'administration : tableau, dialogue de confirmation, notifications, variante `danger`, erreurs du serveur sur les champs, garde des modifications non enregistrées (D-EQ) | 🟡 | `284e518`, `11b9b05`, `0bb21d3`, docs `9c2642d` (à pousser) |
| F25 | Médiathèque : envoi (bouton ou dépôt, plusieurs fichiers, progression, vérification locale puis du serveur), tableau paginé, texte alternatif exigé pour une image, suppression contrôlée ; sélecteur de médias reporté à F26 (D-ER) | 🟡 | `cb4fb5a`, `0bb21d3`, docs `9c2642d` (à pousser) |
| F26 | Profil : un formulaire et un enregistrement (identité, présentation Markdown avec aperçu, avatar et CV par le sélecteur de médias, cinq collections ordonnables au clavier ou au glisser-déposer), erreurs du serveur jusque dans les collections ; `@angular/cdk` ajouté ; KI-36 résolu (D-ES, D-ET) | 🟡 | `ff2940e`, `87f2d51`, `4bde69f`, `a6f2112`, `002e281`, `1be65f3`, `b4d8054`, docs `9d3f2f8` (à pousser) |
| F27 | Projets (administration) : liste filtrée (visibilité, technologie) et paginée, création, modification complète (slug figé, période, technologies par choix multiple Angular Aria, couverture, captures ordonnables) ; jeu de démonstration visuel du profil `dev` (médias réels, 24 projets, 26 publications, profil complet) ; pages publiques revues avec ces données (figures légendées, image prioritaire) ; KI-38 mesuré (D-EU, D-EV, D-EW) | 🟡 | à committer |
| F28 → F37 | Frontend : voir `docs/frontend/00-roadmap-frontend.md` (remplace les étapes 37 à 51 et les parties frontend de 52) | ⏳ | — |
| 52 | Déploiement : voir `docs/steps/liste_complete_etapes.md` (52.7 à 52.21) | ⏳ | — |

Après le push, remplacer les 🟡 par ✅ et noter les hashes.

---

## 4. Décisions

Index : [`decisions/README.md`](decisions/README.md).

| Réf | Sujet | Statut |
|---|---|---|
| ADR 0001 | Architecture interne des modules (ports et adaptateurs légers) | Acceptée (2026-09-24) ; travail induit n° 6 (`shared.domain.model`) fait à l'étape 17 |
| ADR 0002 | Communication entre modules : identifiants et façades de lecture | Acceptée (2026-09-24, étape 20) |
| R-1 … R-8 | Organisation du dépôt et outillage (docs non versionnées, profils Spring, OSIV, CI, TypeScript strict, compose, outillage local hors dépôt, documentation du frontend) | Actives |
| FA01 … FA12 | Architecture frontend : fonctionnalités, frontières ESLint, `httpResource`, types générés, origine de l'API en SSR, rendu par route, Signal Forms, pas de Material ni de Storybook, Playwright et axe, état dans l'URL, Markdown partagé | Acceptées (2026-09-30) ; FA02, FA04, FA05, FA06, FA10 mises en œuvre (F01 à F04) |
| D-DM … D-DY | Frontend : montée 22.2 et hôtes SSR en local ; frontières par règle ESLint qui résout les chemins ; intercepteur commun et jeton serveur ; types générés et `overrides` de TypeScript ; banc Playwright ; direction visuelle ; tokens et `lint:styles` ; utilitaires ; primitives d'action ; shell public et identité ; mentions légales reportées ; SEO ; rendu Markdown | Actives (F00 à F11) ; D-DO modifiée en F12 (D-EB) |
| D-EQ, D-ER | Taxonomie et primitives d'administration (décisions du propriétaire : tableaux, formulaires séparés, suppression confirmée, notifications, entrée « Taxonomie ») ; médiathèque | Actives (F24, F25) |
| D-EU, D-EV, D-EW | Jeu de démonstration visuel du profil `dev` (règle : pas de validation visuelle sans médias quand le modèle en prévoit) ; corrections des pages publiques révélées par ce jeu ; administration des projets (filtres d'API, pastille de statut, choix multiple Angular Aria) | Actives (F27) |
| D-ES, D-ET | Réponses d'administration séparées des requêtes dans le contrat (KI-36) ; profil, liste ordonnable (CDK), sélecteur de médias | Actives (F26) |
| D25, D-EP | `/admin/settings` retiré de la V1 (décision du propriétaire) ; shell d'administration, relevé du tableau de bord, intercepteur des sessions expirées | Actives (F23) |
| D-EL … D-EO | Mouvement du site public (trois mouvements, refus consignés) ; audit public ; authentification d'administration (session, garde, `returnUrl`, intercepteur en F23) ; jeton CSRF au chemin `/` et renouvelé à la connexion | Actives (F20 à F22) |
| D-DZ … D-EK | Contrat des réponses dérivé du code (`required`, `@Nullable`) ; `publicEmail` jamais affiché (décision du propriétaire) ; page À propos, cache de transfert et transport serveur ; pied de page sans appel au profil (liens professionnels sur l'accueil et À propos) ; projets (filtre par clic, URL, SEO des vues, couvertures 3:2) ; publications (redirection du mauvais type, filtres, page d'article, notes, Copier) ; formules et diagrammes, seed additif ; séries, absence de série transmise, registre commun ; accueil et filtre `featured` ; recherche ; contact public (piège, limite, 202) et formulaire | Actives (KI-34, F12 à F19) |
| ADR 0003 | Rendu du Markdown : `markdown-it`, `highlight.js`, sûreté par construction | Acceptée (2026-09-30, F11) |
| DS01 … DS08 | Système de design : direction « Planche technique » (F05), tokens Tailwind 4, polices auto-hébergées, stratégie de style, primitives à la demande, mouvement, thème clair, typographie française | Acceptées (2026-09-30) ; DS01 choisie par le propriétaire en F05 |
| D-O, D-Q | Lecture en 1 + 5 requêtes constantes ; invariants de dates au domaine | Actives (16.3.1) |
| D-P, D-R | Aplatissement de `DateRange` dans le JSON ; contrat public sans `id`/`displayOrder` | Actives (16.3.2) |
| D-S, D-U, D-V, D-W, D-X, D-Y | `DateRange` partagé ; visibilité publique ; pagination sans type Spring dans les ports ; représentations publiques des projets ; slug ; périmètre de `V004` | Actives (17) |
| D-T | `stage` ⇔ absence de `endDate` (invariant 21) | Active — **à confirmer** |
| D-Z … D-AE | `Technology` agrégat du module `project` ; unicités ; chargement par lot ; filtre `?technology=` ; médias des projets à l'étape 27 ; pas de liste publique des technologies avant l'étape 42 | Actives (18) |
| D-AF … D-AM | Périmètre de `publication` ; horloge applicative ; visibilité à `now` ; invariant 24 ; contrat public (SEO, temps de lecture) ; ordre et `?type=` ; slug commun ; dates d'audit par l'application | Actives (19) |
| D-AN … D-AT | Communication entre modules (ADR 0002) ; unicité des vocabulaires ; contrat `category`/`tags` ; filtres par slug ; `Specification` ; 5 requêtes par page ; pas de route publique des termes avant l'étape 43 | Actives (20) |
| D-AU … D-AY | Cycle de vie sans route HTTP avant l'étape 36 ; table des transitions sur le statut effectif ; 409 `INVALID_PUBLICATION_TRANSITION` ; écriture limitée au statut ; concordance règle Java / règle SQL | Actives (21) |
| D-AZ … D-BD | Mémoire de la première publication (`V010`) ; objet de valeur `Slug` ; slug mal formé → 404 sans requête ; slug verrouillé après publication (409 `SLUG_LOCKED`) ; unicité par suffixe puis `UNIQUE` (appelants à l'étape 36) | Actives (22) |
| D-BE … D-BK | Périmètre de `series` (lecture seule avant l'étape 36) ; invariants 1 à 3 en SQL, NEWS exclue par clé composite ; série publique si un article est visible, positions publiques par rang ; façade `PublicationQueryService` ; contrat public ; 5 et 4 requêtes ; slug stable à l'étape 36 | Actives (23) |
| D-BL … D-BN | Navigation servie par `series` sous `/publications/{slug}/series` ; voisins et position parmi les visibles, calculés par le domaine ; 6 requêtes | Actives (24) |
| D-BO … D-BQ | Port `MediaStorage` livré avec son premier appelant (écart assumé avec la liste des étapes) ; clé opaque et lecture publique en cache immuable ; cas d'usage sans base non transactionnel | Actives (25) |
| D-BR … D-BT | Format par signature, 5 / 10 Mio, lecture bornée, écriture atomique ; 415 et 413 (`MEDIA_TOO_LARGE`) ; suppression à l'étape 27 | Actives (26) ; D-BT remplacée en partie par D-BU |
| D-BU | Catalogue `media`, dimensions lues dans l'en-tête, envoi transactionnel sans compensation | Active (27.1) |
| D-BV, D-BW | Références aux médias par clé étrangère `RESTRICT`, suppression contrôlée ; `PublicImage` par la façade, couverture et captures des projets | Actives (27.2) |
| D-BX | Avatar et CV du profil ; `PublicDocument` pour les PDF | Active (27.3) |
| D-BY | Couvertures des publications et des séries | Active (27.4) |
| D-BZ … D-CB | Configuration `french_unaccent` ; document de recherche généré et index GIN dans chaque table propriétaire ; noms des tags et des technologies recopiés par PostgreSQL (invariant 28) | Actives (28.1) |
| D-CC, D-CD | Recherche classée derrière les façades, règle de visibilité non réécrite ; module `search` sans table, classement commun et pagination | Actives (28.2) |
| D-CE, D-CF | Route et contrat de la recherche publique ; `q` obligatoire, vide → page vide, 200 caractères au plus (`VALIDATION_FAILED`) ; code ajouté aux erreurs de Spring sans corps | Actives (29) |
| D-CG, D-CH | Module `contact` sans route (envoi public à l'étape 48, administration à l'étape 36) ; règles du message doublées en SQL ; cycle en avant seulement (invariant 29, `INVALID_CONTACT_MESSAGE_TRANSITION`) | Actives (30) |
| D-CS | Administration de la taxonomie : conventions des routes d'administration, slug généré, conservé ou suffixé dans la longueur de sa colonne, `NAME_ALREADY_USED`, `TERM_STILL_USED`, contraintes traduites | Active (36.1) |
| D-CT | Administration des médias : envoi multipart, liste paginée des plus récents, texte alternatif seul modifiable, suppression contrôlée ; requête bornée à 11 Mio (413 `MEDIA_TOO_LARGE`), reste lu par Tomcat jusqu'à 12 Mio | Active (36.2) |
| D-CU | Administration des publications : brouillon à la création, type fixé, saisie remplacée (slug conservé, suffixé ou `SLUG_LOCKED`), route du statut, pas de suppression ; contenu borné à 100 000 caractères (`V022`) ; références vérifiées (400 sur le champ, `InvalidInputException`) | Active (36.3) |
| D-CV | Administration des séries : série créée sans chapitre, saisie remplacée (slug conservé, suffixé ou `SLUG_LOCKED`), chapitres remplacés d'un bloc et renumérotés, `ARTICLE_ALREADY_IN_SERIES` au lieu de `SERIES_POSITION_ALREADY_USED`, pas de suppression | Active (36.4) |
| D-CW, D-CX | Administration des technologies (sur le modèle des tags) et des projets : toute la saisie en une requête, visibilité comprise ; mémoire de publication `ever_published` (`V023`) et slug figé ensuite ; période, adresses http(s) et références vérifiées sur leur champ ; description bornée à 100 000 caractères ; pas de suppression | Active (36.5) |
| D-CY | Administration du profil : ressource unique, `PUT` qui crée ou remplace tout le profil, collections comprises, dans l'ordre saisi ; périodes, compétences (casse ignorée), avatar et CV vérifiés sur leur champ ; suppressions envoyées avant les insertions | Active (36.6) |
| D-CZ | Administration des messages : boîte de réception triée et filtrable par statut, lecture sans effet, statut avancé par sa route (invariant 29), pas de suppression | Active (36.7) |
| D-CR | Cookies `Secure` et `SameSite=Strict`, session de 30 minutes ; `frame-ancestors 'none'` et `Referrer-Policy: no-referrer` en plus des en-têtes par défaut ; `%2F` confié au pare-feu de Spring Security | Active (35) |
| D-CO … D-CQ | Connexion par session, refus uniforme, identifiant de session renouvelé, garde des 72 octets de bcrypt ; CSRF par cookie réservé à l'administration (avancé de l'étape 35) ; 5 échecs par adresse en 15 minutes, sans verrouillage du compte | Actives (34) |
| D-CM, D-CN | Compte administrateur unique (invariant 30), empreinte bcrypt seule en SQL (invariant 15) ; configuration qui fait foi à chaque démarrage, mot de passe de 15 caractères à 72 octets | Actives (33) |
| D-CK, D-CL | Séparation des routes (public, administration authentifiée, reste refusé), aucun utilisateur généré, aucune session anonyme, springdoc en `dev` ; refus de sécurité rendus par `GlobalExceptionHandler` (`AUTHENTICATION_REQUIRED`, `ACCESS_DENIED`, `MALFORMED_REQUEST`) | Actives (32) |
| D-CI, D-CJ | Port `ContactNotificationSender`, courriel SMTP à l'administrateur (`Reply-To` visiteur, sujet sur une ligne) ; envoi après validation, échec journalisé sans donnée personnelle ; santé SMTP hors de l'état de l'application ; GreenMail en test, Mailpit en développement | Actives (31) |
| D-DJ … D-DL | Textes de l'API en français (locale fixe, `messages.properties`) ; bcrypt 12 ; actions épinglées par empreinte | Actives (P-B10, P-B11, P-B14) |
| D-DA … D-DI | Professionnalisation du backend : Enforcer et agent Mockito ; `X-Request-Id` et journal d'erreur sans message ; en-têtes du mandataire depuis un réseau de confiance ; aucune valeur de développement hors `dev` ; fichiers médias alignés sur la transaction ; événements de sécurité et écritures d'administration journalisés, JSON ECS facultatif ; contrat OpenAPI versionné ; nom structuré des contraintes ; Dependabot | Actives (P-B01 à P-B09) |
| 14.4 | Exposition publique de `publicEmail` | Tranchée le 2026-09-30 : champ gardé dans le contrat, jamais affiché ni lié en `mailto:` (D-EA) |

## 5. Problèmes connus

Audit d'origine : [`audits/2026-09-24-audit-avant-16.3.md`](audits/2026-09-24-audit-avant-16.3.md). Contrôle de dérive refait avant l'étape 17 (voir « Résolus »).

### Ouverts

| ID | Priorité | Problème | Traitement prévu |
|---|---|---|---|
| KI-18 | AMÉLIORATION | `LICENSE` : titulaire et année non renseignés (`[year] [fullname]`) | à décider par le propriétaire du dépôt |
| KI-22 | AMÉLIORATION | SSR : `security.allowedHosts` vide ; depuis Angular 22.2, le build SSR refuse alors **tout** hôte (400), `localhost` compris : obligatoire avant la mise en production ; en local, `NG_ALLOWED_HOSTS=localhost` (D-DM) | F35 |
| KI-16 | OPTIONNEL | `V001` : virgule manquante avant `CONSTRAINT profile_single_row` (comportement identique) ; migration appliquée, non modifiable | aucun |
| KI-38 | **PRIORITAIRE** (avant la mise en production) | Médias servis en une seule taille (`GET /api/public/media/…`) : aucune variante pour `srcset`. Mesuré en F27 avec le jeu de démonstration (originaux de 1 500 px) : accueil 6 images, 1 120 ko, liste des projets 756 ko, articles 903 ko, mêmes poids à 390 px ; vignettes de 64 px de la médiathèque chargées en taille d'origine (`NG0913`, `NG02960` en développement) | F34 (performance) : variantes produites à l'envoi (backend) ou service de redimensionnement, puis `ngSrcset` / chargeur d'images (D-EV) |
| KI-35 | OPTIONNEL | `npm ci` avertit que quatre paquets de la chaîne de build (`esbuild`, `lmdb`, `msgpackr-extract`, `@parcel/watcher`) ont des scripts d'installation non listés dans `allowScripts` (npm 11) ; sans effet sur l'installation | décider d'une liste `allowScripts` explicite, ou ignorer |
| KI-30 | OPTIONNEL | Les `*IT` transactionnels (écriture de statut, lectures publiques) ne prouvent pas le comportement après commit | à reprendre si un défaut de transaction apparaît |

### Résolus le 2026-10-02 (F26)

| ID | Problème | Résolution |
|---|---|---|
| KI-36 | Contrat OpenAPI : les éléments du profil d'administration et `Screenshot`, partagés entre requête et réponse, gardaient la sémantique de la requête (propriétés facultatives omissibles) | records de réponse distincts (`AdminLink` … `AdminCertification`) et `AdminScreenshot` : contrat régénéré, `OpenApiContractIT` (D-ES) |

### Résolus le 2026-09-30 (socle frontend)

| ID | Problème | Résolution |
|---|---|---|
| KI-37 | Routes `/test-errors/**` et schéma `SampleRequest` du contrôleur de test dans `docs/api/openapi.json` et les types frontend | corrigé à la source : `ErrorHandlingTestController` annoté `@TestComponent`, donc exclu de l'analyse des composants de tout contexte de test ; `GlobalExceptionHandlerTest` l'importe ; `OpenApiContractIT.the_contract_describes_no_test_route` ; contrat et types régénérés |
| KI-34 | Schémas de réponse sans `required` ni `null` : types frontend tous optionnels, `endDate` nullable indistinct | corrigé à la source (D-DZ) : `ResponsePropertiesRequired` (propriétés des schémas atteints depuis une réponse), `NullableRecordComponents` (`@Nullable` de JSpecify → type `null` ajouté, ou `oneOf` avec `null` pour un objet), 21 records annotés ; contrat et types régénérés, `Required<>` retiré de `Page<T>` ; `NullableRecordComponentsTest`, `ResponsePropertiesRequiredTest`, `OpenApiContractIT` |
| KI-21 | `@types/node ^20` alors que Node 24 est la cible | F00 : `@types/node ^24` (24.19.0) |
| KI-29 | `App.title` sans appelant (frontend) ; côté backend déjà résolu (D-CV, D-CY) | F00 : squelette réduit à `<router-outlet />` |

### Résolus le 2026-09-29 (étape 36.3)

| ID | Problème | Résolution |
|---|---|---|
| KI-31 | Un `tsvector` est limité à 1 Mio : un contenu Markdown démesuré aurait été refusé par PostgreSQL à l'écriture (500) | contenu borné à 100 000 caractères par le domaine, le DTO (400) et `V022` (D-CU), comme la description des projets (`V023`, D-CX), `PublicationTest`, `PublicationSchemaIT`, `AdminPublicationIT` |

### Résolus le 2026-09-29 (étape 35)

| ID | Problème | Résolution |
|---|---|---|
| KI-33 | Un `%2F` était rejeté par Tomcat en page HTML, pas en `ProblemDetail` codé | transmis au pare-feu de Spring Security, 400 `MALFORMED_REQUEST` (D-CR), `HttpServerSecurityIT` |

### Résolus le 2026-09-29 (étape 32)

| ID | Problème | Résolution |
|---|---|---|
| KI-19 | springdoc exposait `/api/v3/api-docs` et Swagger UI sans décision documentée | désactivés par défaut, activés en profil `dev` seulement (D-CK), vérifié par `SecurityConfigurationIT` |
| KI-20 | Une `AccessDeniedException` levée par un contrôleur devenait une 500 (gestionnaire `Exception`) | 403 `ACCESS_DENIED` (D-CL), `GlobalExceptionHandlerTest` |

### Résolus le 2026-09-29 (étape 29)

| ID | Problème | Résolution |
|---|---|---|
| KI-32 | Une exception de Spring transmise sans corps (paramètre obligatoire absent) produisait un `ProblemDetail` sans `code` stable, contrairement à `05` §10 | `GlobalExceptionHandler.handleExceptionInternal` construit le corps avant d'ajouter le code (D-CF), `GlobalExceptionHandlerTest` |
| — | Historique : `e80b3d2` et `de9829f` (étape 28) poussés avant les commits dont ils dépendent | constaté, non réécrit (pas de poussée forcée) ; arbre correct depuis `a776fc9` |

### Résolus le 2026-09-28 (étape 26)

| ID | Problème | Résolution |
|---|---|---|
| KI-23 | `UNSUPPORTED_MEDIA_FORMAT` prévu en 409 | 415, et `MEDIA_TOO_LARGE` en 413 (D-BS) |

### Résolus le 2026-09-28 (audit du 2026-09-25, étape 22)

| ID | Problème | Résolution |
|---|---|---|
| A01 | `publishedAt` ne suffisait pas à savoir si une publication avait été publique (`PUBLISHED → ARCHIVED → DRAFT → SCHEDULED → DRAFT` l'effaçait) | 22.3 : `first_published_at` (`V010`), `Publication.hasBeenPublic` (D-AZ) |
| A02 | `progress.md` demandait de committer l'étape 21 déjà poussée | §1 à §3 alignés sur le dépôt |
| A03 | `02` §30 présentait la table des transitions comme garantie par PostgreSQL | §30 : invariants 17 à 25 en SQL, 26 et 6 applicatifs ; D-AX précise la confiance du port en son unique appelant |
| A04 | Graphe des dépendances du `04` §4 non vérifié couple par couple | 22.1 : règle ArchUnit du graphe, validée par mutation |
| A05, A06, A08, A11 | Exemples de `05` contradictoires ou obsolètes (§16, §17, §29, §34) ; formulation de D-AW | exemples et formulations alignés sur le code |
| A07 | Versions de JUnit et Testcontainers non à jour | `README.md`, `06` §15 |
| A10 | Fichiers d'outillage local non ignorés | `.gitignore` (R-7) |
| — | SQL journalisé deux fois dans les tests (`show_sql` et logger) | `application-test.yaml` : logger seul |

### Résolus le 2026-09-24 (consolidation, 16.3 à 21)

| ID | Problème | Résolution |
|---|---|---|
| KI-01 | 16.2 partiellement commitée, HEAD non compilable ; `new ProfileEntity(...)` laissait les collections à `null` (`@Builder.Default`) → NPE au seed `dev` | entités réécrites (collections initialisées, fermées) ; commit unique de 16.1/16.2 |
| KI-02 | Architecture réelle ≠ documentation | ADR 0001 acceptée ; `04`, `05`, template d'étape et fiche 16.3 alignés |
| KI-03 | Bouchons silencieux dans `ProfileRepositoryAdapter` | port réduit à `find()` / `save()` |
| KI-04 | `toEntity` perdait `aboutMarkdown`, `publicLocation`, `publicEmail` | mapper complet + `ProfilePersistenceMapperTest` |
| KI-05 | `*Test` nécessitant Docker | `ActuatorHealthIT`, `GetProfileUseCaseIT` |
| KI-06 | Profil `dev` actif par défaut | retiré (R-2) |
| KI-07 | Aucun test HTTP de bout en bout | `PublicProfileIT` |
| KI-08 | `open-in-view` désactivé seulement en test | configuration commune (R-3) |
| KI-09 | Import de `deploy/.env` dépendant du répertoire de travail | deux imports optionnels |
| KI-10 | Pas de CI | `.github/workflows/ci.yml` (R-4) |
| KI-12 | TypeScript non strict | `strict` + `strictTemplates` (R-5) |
| KI-13 | `lang="en"`, titre « Frontend » | `lang="fr"`, « Portfolio » |
| KI-14 | Règles ArchUnit de couches absentes | `ModuleLayersTest` |
| KI-15 | Versionnement de `docs/steps/`, `docs/prompts/` | décision : non versionnés (R-1) |
| KI-11 | Invariants de dates dans la couche JPA | 16.3.1 : `DateRange`, `Certification` dans `domain.model` (D-Q) |
| KI-17 | Contrat public incohérent (`id`/`displayOrder` exposés) | 16.3.2 : D-R appliquée à toutes les collections |
| KI-24 | `ApiPaging.MAX_PAGE_SIZE` documenté (`05` §33) mais appliqué nulle part à la résolution HTTP | 17 : `spring.data.web.pageable.max-page-size: 100`, vérifié par `PublicProjectControllerTest` (D-V) |
| KI-25 | `PageResponse.from(Page)` inutilisable : un port ne peut pas renvoyer de type Spring (ADR 0001) | 17 : `PageResult` / `PageQuery` dans `shared.domain.model`, `PageResponse.from(PageResult)` (D-V) |
| KI-26 | `05-conventions-api.md` §18 décrivait encore `project/api/` | 17 : exemple aligné sur l'ADR 0001 |
| KI-27 | `progress.md` indiquait 16.3 « à pousser » alors que les commits étaient poussés et la CI verte | 17 : statuts et hashes mis à jour |
| KI-28 | Outillage local du poste non encadré (risque de versionner des fichiers personnels) | R-7 : ignoré par `.gitignore`, hors dépôt |
| — | Test frontend rouge ; `compose.dev.yml` ≠ documentation ; docs obsolètes ; `pom.xml` et `.gitignore` à nettoyer | corrigés |

## 6. Décisions reportées

| Sujet | Reporté à | Raison |
|---|---|---|
| Liste publique des technologies (`/api/public/technologies`) | Étape 42 | D-AE : les technologies sont exposées par projet |
| Tri choisi par le client (`sort`) | quand un écran le demande | D-V : ordre fixe, paramètre ignoré |
| Rejet explicite (400) des paramètres de pagination hors bornes, au lieu de les ramener aux bornes | si un client en a besoin | comportement Spring Data retenu (D-V) |
| Verrouillage optimiste des saisies d'administration (`@Version`, `version` dans les `PUT`) | décision du propriétaire (contrat d'API) | D-AX : un seul administrateur en V1, mais deux onglets peuvent s'écraser (dernier `PUT` gagnant) |
| Routes publiques `/api/public/categories` et `/api/public/tags` | Étape 43 | D-AT : les termes sont exposés par publication |
| Création et modification avec slug libre (`firstAvailable`), `SLUG_ALREADY_USED` sur conflit `UNIQUE` | fait (36.1 à 36.5) | D-BD : fait pour les termes (D-CS), les publications (D-CU) et les séries (D-CV) |
| Suppression d'un brouillon de publication | si le besoin apparaît | D-CU : l'archivage retire une publication du site |
| Suppression d'un projet | si le besoin apparaît | D-CX : l'archivage retire un projet du site |
| Suppression d'une série | si le besoin apparaît | D-CV : une série sans article visible n'apparaît pas sur le site |
| Séries mises en avant sur l'accueil | si le modèle l'adopte | D-BI : absent de `02` §19 |
| Médias privés (fichier visible seulement si le contenu qui l'utilise l'est) | si le besoin apparaît | D-BP : clé imprévisible jugée suffisante |
| Temps de lecture précalculé (colonne) si la liste devient coûteuse | Étape 52.6 | D-AJ : calculé à la lecture, contenu chargé dans la liste |
| Prérendu route par route | Étape 52.1 | D22 |
| Cache du profil public | si le profil devient un chemin chaud | D-O |
| Suppression d'un message de contact (données personnelles, à la demande de la personne) | avant la mise en ligne (étape 52) | D-CZ : l'archivage range un message, il ne l'efface pas |
| Restaurer un message archivé, remettre un message « non lu » | si l'administration le demande | D-CH : cycle en avant seulement |
| Notification asynchrone (hors du fil de la requête) | Étape 48, si la latence de la route publique le justifie | D-CJ : envoi synchrone borné à 5 s par délai SMTP |
| Argon2id au lieu de bcrypt | si une dépendance BouncyCastle devient acceptable | D-CN : l'encodeur délégué permet la migration (empreintes préfixées, `upgradeEncoding`) |
| Politique de sécurité du contenu (CSP complète) du site | Étapes 37 et 52 | D-CR : l'API n'envoie que `frame-ancestors 'none'` |
| Liens professionnels (GitHub, CV) dans le pied de page, lus dans le profil | F17 au plus tard | D-EC : décision du propriétaire (2026-09-30) : pied de page minimal, aucun appel global au profil depuis le shell, aucun lien inventé |
| Politique de cache HTTP des réponses publiques de l'API (`Cache-Control` au lieu du `no-store` de Spring Security) | F34 / étape 52 | D-EB : le cache de transfert est ouvert explicitement aux GET de `/api/public/` |
| Nombre de brouillons sur le tableau de bord (filtre de statut de la liste d'administration des publications) | F28 | D-EP : la liste n'a pas de filtre de statut ; il viendra avec son écran |
| Sélecteur de médias (dialogue réutilisé par le profil, les projets, les publications et les séries) | fait (F26) | D-ER, D-ET : `app-media-picker` |
| Primitives `select` et case à cocher | fait (F27) | D-EW : contrôles natifs de `styles/controls.css` |
| Page de mentions légales (éditeur, hébergeur) | avant la mise en production publique, une fois l'hébergement connu | D-DW : décision du propriétaire (2026-09-30) ; aucun lien provisoire d'ici là |
| HSTS derrière le mandataire inverse (requête vue en HTTP par Tomcat) | fait (P-B03) | D-DC : `X-Forwarded-Proto` accepté depuis un réseau de confiance |
| Adresse réelle du client derrière le mandataire inverse (en-têtes de transfert) pour la limite des essais | fait (P-B03) | D-DC : `X-Forwarded-For` accepté depuis un réseau de confiance ; à vérifier avec le réseau Docker réel à l'étape 52 |
| Limite des essais partagée entre plusieurs instances | si l'application passe à plusieurs instances | D-CQ : compteur en mémoire |
| Désactiver le compte (`enabled`) | si l'administration le demande | D-CM : colonne présente (`02` §26), toujours vraie |
| Configuration SMTP de production (authentification, STARTTLS) | Étape 52 | D-CI : `MAIL_*` lues, sécurité du transport à fixer avec le fournisseur |
| Filtre de la recherche par genre de contenu (`type`), extraits mis en évidence (`ts_headline`), recherche par préfixe pendant la saisie | si l'écran de recherche le demande (étape 47) | D-CE ; D-CC : `websearch_to_tsquery` n'accepte pas `:*` |
| Catégorie dans le document de recherche | si le modèle l'adopte | D-CA : absente de `01` §11 |
| Rapprochement des fichiers médias orphelins (arrêt brutal entre l'écriture et la fin de la transaction) | si un orphelin est constaté | D-DE : un rollback ou une suppression validée gardent déjà la cohérence |
| `build-info`, point `info`, métriques et traces exportées | si une supervision est mise en place | D-DF : version présente au démarrage et dans les journaux JSON |
| Comparaison de compatibilité du contrat (OpenAPI Diff) | si un client externe apparaît | D-DG : le diff de `docs/api/openapi.json` est relu |
| CodeQL, SBOM (CycloneDX) | Étape 52 | D-DI |
| Contrôle de la longueur de ligne (120, `.editorconfig`) à la construction | si un formateur est adopté | D-DL : 124 lignes à replier dans 78 fichiers |
| Protection des branches `main` et `develop`, alertes Dependabot | paramètres du dépôt (propriétaire) | D-DI, D-DL |
| Classement en base (fenêtre de pagination SQL) au lieu du classement en mémoire de tous les résultats | Étape 52.6, si le corpus dépasse quelques milliers de contenus | D-CD |

---

## 7. Mettre à jour ce fichier

À la **fin de chaque sous-étape**, dans le même commit que le code (ou juste après, en `docs(progress): …`) :

1. §1 : dernière étape terminée, prochaine étape, état, vérifications ;
2. §2 : la prochaine action concrète ;
3. §3 : statut et hash(es) de la sous-étape ;
4. §4 : décisions nouvelles (le détail va dans `decisions/`, ici seulement la référence) ;
5. §5 : ajouter les problèmes découverts, déplacer les résolus (avec le commit).

Une étape n'est ✅ que si : code, tests et documentation commités, `./mvnw verify` (et/ou `npm test`, `npm run build`) vert, commit poussé, CI verte, arbre de travail propre.
