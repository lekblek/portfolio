# Architecture backend — Portfolio

## 1. Objectif

Ce document définit l’architecture du backend du projet Portfolio.

Le backend est construit comme un **monolithe modulaire Spring Boot**, organisé par domaine métier.

Les objectifs sont :

- isoler les responsabilités métier ;
- limiter les dépendances entre modules ;
- empêcher les cycles ;
- éviter les packages génériques de type `utils`, `helpers` ou `common` ;
- conserver une architecture simple à comprendre, tester et faire évoluer ;
- rendre les frontières architecturales vérifiables automatiquement avec ArchUnit.

Le package racine du backend est :

```text
com.scalke.portfolio.backend
```

La classe `Application` reste dans ce package racine afin que Spring Boot puisse détecter automatiquement tous les composants situés dans ses sous-packages.

---

## 2. Style architectural

Le backend adopte un découpage :

```text
package by feature
```

et non :

```text
package by technical layer
```

Le premier niveau représente donc les domaines métier :

```text
shared/
security/
profile/
project/
publication/
series/
taxonomy/
media/
search/
contact/
```

Les couches techniques apparaissent uniquement **à l’intérieur d’un module** lorsqu’elles deviennent nécessaires.

Exemple (détail au §6) :

```text
project/
├── domain/
├── application/
├── infrastructure/
└── web/
```

Cette organisation permet de garder ensemble le code appartenant à une même fonctionnalité et de contrôler les dépendances entre domaines.

---

## 3. Modules du backend

### 3.1 `shared`

Responsabilités :

- primitives réellement transversales ;
- gestion commune des erreurs ;
- pagination ;
- audit ;
- horloge applicative ;
- abstractions techniques très générales utilisées par plusieurs modules.

Contraintes :

- `shared` ne dépend d’aucun module métier ;
- il ne doit pas devenir un package fourre-tout ;
- une classe ne doit être placée dans `shared` que si sa responsabilité est réellement transversale.

Dépendances autorisées :

```text
shared → aucune dépendance métier
```

Contenu actuel (étape 19) :

| Paquet | Contenu | Règle |
|---|---|---|
| `shared.api` | `GlobalExceptionHandler`, `PageResponse`, `ApiPaging` | contrats HTTP transversaux |
| `shared.error` | `ErrorCode`, `ApplicationException` et sous-classes | erreurs métier stables |
| `shared.domain.model` | `DateRange`, `PageQuery`, `PageResult` | objets de valeur utilisés par **plusieurs** modules ; soumis aux règles de `domain` (§12.4 : ni Spring ni JPA) |
| `shared.infrastructure` | `ClockConfiguration` | horloge applicative (`Clock`, D03) : les cas d’usage lisent « maintenant » dans ce bean, jamais par `Instant.now()` ; horloge fixe dans les `*IT` (D-AG) |

Une classe n’entre dans `shared.domain.model` qu’au deuxième usage réel dans un autre module (ex. `DateRange` : `profile` puis `project`, D-S).

---

### 3.2 `security`

Responsabilités :

- configuration Spring Security ;
- authentification administrateur ;
- sessions HTTP ;
- cookies ;
- CSRF ;
- protections HTTP ;
- gestion du compte administrateur unique.

Dépendances autorisées :

```text
security → shared
```

Les autres modules métier ne doivent pas dépendre directement de `security`.

La sécurité protège principalement les points d’entrée HTTP et l’accès aux fonctionnalités administratives.

---

### 3.3 `profile`

Responsabilités :

- profil professionnel ;
- compétences ;
- expériences ;
- formations ;
- certifications ;
- liens professionnels ;
- avatar ;
- CV.

Dépendances autorisées :

```text
profile → shared
profile → media
```

---

### 3.4 `project`

Responsabilités :

- projets ;
- état métier du projet ;
- visibilité publique ;
- technologies ;
- captures ;
- média de couverture ;
- mise en avant ;
- ordre d’affichage.

Dépendances autorisées :

```text
project → shared
project → media
```

Les technologies des projets restent distinctes des tags éditoriaux.

---

### 3.5 `publication`

Responsabilités :

- moteur éditorial commun ;
- articles ;
- news ;
- contenu Markdown ;
- cycle de vie éditorial ;
- publication planifiée ;
- slugs ;
- métadonnées SEO ;
- catégorie ;
- tags ;
- média de couverture.

Types supportés :

```text
ARTICLE
NEWS
```

Dépendances autorisées :

```text
publication → shared
publication → media
publication → taxonomy
```

`publication` ne dépend jamais de `series`.

Une publication doit pouvoir exister indépendamment d’une série.

---

### 3.6 `series`

Responsabilités :

- séries d’articles ;
- ordre des chapitres ;
- position d’un article ;
- navigation précédent / suivant ;
- progression dans une série.

Dépendances autorisées :

```text
series → shared
series → publication
series → media
```

Une série référence des publications de type `ARTICLE`.

La dépendance est volontairement unidirectionnelle :

```text
series → publication
```

et jamais :

```text
publication → series
```

Cela évite un cycle entre les deux modules.

---

### 3.7 `taxonomy`

Responsabilités :

- catégories éditoriales ;
- tags éditoriaux ;
- slugs associés.

Dépendances autorisées :

```text
taxonomy → shared
```

La taxonomie éditoriale est indépendante des technologies utilisées dans les projets.

---

### 3.8 `media`

Responsabilités :

- catalogue des médias ;
- métadonnées des fichiers ;
- abstraction de stockage ;
- contrôle des références avant suppression.

Abstraction principale :

```text
MediaStorage
```

Implémentation V1 :

```text
LocalMediaStorage
```

Dépendances autorisées :

```text
media → shared
```

Le domaine manipule une clé de stockage :

```text
storageKey
```

et non un chemin physique du système de fichiers.

---

### 3.9 `search`

Responsabilités :

- recherche publique ;
- PostgreSQL Full-Text Search ;
- classement des résultats ;
- pagination ;
- lecture des publications visibles ;
- lecture des projets publiés.

Dépendances autorisées :

```text
search → shared
search → publication
search → project
```

La recherche est une capacité de lecture.

Les modules `publication` et `project` ne doivent jamais dépendre de `search`.

---

### 3.10 `contact`

Responsabilités :

- formulaire de contact ;
- validation métier du message ;
- persistence ;
- cycle de statut ;
- déclenchement d’une notification après sauvegarde.

Dépendances autorisées :

```text
contact → shared
```

L’envoi d’un email ne fait pas partie de la transaction de sauvegarde du message.

Un échec SMTP ne doit jamais annuler un message déjà enregistré.

---

## 4. Graphe des dépendances autorisées

Le contrat de dépendances est le suivant :

```text
shared
   ↑
   ├── security
   ├── taxonomy
   ├── media
   ├── contact
   │
   ├── profile ───────────────→ media
   │
   ├── project ───────────────→ media
   │
   ├── publication ───────────→ media
   │          │
   │          └───────────────→ taxonomy
   │
   ├── series ────────────────→ publication
   │      └───────────────────→ media
   │
   └── search ────────────────→ publication
          └───────────────────→ project
```

Sous forme compacte :

```text
shared       → rien
security     → shared
taxonomy     → shared
media        → shared
contact      → shared
profile      → shared, media
project      → shared, media
publication  → shared, media, taxonomy
series       → shared, publication, media
search       → shared, publication, project
```

Ce graphe doit rester **acyclique**.

---

## 5. Dépendances interdites importantes

Les dépendances suivantes sont explicitement interdites.

### `shared` vers un domaine métier

Interdit :

```text
shared → project
shared → publication
shared → profile
...
```

`shared` est le socle le plus bas de l’architecture.

---

### `publication` vers `series`

Interdit :

```text
publication → series
```

Une publication ignore si elle est utilisée dans une série.

C’est `series` qui référence une publication.

---

### `project` ou `publication` vers `search`

Interdit :

```text
project → search
publication → search
```

Le moteur de recherche observe les contenus publics ; les domaines recherchés ne connaissent pas le moteur de recherche.

---

### Modules métier vers `security`

Interdit par défaut :

```text
profile → security
project → security
publication → security
...
```

La sécurité s’applique principalement à la couche HTTP et à la configuration de l’application.

Une dépendance métier vers `security` serait le signe d’un couplage incorrect.

---

## 6. Structure interne d’un module

> Section réécrite le 2026-09-24 pour décrire la structure réellement implémentée dans `profile`.
> Référence et justification : [ADR 0001](decisions/0001-architecture-interne-des-modules.md) (acceptée le 2026-09-24).
> La version précédente (`api/`, `domain/`, `repository/`, `service/`, entités JPA dans `domain`) est celle qu'utilisent encore les fiches de `docs/steps/` ; l'ADR donne la table de traduction.

Un module est organisé en ports et adaptateurs « légers » :

```text
<module>/
├── domain/
│   ├── model/          modèle métier (records, objets de valeur, enums, invariants)
│   └── port/           interfaces sortantes utilisées par les cas d’usage
├── application/
│   └── usecase/        un cas d’usage par classe, frontière transactionnelle
├── infrastructure/
│   ├── persistence/jpa/
│   │   ├── entity/     entités JPA *Entity
│   │   ├── mapper/     conversions entité ↔ modèle
│   │   └── repository/ *JpaRepository (Spring Data) + *RepositoryAdapter (implémente le port)
│   └── seed/           données de développement (profil Spring dev uniquement)
└── web/
    ├── controller/     Public*Controller, Admin*Controller
    └── dto/            *Request, *Response (records)
```

Les sous-paquets n’apparaissent qu’avec leur première classe.

### `domain.model`

- modèle métier indépendant de JPA, de Spring et de Lombok ;
- peut dépendre de `shared.domain.model` (objets de valeur partagés) et de `shared.error` ;
- porte les invariants (constructeur compact, méthodes de l’agrégat) ;
- testé par des tests unitaires purs (`*Test`, sans Spring ni Docker).

### `domain.port`

- interfaces **sortantes** seulement (persistance, stockage, email…) ;
- ne contient que les méthodes réellement appelées par un cas d’usage ;
- ne manipule aucun type de framework : une lecture paginée prend un `PageQuery` et renvoie un `PageResult` (`shared.domain.model`), jamais `Pageable` / `Page` de Spring Data (D-V) ;
- aucune implémentation bouchon (`return Optional.empty()`, `return false`, méthode vide).

### `application.usecase`

- une classe concrète par intention : `GetProfileUseCase`, `CreateProjectUseCase`… avec une méthode `execute(...)` ;
- pas d’interface d’entrée (`XxxUseCase` + `XxxUseCaseImpl`) ;
- porte `@Transactional` (`readOnly = true` pour une lecture) : c’est la frontière transactionnelle ;
- renvoie des objets du domaine, jamais des entités JPA.

### `infrastructure`

- entités JPA : structures de persistance, sans règle métier propre ;
- l’adaptateur exécute ses requêtes dans la transaction du cas d’usage et convertit en modèle métier avant de rendre la main ; il peut porter `@Transactional` pour rester correct lorsqu’il est appelé hors cas d’usage (seed `dev`) ;
- entités fermées : constructeur sans argument `protected`, pas de setter public hors champs optionnels, méthodes `addX` qui fixent la référence arrière ;
- Flyway reste l’unique propriétaire du schéma (principe 6, §14).

### `web`

- contrôleurs préfixés `Public` ou `Admin` (voir `05-conventions-api.md`) ;
- DTO en records, construits par une méthode statique `from(...)` à partir du modèle métier ;
- dépend de `application` et de `domain.model`, **jamais** de `infrastructure`.

### Dépendances internes autorisées

```text
web ──────────→ application ──→ domain
 │                                 ↑
 └──────────────→ domain.model     │
infrastructure ────────────────────┘
```

Interdits :

```text
domain         → application, infrastructure, web, Spring, jakarta.persistence
application    → infrastructure, web
web            → infrastructure
infrastructure → web
```

Une entité JPA n’est jamais exposée comme contrat REST, et un contrôleur ne manipule jamais une entité JPA.

---

## 7. Visibilité Java

Par défaut, la visibilité doit être la plus restrictive possible.

Une classe interne à un module n’a pas besoin d’être `public` si elle n’est pas utilisée depuis un autre package.

Règle générale :

```text
public
→ uniquement si nécessaire en dehors du package
```

La visibilité package-private participe à l’encapsulation du monolithe modulaire.

---

## 8. Contrats inter-modules

Un module ne doit pas exposer ses détails internes inutilement.

> Depuis l’étape 20, ce principe est une règle vérifiée : voir [ADR 0002](decisions/0002-communication-entre-modules.md) et §12.5. Première façade : `taxonomy.application.query.TaxonomyQueryService`, utilisée par `publication`.

Les dépendances entre modules passent par :

```text
Module A
   ↓
API / façade publique du module B
   ↓
internes du module B
```

et éviter :

```text
Module A
   ↓
Repository interne du module B
```

Exemple souhaité :

```text
series
   ↓
PublicationQueryService
   ↓
publication
```

plutôt que :

```text
series
   ↓
PublicationRepository
```

lorsque l’accès direct au repository rendrait les modules trop couplés.

---

## 9. Package racine Spring Boot

La classe principale reste dans :

```text
com.scalke.portfolio.backend
```

Exemple :

```text
com.scalke.portfolio.backend.Application
```

Les modules sont directement placés en dessous :

```text
com.scalke.portfolio.backend.shared
com.scalke.portfolio.backend.security
com.scalke.portfolio.backend.profile
com.scalke.portfolio.backend.project
com.scalke.portfolio.backend.publication
com.scalke.portfolio.backend.series
com.scalke.portfolio.backend.taxonomy
com.scalke.portfolio.backend.media
com.scalke.portfolio.backend.search
com.scalke.portfolio.backend.contact
```

Cette disposition permet à `@SpringBootApplication` de détecter les composants sans configuration supplémentaire de `component scan`.

---

## 10. Packages interdits au premier niveau

Les packages racines suivants ne doivent pas apparaître :

```text
utils/
helper/
helpers/
common/
misc/
services/
controllers/
repositories/
entities/
```

Les responsabilités doivent être rattachées à un domaine métier existant.

Exemple incorrect :

```text
com.scalke.portfolio.backend.service.ProjectService
```

Exemple attendu :

```text
com.scalke.portfolio.backend.project.application.usecase.CreateProjectUseCase
```

---

## 11. API publique et API d’administration

Le backend exposera deux familles de routes :

```text
/api/public/**
/api/admin/**
```

La séparation HTTP ne doit pas être traduite par un package Java nommé :

```text
public
```

car `public` est un mot réservé Java.

La distinction se fera donc par les noms de classes ou une organisation de package compatible.

Exemples :

```text
PublicProjectController
AdminProjectController
```

ou, si une séparation supplémentaire devient utile :

```text
project.web.controller.publicapi
project.web.controller.admin
```

Le choix définitif des conventions REST est traité dans `05-conventions-api.md` ou dans l’étape dédiée aux conventions API.

Les conventions HTTP détaillées du backend sont définies dans [`05-conventions-api.md`](./05-conventions-api.md).
---

## 12. Règles d’architecture automatisées

L’architecture doit être protégée avec ArchUnit.

Les règles minimales sont :

### 12.1 Absence de cycles

```text
aucun cycle entre les modules racines
```

Exemple de cycle interdit :

```text
publication
   ↓
series
   ↓
publication
```

---

### 12.2 Isolation de `shared`

Aucune classe de `shared` ne doit dépendre d’un module métier.

---

### 12.3 Fermeture de l’espace de nommage

Les classes applicatives doivent résider uniquement dans :

```text
com.scalke.portfolio.backend
com.scalke.portfolio.backend.shared..
com.scalke.portfolio.backend.security..
com.scalke.portfolio.backend.profile..
com.scalke.portfolio.backend.project..
com.scalke.portfolio.backend.publication..
com.scalke.portfolio.backend.series..
com.scalke.portfolio.backend.taxonomy..
com.scalke.portfolio.backend.media..
com.scalke.portfolio.backend.search..
com.scalke.portfolio.backend.contact..
```

Cela empêche l’apparition silencieuse de packages non prévus comme :

```text
com.scalke.portfolio.backend.utils
com.scalke.portfolio.backend.common
```

---

### 12.4 Couches internes d’un module

État au 2026-09-24 : **automatisé** (`ApiConventionsTest`, `ModuleLayersTest`).

| Règle | Test | État |
|---|---|---|
| contrôleurs dans `..web.controller..` | `ApiConventionsTest` | en place |
| contrôleurs préfixés `Public` / `Admin` | `ApiConventionsTest` | en place |
| entités JPA dans `..infrastructure.persistence.jpa.entity..` | `ApiConventionsTest` | en place |
| `domain` ne dépend ni de `application`, `infrastructure`, `web`, ni de Spring / `jakarta.persistence` / Hibernate | `ModuleLayersTest` | en place |
| `application` ne dépend ni de `infrastructure` ni de `web` | `ModuleLayersTest` | en place |
| `web` ne dépend pas de `infrastructure` | `ModuleLayersTest` | en place |
| `infrastructure` ne dépend pas de `web` | `ModuleLayersTest` | en place |

---

### 12.5 Communication entre modules

État au 2026-09-24 (étape 20) : **automatisé** (`ModuleBoundariesTest.modules_only_use_each_other_through_domain_models_and_application_services`).

Un module n’utilise d’un autre module que ses records `domain.model` et ses services `application` ; jamais ses `domain.port`, son `infrastructure` ni son `web` ([ADR 0002](decisions/0002-communication-entre-modules.md)).

---

## 13. Arborescence initiale (étape 10)

L’étape 10 a créé uniquement les packages racines. Les modules implémentés (`profile` depuis l’étape 14, `project` depuis l’étape 17, `publication` depuis l’étape 19, `taxonomy` depuis l’étape 20) et `shared` suivent le §6 et le §3.1. Une façade de lecture destinée aux autres modules vit dans `<module>.application.query` (ADR 0002).

```text
backend/
└── src/
    └── main/
        └── java/
            └── com/
                └── scalke/
                    └── portfolio/
                        └── backend/
                            ├── Application.java
                            ├── shared/
                            │   └── package-info.java
                            ├── security/
                            │   └── package-info.java
                            ├── profile/
                            │   └── package-info.java
                            ├── project/
                            │   └── package-info.java
                            ├── publication/
                            │   └── package-info.java
                            ├── series/
                            │   └── package-info.java
                            ├── taxonomy/
                            │   └── package-info.java
                            ├── media/
                            │   └── package-info.java
                            ├── search/
                            │   └── package-info.java
                            └── contact/
                                └── package-info.java
```

Les sous-packages décrits au §6 sont créés seulement lorsqu’un module reçoit réellement ses premières classes.

---

## 14. Principes à respecter pendant le développement

### Principe 1 — Le domaine possède ses données

Chaque concept métier appartient à un module précis.

Exemple :

```text
Publication → publication
Series      → series
Media       → media
```

---

### Principe 2 — Pas d’accès direct généralisé entre repositories

Un repository n’est pas un contrat public du backend.

L’accès entre modules doit être volontaire et minimal.

---

### Principe 3 — Les dépendances vont dans un seul sens

Le graphe défini dans ce document est la référence.

Toute nouvelle dépendance inter-module doit être justifiée avant d’être ajoutée.

---

### Principe 4 — Pas d’anticipation inutile

La V1 ne crée pas de modules pour :

```text
newsletter
analytics
recommendation
payment
user
roles
comments
```

tant que ces besoins ne font pas partie du périmètre.

---

### Principe 5 — Les entités JPA ne sont pas des DTO

Le modèle de persistence reste interne au backend.

Les contrôleurs exposent des DTO dédiés.

---

### Principe 6 — Flyway possède le schéma

Toute évolution structurelle PostgreSQL passe par une migration Flyway.

Hibernate doit valider le schéma, pas le modifier automatiquement.

---

## 15. Architecture et tests

Le test d’architecture doit être exécuté avec la suite Maven normale :

```bash
cd backend
./mvnw test
```

Sous Windows PowerShell :

```powershell
cd backend
.\mvnw.cmd test
```

Une violation architecturale doit produire :

```text
BUILD FAILURE
```

L’objectif est que l’architecture soit une contrainte du build et non une simple convention documentaire.

---

## 16. Décisions principales

| Réf | Décision |
|---|---|
| A01 | Backend construit comme monolithe modulaire |
| A02 | Découpage racine par domaine fonctionnel |
| A03 | `shared` ne dépend d’aucun domaine métier |
| A04 | Graphe de dépendances acyclique |
| A05 | `series` dépend de `publication`, jamais l’inverse |
| A06 | `search` dépend de `publication` et `project`, jamais l’inverse |
| A07 | Les modules métier ne dépendent pas directement de `security` |
| A08 | Les couches techniques sont internes à chaque module (structure du §6, [ADR 0001](decisions/0001-architecture-interne-des-modules.md)) |
| A09 | Une entité JPA n’est jamais exposée directement par l’API |
| A10 | Les dépendances architecturales sont vérifiées avec ArchUnit |
| A11 | Aucun package racine générique `utils`, `helpers` ou `common` |
| A12 | Le package racine Spring Boot est `com.scalke.portfolio.backend` |
| A13 | Modèle métier sans JPA ; persistance derrière un port ; cas d’usage transactionnels ([ADR 0001](decisions/0001-architecture-interne-des-modules.md)) |
| A14 | Entre modules : références par identifiant et façades de lecture ; ni port, ni persistance, ni web d’un autre module ([ADR 0002](decisions/0002-communication-entre-modules.md)) |

---

## 17. Résultat

Le backend dispose d’un contrat architectural explicite :

```text
domaine
   ↓
frontières de modules
   ↓
dépendances autorisées
   ↓
tests ArchUnit
   ↓
build Maven
```

L’architecture du projet n’est donc pas seulement représentée par une arborescence de dossiers.

Elle est documentée, contrainte et vérifiable automatiquement.
