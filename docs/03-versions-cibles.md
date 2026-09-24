# Versions cibles — Portfolio V1

## 1. Objectif

Ce document définit les **versions techniques de référence** utilisées pour développer, tester et déployer la V1 du Portfolio.

Il permet de :

* rendre l'environnement reproductible ;
* éviter les différences inutiles entre machines ;
* utiliser des versions compatibles entre elles ;
* distinguer les outils installés sur la machine des versions réellement imposées par le projet ;
* éviter les dépendances implicites à une installation locale de PostgreSQL ;
* faciliter la CI/CD et le futur déploiement Docker.

Ce document constitue la référence lorsqu'une étape du tutoriel demande quelle version d'un outil ou d'une technologie utiliser.

---

# 2. Principes de versionnement

Le projet applique les règles suivantes.

## 2.1 Versions applicatives contrôlées par le projet

Les technologies directement utilisées par l'application sont explicitement versionnées.

Exemples :

```text
Java
Spring Boot
Maven Wrapper
Angular
PostgreSQL
```

---

## 2.2 Versions des outils système

Les outils tels que :

```text
Git
Docker
Docker Compose
Node.js
npm
```

ne nécessitent pas toujours exactement le même patch sur toutes les machines.

Une version compatible et maintenue suffit, sauf contrainte spécifique du projet.

---

## 2.3 Pas de dépendance au Maven global

Le projet utilise :

```text
Maven Wrapper
```

Une fois le backend initialisé, les commandes normales utiliseront :

```bash
./mvnw
```

ou sous Windows :

```powershell
.\mvnw.cmd
```

plutôt que :

```bash
mvn
```

La version Maven utilisée par le projet est donc contrôlée par le repository.

---

## 2.4 Pas de PostgreSQL installé localement

PostgreSQL n'est **pas installé directement sur Windows**.

Le projet utilise exclusivement PostgreSQL dans un conteneur Docker pour le développement local.

Architecture locale :

```text
Spring Boot
     │
     ▼
localhost:<port publié>
     │
     ▼
Docker
     │
     ▼
PostgreSQL 18
```

Le client `psql` local n'est donc pas un prérequis.

Lorsque `psql` est nécessaire, le client fourni par le conteneur PostgreSQL est utilisé.

Exemple :

```bash
docker compose -f deploy/compose.dev.yaml exec postgres psql -U portfolio -d portfolio
```

Le service s'appelle `postgres` ; l'utilisateur et la base viennent de `deploy/.env` (modèle : `deploy/.env.example`).

---

# 3. Versions cibles principales

| Technologie               |                  Version cible | Politique                               |
| ------------------------- | -----------------------------: | --------------------------------------- |
| Java                      |                         25 LTS | version Java de référence du backend    |
| Spring Boot               |                          4.1.1 | version stable de référence             |
| Maven                     |                         3.9.16 | utilisée via Maven Wrapper              |
| Maven Wrapper             |                          3.3.4 | version du wrapper                      |
| Node.js                   |                         24 LTS | branche LTS                             |
| npm                       |                           11.x | version compatible fournie avec Node 24 |
| Angular                   |                         22.1.x | branche stable Angular 22               |
| Angular CLI               |                         22.1.x | alignée sur Angular                     |
| TypeScript                | version imposée par Angular 22 | ne pas choisir indépendamment           |
| PostgreSQL                |                             18 | version majeure de référence            |
| Image PostgreSQL initiale |                           18.6 | version mineure initialement épinglée   |
| Docker Engine             |                           29.x | version moderne compatible              |
| Docker Compose            |                            5.x | plugin `docker compose`                 |
| Git                       |                        >= 2.45 | suffisant pour le workflow du projet    |

---

# 4. Backend

## 4.1 Java

Version cible :

```text
Java 25 LTS
```

Le projet compile et s'exécute avec Java 25.

Java 25 est retenu comme version de référence afin de disposer d'une version LTS adaptée à un projet maintenu dans la durée.

Le `pom.xml` devra donc définir Java 25.

Exemple conceptuel :

```xml
<properties>
    <java.version>25</java.version>
</properties>
```

Cette valeur sera ajoutée lors de l'initialisation réelle du backend.

---

## 4.2 Spring Boot

Version cible initiale :

```text
Spring Boot 4.1.1
```

Spring Boot gérera lui-même les versions compatibles d'une grande partie de son écosystème.

Il ne faut donc pas fixer arbitrairement des versions indépendantes pour :

```text
Spring Framework
Hibernate
Jackson
Tomcat
Jakarta Validation
Spring Data
```

lorsqu'elles sont déjà administrées par le dependency management de Spring Boot.

Principe :

```text
Spring Boot BOM
       ↓
versions compatibles des dépendances Spring
```

Cela réduit les incompatibilités artificielles.

---

# 5. Maven

## 5.1 Maven Wrapper

Le repository doit contenir :

```text
mvnw
mvnw.cmd
.mvn/
```

Version cible Maven :

```text
3.9.16
```

Version Maven Wrapper :

```text
3.3.4
```

Le Maven installé globalement sur la machine n'est utilisé que pour les opérations initiales éventuelles.

Ensuite :

```text
Maven du système
        ≠
Maven de référence du projet
```

La référence est le Wrapper.

---

# 6. Frontend

## 6.1 Node.js

Version cible :

```text
Node.js 24 LTS
```

Il n'est pas nécessaire de verrouiller le projet sur un patch précis de Node.js.

La branche :

```text
24.x LTS
```

est utilisée.

---

## 6.2 npm

Version cible :

```text
npm 11.x
```

Le projet conservera :

```text
package-lock.json
```

dans Git.

Le lockfile permettra de reproduire précisément l'arbre des dépendances JavaScript installé par :

```bash
npm ci
```

notamment dans la CI.

---

# 7. Angular

Version majeure :

```text
Angular 22
```

Version stable retenue pour l'initialisation :

```text
22.1.x
```

Angular CLI doit appartenir à la même branche.

```text
Angular       22.1.x
Angular CLI   22.1.x
```

Il n'est pas nécessaire que le CLI Angular installé globalement corresponde exactement à cette version.

L'initialisation pourra utiliser explicitement la version voulue.

Exemple :

```bash
npx @angular/cli@22.1.8 new frontend
```

Le projet dépend ensuite du CLI local enregistré dans :

```text
package.json
package-lock.json
```

---

# 8. TypeScript

La version de TypeScript ne doit pas être choisie indépendamment.

Angular impose une plage de versions TypeScript compatible.

Principe :

```text
Angular
   ↓
contraintes TypeScript
   ↓
version installée par npm
```

Il faut donc éviter une mise à jour manuelle de TypeScript vers une version non supportée par Angular.

---

# 9. Angular Material

Angular Material est utilisé uniquement lorsqu'il apporte une valeur réelle au projet.

Lorsqu'il est installé, sa version majeure doit rester alignée sur Angular.

Exemple :

```text
Angular           22.x
Angular Material  22.x
Angular CDK       22.x
```

Il ne faut pas mélanger plusieurs versions majeures Angular.

---

# 10. PostgreSQL

> Section complétée le 2026-09-24 : le fichier était tronqué à cet endroit.

## 10.1 Version majeure

```text
PostgreSQL 18
```

## 10.2 Image épinglée

La même image est utilisée partout :

| Usage | Fichier | Image |
|---|---|---|
| Développement local | `deploy/compose.dev.yaml` | `postgres:18.6` |
| Tests d'intégration | `backend/src/test/java/.../testsupport/ContainersConfiguration.java` (`POSTGRES_IMAGE`) | `postgres:18.6` |
| Production | `deploy/compose.yaml` (étape 52.9) | à aligner |

`FlywayMigrationIT` vérifie que la base de test est en version 18 ou supérieure.

## 10.3 Montée de version

Une montée de version mineure (`18.x`) modifie les trois emplacements ci-dessus dans le même commit, puis `./mvnw verify`.

Une montée de version majeure exige en plus une procédure de migration des données du volume (`pg_upgrade` ou dump/restore) : elle fera l'objet d'un ADR.

## 10.4 Extensions

`V000__init_schema.sql` installe `unaccent` (recherche plein texte, étape 28). En production, l'utilisateur applicatif doit avoir le droit de créer l'extension, ou l'extension doit être créée au provisionnement.

---

# 11. Dépendances notables effectivement installées

Relevé du 2026-09-24 (`backend/pom.xml`, `frontend/package-lock.json`).

## 11.1 Backend

| Dépendance | Version | Gestion |
|---|---|---|
| Spring Boot (parent) | 4.1.1 | BOM |
| Starters : webmvc, data-jpa, validation, actuator, flyway | BOM | BOM |
| `flyway-database-postgresql` | BOM | BOM |
| Driver PostgreSQL | BOM | BOM |
| Lombok | BOM | processeur d'annotations déclaré dans `maven-compiler-plugin` |
| `springdoc-openapi-starter-webmvc-ui` | 3.1.0 | **hors BOM**, version fixée dans le `pom.xml` |
| ArchUnit | 1.5.0 | **hors BOM**, version fixée dans le `pom.xml` |
| Testcontainers (`testcontainers-postgresql`) | BOM | BOM |
| `spring-boot-devtools` | BOM | `runtime` |

Toute dépendance hors BOM doit être vérifiée à chaque montée de Spring Boot.

## 11.2 Frontend

| Paquet | Version résolue (lockfile) |
|---|---|
| `@angular/core` | 22.1.7 |
| `@angular/cli`, `@angular/build` | 22.1.8 |
| `@angular/ssr` | 22.1.8 |
| TypeScript | 6.0.3 |
| Tailwind CSS (`@tailwindcss/postcss`) | 4.3.3 |
| Vitest (via `@angular/build:unit-test`) | 4.1.11 |
| Express (serveur SSR) | 5.2.1 |

Le moteur Node requis par Angular 22.1 est `^22.22.3 || ^24.15.0 || >=26.0.0`. Le projet cible Node 24 (`.nvmrc`).

---

# 12. Fichiers d'épinglage des outils

| Fichier | Contenu | Remarque |
|---|---|---|
| `.nvmrc` | `24` | Node 24 LTS |
| `.sdkmanrc` | `java=25.0.4.1-tem`, `maven=3.9.15` | le Maven de référence reste celui du wrapper (3.9.16) ; la ligne `maven` de SDKMAN ne sert qu'aux opérations hors wrapper |
| `backend/.mvn/wrapper/maven-wrapper.properties` | Maven 3.9.16, wrapper 3.3.4 | référence |
| `frontend/package.json` → `packageManager` | `npm@11.16.0` | |
