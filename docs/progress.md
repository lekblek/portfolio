# Suivi de progression

Ce fichier est la **seule** source de vérité sur l'avancement. Il ne répète pas le contenu des autres documents : il pointe vers eux.

Dernière mise à jour : 2026-09-28 (27.2 poussée, CI verte ; 27.3 terminée et vérifiée, commits à faire)

---

## 1. État courant

```text
Dernière étape terminée   : 26 — Envoi et validation des médias (poussée, CI verte : run 36462754639)
Étape en cours            : 27 — Catalogue Media : 27.1 et 27.2 poussées (CI verte : run 36466269701) ; 27.3 vérifiée, commits à faire ; 27.4 à faire
Prochaine étape prévue    : 27.4 — Couvertures des publications et des séries
État                      : PRÊT après commit et push de 27.3
Branche                   : develop
Vérification              : ./mvnw clean verify → 429 tests verts (231 *Test, 198 *IT) à la fin de 27.3
```

## 2. Prochaine action

1. Committer et pousser 27.3, vérifier la CI, puis remplacer les 🟡 par ✅.
2. 27.4 : couvertures des publications (D-AF) et des séries (D-BE), références `ON DELETE RESTRICT` comme D-BV, `cover` dans leurs contrats publics (listes et détails) ; clôture de l'étape 27.
3. Confirmer ou infirmer D-T (cohérence `stage` ⇔ `endDate`) : `docs/decisions/registre-implementation.md`.

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
| 27.3 | Avatar et CV du profil (`V014`), `PublicDocument` | 🟡 | à committer |
| 27.4 | Couvertures des publications et des séries | ⏳ | — |
| 28 → 52 | Voir `docs/steps/liste_complete_etapes.md` | ⏳ | — |

Après le push, remplacer les 🟡 par ✅ et noter les hashes.

---

## 4. Décisions

Index : [`decisions/README.md`](decisions/README.md).

| Réf | Sujet | Statut |
|---|---|---|
| ADR 0001 | Architecture interne des modules (ports et adaptateurs légers) | Acceptée (2026-09-24) ; travail induit n° 6 (`shared.domain.model`) fait à l'étape 17 |
| ADR 0002 | Communication entre modules : identifiants et façades de lecture | Acceptée (2026-09-24, étape 20) |
| R-1 … R-7 | Organisation du dépôt et outillage (docs non versionnées, profils Spring, OSIV, CI, TypeScript strict, compose, outillage local hors dépôt) | Actives |
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
| 14.4 | Exposition publique de `publicEmail` | À confirmer |

## 5. Problèmes connus

Audit d'origine : [`audits/2026-09-24-audit-avant-16.3.md`](audits/2026-09-24-audit-avant-16.3.md). Contrôle de dérive refait avant l'étape 17 (voir « Résolus »).

### Ouverts

| ID | Priorité | Problème | Traitement prévu |
|---|---|---|---|
| KI-18 | AMÉLIORATION | `LICENSE` : titulaire et année non renseignés (`[year] [fullname]`) | à décider par le propriétaire du dépôt |
| KI-19 | AMÉLIORATION | springdoc expose `/api/v3/api-docs` et Swagger UI sans décision documentée | étape 32 |
| KI-20 | AMÉLIORATION | `GlobalExceptionHandler` : le gestionnaire `Exception` interceptera `AccessDeniedException` (→ 500) | étape 32 |
| KI-21 | AMÉLIORATION | `@types/node ^20` alors que Node 24 est la cible | étape 37 |
| KI-22 | AMÉLIORATION | SSR : `security.allowedHosts` vide | étape 52 (déploiement) |
| KI-16 | OPTIONNEL | `V001` : virgule manquante avant `CONSTRAINT profile_single_row` (comportement identique) ; migration appliquée, non modifiable | aucun |
| KI-29 | OPTIONNEL | Surface sans appelant : `ProfileEntity.removeLink` / `removeSkill`, `App.title` (frontend) ; codes d'erreur des séries et médias déclarés d'avance | au plus tard avec l'administration du profil (étape 36) et le frontend (37) |
| KI-30 | OPTIONNEL | Les `*IT` transactionnels (écriture de statut, lectures publiques) ne prouvent pas le comportement après commit | à reprendre si un défaut de transaction apparaît |

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
| Filtre `featured` sur `GET /api/public/projects` | Étape 40 | aucun écran ne l'utilise encore (le filtre `technology` est fait : D-AC) |
| Couvertures des publications et des séries | Étape 27.4 | D-AF, D-BE |
| Vérifier qu'une couverture ou une capture est une image à l'écriture | Étape 36 | D-BV : pas de garantie SQL ; la façade n'expose que des images |
| Liste publique des technologies (`/api/public/technologies`) | Étape 42 | D-AE : les technologies sont exposées par projet |
| Tri choisi par le client (`sort`) | quand un écran le demande | D-V : ordre fixe, paramètre ignoré |
| Rejet explicite (400) des paramètres de pagination hors bornes, au lieu de les ramener aux bornes | si un client en a besoin | comportement Spring Data retenu (D-V) |
| Route `POST /api/admin/publications/{id}/status` | Étape 36 | D-AU : cas d'usage prêt ; aucune route d'administration sans authentification (32-35) |
| Verrouillage optimiste des publications (`@Version`) | si plusieurs administrateurs | D-AX : un seul administrateur en V1 |
| Routes publiques `/api/public/categories` et `/api/public/tags` | Étape 43 | D-AT : les termes sont exposés par publication |
| Stabilité du slug d'un projet après sa première publication (D11) | Étape 36 | D-BC : pas encore de mémoire de publication des projets ni de modification |
| Création et modification avec slug libre (`firstAvailable`), `SLUG_ALREADY_USED` sur conflit `UNIQUE` | Étape 36 | D-BD : aucun appelant avant l'administration |
| Écriture des séries (création, chapitres, `NEWS_CANNOT_JOIN_SERIES`, `SERIES_POSITION_ALREADY_USED`) et stabilité de leur slug | Étape 36 | D-BE, D-BK : aucune route d'administration sans authentification |
| Séries mises en avant sur l'accueil | si le modèle l'adopte | D-BI : absent de `02` §19 |
| Route d'envoi et limite multipart à 10 Mio | Étape 36 | D-BR, D-BS : aucune route d'administration sans authentification |
| Médias privés (fichier visible seulement si le contenu qui l'utilise l'est) | si le besoin apparaît | D-BP : clé imprévisible jugée suffisante |
| Temps de lecture précalculé (colonne) si la liste devient coûteuse | Étape 52.6 | D-AJ : calculé à la lecture, contenu chargé dans la liste |
| Prérendu route par route | Étape 52.1 | D22 |
| Cache du profil public | si le profil devient un chemin chaud | D-O |
| Exposition de springdoc / Swagger UI | Étape 32 | KI-19 |

---

## 7. Mettre à jour ce fichier

À la **fin de chaque sous-étape**, dans le même commit que le code (ou juste après, en `docs(progress): …`) :

1. §1 : dernière étape terminée, prochaine étape, état, vérifications ;
2. §2 : la prochaine action concrète ;
3. §3 : statut et hash(es) de la sous-étape ;
4. §4 : décisions nouvelles (le détail va dans `decisions/`, ici seulement la référence) ;
5. §5 : ajouter les problèmes découverts, déplacer les résolus (avec le commit).

Une étape n'est ✅ que si : code, tests et documentation commités, `./mvnw verify` (et/ou `npm test`, `npm run build`) vert, commit poussé, CI verte, arbre de travail propre.
