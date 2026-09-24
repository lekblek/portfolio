# Suivi de progression

Ce fichier est la **seule** source de vérité sur l'avancement. Il ne répète pas le contenu des autres documents : il pointe vers eux.

Dernière mise à jour : 2026-09-24 (étape 20 implémentée, commits à faire ; étape 19 commitée, push en attente)

---

## 1. État courant

```text
Dernière étape terminée   : 18 — Technologies des projets (poussée, CI verte : run 36044315331)
Étapes en attente         : 19 — Publication (commitée localement, 4 commits NON poussés)
                            20 — Catégories et tags (implémentée et vérifiée, à committer)
Prochaine étape prévue    : 21 — Implémenter le cycle de vie éditorial (transitions de statut)
État                      : PRÊT dès que les commits de 19 et 20 sont poussés et la CI verte
Branche                   : develop
Vérification              : ./mvnw clean verify → 166 tests verts (69 *Test, 97 *IT) ; démarrage dev vérifié
                            (V007 et V008 appliquées, seed de la taxonomie idempotent, ?category=, ?tag=, termes résolus)
```

## 2. Prochaine action

1. Committer l'étape 20 (6 commits, commandes fournies avec l'étape), puis pousser **19 et 20 ensemble** (`git push origin develop`), vérifier la CI, remplacer les 🟡 par ✅.
2. Confirmer ou infirmer D-T (cohérence `stage` ⇔ `endDate`) : `docs/decisions/registre-implementation.md`.
3. Étape 21 : transitions de statut (écriture, administration) ; l'API d'administration n'existant pas encore (étape 36), décider en section 0 si 21 livre le domaine seul ou attend la sécurité (32-35).

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
| 19.1 | Horloge applicative et schéma `V006` | 🟡 | `cf1ac20` (non poussé) |
| 19.2 | Domaine, persistance, visibilité, cas d'usage, seed | 🟡 | `2501559` (non poussé) |
| 19.3 | API publique des publications | 🟡 | `66a3ac4`, `fe85dfe` (non poussés) |
| 20.1 | Module `taxonomy` : schéma `V007`, domaine, façade `TaxonomyQueryService`, seed | 🟡 | à committer |
| 20.2 | Règle ArchUnit de communication entre modules (ADR 0002) | 🟡 | à committer |
| 20.3 | Publications classées : `V008`, filtres `?category=` / `?tag=`, `Specification` | 🟡 | à committer |
| 21 → 52 | Voir `docs/steps/liste_complete_etapes.md` | ⏳ | — |

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
| KI-23 | AMÉLIORATION | `UNSUPPORTED_MEDIA_FORMAT` mappé en 409 (415 ou 400 plus juste) | étape 27 |
| KI-16 | OPTIONNEL | `V001` : virgule manquante avant `CONSTRAINT profile_single_row` (comportement identique) ; migration appliquée, non modifiable | aucun |

### Résolus le 2026-09-24 (consolidation, 16.3 à 20)

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
| Couverture et captures des projets ; avatar et CV du profil | Étape 27 | catalogue `Media` requis (D-AD, D-B) |
| Liste publique des technologies (`/api/public/technologies`) | Étape 42 | D-AE : les technologies sont exposées par projet |
| Objet de valeur `Slug`, génération et collisions | Étape 22 | D-X : seules les contraintes SQL existent |
| Tri choisi par le client (`sort`) | quand un écran le demande | D-V : ordre fixe, paramètre ignoré |
| Rejet explicite (400) des paramètres de pagination hors bornes, au lieu de les ramener aux bornes | si un client en a besoin | comportement Spring Data retenu (D-V) |
| Transitions de statut des publications (`POST /api/admin/publications/{id}/status`, 409 `INVALID_PUBLICATION_TRANSITION`) | Étape 21 | D-AF : la lecture est faite, l'écriture non |
| Routes publiques `/api/public/categories` et `/api/public/tags` | Étape 43 | D-AT : les termes sont exposés par publication |
| Stabilité du slug après la première publication (D11) | Étape 22 | D-AL |
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
