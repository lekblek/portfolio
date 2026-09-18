# Portfolio

Portfolio professionnel full-stack : Spring Boot, Angular (SSR), PostgreSQL, Docker.

## État

En construction — V1.

## Documentation

- [Périmètre V1](docs/01-perimetre-v1.md)
- [Modèle métier](docs/02-modele-metier.md)
- [Conventions Git](docs/conventions-git.md)

## Structure

| Dossier | Contenu |
|---|---|
| `backend/` | API Spring Boot |
| `frontend/` | Application Angular |
| `deploy/` | Docker Compose, Caddy, variables d'environnement |
| `docs/` | Documentation du projet |
| `scripts/` | Scripts utilitaires |

## Prérequis

| Outil | Version |
|---|---|
| Java (JDK) | 25.0.4.1-tem (LTS) |
| Maven | via `./mvnw` |
| Node.js | 24.x (≥ 24.15.0) |
| Angular CLI | 22.x |
| Docker + Compose v2 | récent |
| PostgreSQL (client) | 18 |

Versions détaillées et justifiées : [docs/03-versions-cibles.md](docs/03-versions-cibles.md)
