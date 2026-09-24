# Portfolio

Portfolio professionnel full-stack : Spring Boot, Angular (SSR), PostgreSQL, Docker.

## État

En construction — V1. Avancement détaillé, étape courante et problèmes connus : [docs/progress.md](docs/progress.md).

## Stack

| Couche | Technologies |
|---|---|
| Backend | Java 25, Spring Boot 4.1 (Web MVC, Data JPA, Validation, Actuator), Flyway, PostgreSQL 18 |
| Frontend | Angular 22 (SSR / rendu hybride), Tailwind CSS 4, Vitest |
| Tests backend | JUnit 5, AssertJ, MockMvc, Testcontainers (PostgreSQL réel), ArchUnit |
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

### 3. PostgreSQL

```bash
docker compose -f deploy/compose.dev.yaml up -d
docker compose -f deploy/compose.dev.yaml exec postgres psql -U portfolio -d portfolio
```

### 4. Backend

Aucun profil Spring n'est actif par défaut : activer `dev` explicitement. Le profil `dev` lit `deploy/.env` depuis la racine du dépôt ou depuis `backend/`.

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# Windows : .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Dans IntelliJ : configuration d'exécution `Application` → *Active profiles* : `dev`.

- API : `http://localhost:8080/api/public/profile`, `http://localhost:8080/api/public/projects`, `http://localhost:8080/api/public/projects/{slug}`
- Santé : `http://localhost:8080/api/actuator/health`
- Filtre par technologie : `http://localhost:8080/api/public/projects?technology=java`
- Publications : `http://localhost:8080/api/public/publications` (`?type=ARTICLE` ou `NEWS`), `http://localhost:8080/api/public/publications/{slug}`
- En profil `dev`, `ProfileSeeder`, `ProjectSeeder` et `PublicationSeeder` créent des données de démonstration si la base n'en contient pas (technologies, projets dont un brouillon, une publication par cas de visibilité).

### 5. Frontend

```bash
cd frontend
npm ci
npm start                       # http://localhost:4200
```

## Tests

```bash
cd backend
./mvnw test      # tests rapides (*Test) — ne doit pas nécessiter Docker
./mvnw verify    # + tests d'intégration (*IT) sur PostgreSQL Testcontainers — référence avant commit

cd frontend
npm test -- --watch=false
npm run build
```

Stratégie complète : [docs/06-strategie-tests.md](docs/06-strategie-tests.md).

## Intégration continue

`.github/workflows/ci.yml` exécute `./mvnw verify` (JDK 25, Testcontainers) et `npm ci`, `npm test`, `npm run build` (Node 24) à chaque push ou pull request sur `develop` et `main`.

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
