# Portfolio

Portfolio professionnel full-stack : Spring Boot, Angular (SSR), PostgreSQL, Docker.

## État

En construction — V1. Avancement détaillé, étape courante et problèmes connus : [docs/progress.md](docs/progress.md).

## Stack

| Couche | Technologies |
|---|---|
| Backend | Java 25, Spring Boot 4.1 (Web MVC, Data JPA, Validation, Actuator), Flyway, PostgreSQL 18 |
| Frontend | Angular 22 (SSR / rendu hybride), Tailwind CSS 4, Vitest, Playwright et axe (bout en bout, accessibilité), ESLint |
| Tests backend | JUnit 6, AssertJ, MockMvc, Testcontainers 2 (PostgreSQL réel), ArchUnit |
| Infrastructure | Docker Compose ; en production : Caddy, HTTPS, VPS (à venir) |

Versions exactes et justification : [docs/03-versions-cibles.md](docs/03-versions-cibles.md).

## Structure

| Dossier | Contenu |
|---|---|
| `backend/` | API Spring Boot — monolithe modulaire ([architecture](docs/04-architecture-backend.md)) |
| `frontend/` | Application Angular SSR |
| `deploy/` | Docker Compose de développement, modèles de variables d'environnement |
| `docs/` | Documentation du projet (index ci-dessous) |
| `scripts/` | Hooks Git (`install-hooks.sh`, validation des messages de commit) |

## Prérequis

| Outil | Version |
|---|---|
| JDK | 25 (Temurin, voir `.sdkmanrc`) |
| Maven | aucun : utiliser le wrapper `backend/mvnw` |
| Node.js | 24 LTS (voir `.nvmrc`) |
| npm | 11 |
| Docker + Compose v2 | récent ; indispensable pour PostgreSQL local **et** pour les tests d'intégration (Testcontainers) |

Aucun PostgreSQL ni client `psql` local n'est nécessaire : `psql` s'utilise depuis le conteneur.

## Démarrage local

### 1. Hooks Git (une fois)

```bash
sh scripts/install-hooks.sh
```

### 2. Variables d'environnement

```bash
cp deploy/.env.example deploy/.env
# renseigner POSTGRES_PASSWORD (le fichier deploy/.env n'est jamais commité)
```

### 3. PostgreSQL et SMTP de développement

```bash
docker compose -f deploy/compose.dev.yaml up -d
docker compose -f deploy/compose.dev.yaml exec postgres psql -U portfolio -d portfolio
```

Le compose démarre aussi Mailpit, serveur SMTP de développement qui reçoit les notifications de contact sans rien envoyer : interface sur `http://localhost:8025`. Sans variable `MAIL_*`, le backend lui écrit (`localhost:1025`). Un serveur SMTP absent n'empêche ni le démarrage ni l'enregistrement d'un message : l'échec est journalisé.

### 4. Backend

Aucun profil Spring n'est actif par défaut : activer `dev` explicitement. Le profil `dev` lit `deploy/.env` depuis la racine du dépôt ou depuis `backend/`. Les fichiers de médias sont stockés dans `uploads/` sous le répertoire de travail (ignoré par Git) ; `MEDIA_STORAGE_ROOT` désigne un autre répertoire.

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# Windows : .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Dans IntelliJ : configuration d'exécution `Application` → *Active profiles* : `dev`.

- API : `http://localhost:8080/api/public/profile`, `http://localhost:8080/api/public/projects`, `http://localhost:8080/api/public/projects/{slug}`
- Santé : `http://localhost:8080/api/actuator/health`
- Filtre par technologie : `http://localhost:8080/api/public/projects?technology=java`
- Publications : `http://localhost:8080/api/public/publications` (filtres cumulables `?type=ARTICLE|NEWS`, `?category=backend`, `?tag=java`), `http://localhost:8080/api/public/publications/{slug}`
- Recherche : `http://localhost:8080/api/public/search?q=angular`
- Documentation OpenAPI (profil `dev` seulement) : `http://localhost:8080/api/swagger-ui/index.html`
- Administration (`/api/admin/**`) : session ouverte par `POST /api/admin/session` (`{ "login", "password" }`, en-tête `X-XSRF-TOKEN` recopiant le cookie `XSRF-TOKEN` déposé par une première requête `GET /api/admin/session`). Le compte administrateur est créé ou mis à jour à chaque démarrage depuis `ADMIN_USERNAME` et `ADMIN_PASSWORD` (`deploy/.env`, facultatifs : les deux ou aucun ; mot de passe de 15 caractères au moins)
- En profil `dev`, `ProfileSeeder`, `ProjectSeeder`, `TaxonomySeeder` et `PublicationSeeder` créent des données de démonstration si la base n'en contient pas (technologies, projets dont un brouillon, catégories et tags, une publication par cas de visibilité).

### 5. Frontend

```bash
cd frontend
npm ci
npm start                       # http://localhost:4200, appels /api transmis au backend 8080
```

Un autre backend : `API_ORIGIN=http://localhost:8081 npm start`. Détails (proxy, build SSR, tests de bout en bout) : [frontend/README.md](frontend/README.md).

## Tests

```bash
cd backend
./mvnw test      # tests rapides (*Test) — ne doit pas nécessiter Docker
./mvnw verify    # + tests d'intégration (*IT) sur PostgreSQL Testcontainers — référence avant commit

cd frontend
npm run lint
npm test -- --watch=false
npm run build
npm run e2e:no-api   # bout en bout sans backend (Chromium : npx playwright install chromium)
```

Stratégie complète : [docs/06-strategie-tests.md](docs/06-strategie-tests.md).

## Intégration continue

`.github/workflows/ci.yml` exécute `./mvnw verify` (JDK 25, Testcontainers) et, côté frontend (Node 24), `npm ci`, lint, formatage, contrôle des types de l'API contre `docs/api/openapi.json`, `npm test`, `npm run build`, puis les tests de bout en bout sans backend (Chromium) à chaque push ou pull request sur `develop` et `main`.

## Documentation

| Document | Rôle |
|---|---|
| [progress.md](docs/progress.md) | étape courante, historique, problèmes connus, prochaine action |
| [01-perimetre-v1.md](docs/01-perimetre-v1.md) | périmètre fonctionnel V1 et décisions D01–D24 |
| [02-modele-metier.md](docs/02-modele-metier.md) | modèle métier conceptuel et invariants |
| [03-versions-cibles.md](docs/03-versions-cibles.md) | versions des outils et dépendances |
| [04-architecture-backend.md](docs/04-architecture-backend.md) | modules, dépendances, structure interne |
| [05-conventions-api.md](docs/05-conventions-api.md) | contrat REST, erreurs RFC 9457, pagination |
| [06-strategie-tests.md](docs/06-strategie-tests.md) | pyramide de tests, Surefire/Failsafe, Testcontainers |
| [conventions-git.md](docs/conventions-git.md) | branches, Conventional Commits, clôture d'étape |
| [decisions/](docs/decisions/README.md) | index des décisions, ADR, registre d'implémentation |
| [audits/](docs/audits/) | audits ponctuels (instantanés datés, non maintenus) |

## Licence

Voir [LICENSE](LICENSE).
