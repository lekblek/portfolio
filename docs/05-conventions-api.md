# Conventions API — Portfolio

## 1. Objectif

Ce document définit le **contrat HTTP commun** du backend Portfolio.

Il fixe les conventions utilisées par tous les modules exposant une API afin d’éviter que chaque contrôleur définisse ses propres règles.

Les conventions couvrent notamment :

* les espaces de routes publiques et administratives ;
* le nommage des ressources ;
* les identifiants utilisés dans les URI ;
* les verbes HTTP ;
* les codes de statut ;
* les DTO ;
* le format des erreurs ;
* la pagination ;
* les dates ;
* les enums ;
* les valeurs nulles ;
* les conventions de nommage Java.

Le package racine du backend est :

```text
com.scalke.portfolio.backend
```

Les conventions définies ici constituent le contrat de référence pour les futures API du projet.

---

# 2. Principes généraux

L’API suit plusieurs principes simples :

```text
HTTP
→ porte la sémantique du transport

DTO
→ portent le contrat d’échange

domaine
→ porte les règles métier

entités JPA
→ restent internes au backend
```

Une entité JPA ne doit jamais devenir directement une représentation HTTP.

Le backend distingue deux familles principales d’API :

```text
/api/public/**
/api/admin/**
```

Cette séparation sert à la fois :

* à rendre les routes compréhensibles ;
* à distinguer les représentations publiques et administratives ;
* à simplifier les règles Spring Security ;
* à éviter d’exposer accidentellement des informations d’administration.

---

# 3. Préfixes de routes

## API publique

```text
/api/public/**
```

Elle contient les ressources accessibles sans authentification.

> Le préfixe `/api` est porté par `server.servlet.context-path: /api` (`application.yaml`). Les contrôleurs sont donc mappés **sans** `/api` : `@RequestMapping("/public/profile")` répond sur `/api/public/profile`. Conséquence : l’Actuator répond sur `/api/actuator/health`.
>
> `profile` est une ressource **singleton** (un seul profil en V1) : elle reste au singulier, `GET /api/public/profile`, sans identifiant.

Exemples :

```text
GET /api/public/profile

GET /api/public/projects
GET /api/public/projects/{slug}

GET /api/public/publications
GET /api/public/publications/{slug}

GET /api/public/series
GET /api/public/series/{slug}

GET /api/public/search?q=spring

POST /api/public/contact-messages
```

---

## API d’administration

```text
/api/admin/**
```

Elle contient les opérations réservées à l’administrateur.

Exemples :

```text
GET    /api/admin/projects
POST   /api/admin/projects
GET    /api/admin/projects/{id}
PUT    /api/admin/projects/{id}
DELETE /api/admin/projects/{id}

GET    /api/admin/publications
POST   /api/admin/publications
GET    /api/admin/publications/{id}

GET    /api/admin/contact-messages
GET    /api/admin/media
```

À l’étape sécurité, la règle générale sera :

```text
/api/public/** → accès public

/api/admin/**  → authentification obligatoire

autres routes  → refusées sauf exception explicitement configurée
```

---

# 4. Versionnement de l’API

La V1 n’utilise pas de segment :

```text
/api/v1/**
```

Les routes restent :

```text
/api/public/**
/api/admin/**
```

Le frontend Angular et le backend Spring Boot appartiennent au même repository et sont déployés ensemble.

Il n’existe donc pas actuellement plusieurs clients ayant des cycles de vie indépendants nécessitant le maintien simultané de plusieurs versions du contrat.

Si ce besoin apparaît ultérieurement, une nouvelle version pourra être introduite explicitement.

Exemple :

```text
/api/v2/public/**
```

Le versionnement ne doit pas être ajouté uniquement « au cas où ».

---

# 5. Nommage des ressources

Les ressources utilisent :

```text
noms pluriels
kebab-case
noms métier
```

Exemples corrects :

```text
/projects
/publications
/contact-messages
/categories
/tags
```

Éviter :

```text
/project
/getProjects
/createPublication
/contactMessages
```

---

## Pas de slash final

Préférer :

```text
/api/public/projects
```

et non :

```text
/api/public/projects/
```

---

## Pas de verbes métier ordinaires dans les URI

Éviter :

```text
POST /api/admin/createProject
GET /api/public/getArticles
```

Préférer :

```text
POST /api/admin/projects
GET /api/public/publications
```

Les verbes HTTP portent déjà l’action principale.

---

# 6. Identifiants dans les URI

## Ressources publiques

Les ressources destinées à être consultées publiquement utilisent leur `slug`.

Exemple :

```text
GET /api/public/projects/portfolio-full-stack

GET /api/public/publications/
    construire-une-api-rest-avec-spring-boot
```

Le slug :

* est lisible ;
* est adapté aux URL publiques ;
* participe au SEO ;
* ne révèle pas l’identifiant technique ;
* devient stable après la première publication conformément au périmètre V1.

---

## Administration

Les routes d’administration utilisent l’identifiant technique.

Exemple :

```text
GET /api/admin/projects/{id}

GET /api/admin/publications/{id}
```

Exemple conceptuel :

```text
public
→ /api/public/publications/api-rest-spring-boot

admin
→ /api/admin/publications/42
```

Le type concret de l’identifiant technique appartient au modèle de persistence.

Il pourra être :

```text
BIGINT
```

ou :

```text
UUID
```

selon les décisions prises lors de l’implémentation.

Pour le client, cet identifiant reste opaque :

```text
Angular le reçoit
Angular le conserve
Angular le renvoie

Angular ne le calcule pas
Angular ne lui attribue aucune signification métier
```

---

# 7. Verbes HTTP

Les conventions suivantes sont utilisées.

| Opération                               | Verbe    |
| --------------------------------------- | -------- |
| Lister                                  | `GET`    |
| Lire                                    | `GET`    |
| Créer                                   | `POST`   |
| Remplacer une représentation modifiable | `PUT`    |
| Modifier partiellement                  | `PATCH`  |
| Supprimer                               | `DELETE` |

---

# 8. Codes de succès

## Lecture

```text
GET
→ 200 OK
```

Exemple :

```text
GET /api/public/projects
→ 200
```

---

## Création

```text
POST
→ 201 Created
```

La réponse fournit également l’en-tête :

```text
Location
```

Exemple :

```http
HTTP/1.1 201 Created
Location: /api/admin/projects/42
```

Le corps contient la représentation créée lorsque cela apporte une valeur au client.

---

## Modification

```text
PUT
→ 200 OK

PATCH
→ 200 OK
```

La représentation mise à jour peut être retournée.

---

## Suppression

```text
DELETE
→ 204 No Content
```

Aucun corps de réponse n’est envoyé.

---

# 9. Codes d’erreur

Les codes principaux de la V1 sont :

| Code  | Signification                                                        |
| ----- | -------------------------------------------------------------------- |
| `400` | requête invalide ou validation échouée                               |
| `401` | authentification requise ou invalide                                 |
| `403` | utilisateur authentifié mais opération interdite, ou protection CSRF |
| `404` | ressource inexistante ou non visible publiquement                    |
| `409` | conflit avec un invariant métier                                     |
| `500` | erreur inattendue du serveur                                         |

---

## 400 — Bad Request

Exemples :

```text
champ obligatoire absent
format invalide
paramètre malformé
validation Jakarta échouée
```

La V1 n’introduit pas `422 Unprocessable Content`.

Les erreurs de validation HTTP utilisent `400`.

---

## 401 — Unauthorized

Utilisé lorsqu’une route administrative nécessite une authentification valide.

Exemple :

```text
GET /api/admin/projects

sans session valide
→ 401
```

---

## 403 — Forbidden

Utilisé lorsque l’identité est connue mais que l’opération est refusée.

Il peut également être utilisé pour certains échecs de sécurité comme CSRF.

---

## 404 — Not Found

Utilisé lorsqu’une ressource n’existe pas.

Il est également utilisé lorsqu’une ressource existe techniquement mais ne doit pas être visible publiquement.

Exemple :

```text
Publication.status = DRAFT

GET /api/public/publications/mon-brouillon
→ 404
```

Le backend ne renvoie pas :

```text
403
```

car cela confirmerait l’existence de la ressource.

Du point de vue de l’API publique, un contenu non visible n’existe pas.

Cette règle s’applique notamment à :

```text
Publication DRAFT
Publication IN_REVIEW
Publication ARCHIVED
Publication SCHEDULED non encore visible
Project DRAFT
Project ARCHIVED
```

---

## 409 — Conflict

Utilisé lorsqu’une opération entre en conflit avec un invariant métier.

Exemples :

```text
slug déjà utilisé

position déjà occupée dans une série

tentative d’ajouter une NEWS à une série

média encore référencé

transition éditoriale interdite
```

---

## 500 — Internal Server Error

Utilisé pour un défaut inattendu du serveur.

La réponse HTTP ne doit jamais exposer :

```text
stack trace
requête SQL
mot de passe
secret
chemin interne sensible
configuration serveur
```

Les détails techniques restent dans les logs du backend.

---

# 10. Format des erreurs

Les erreurs utilisent **Problem Details for HTTP APIs — RFC 9457**.

Type de média :

```text
application/problem+json
```

Format général :

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Ce slug est déjà utilisé.",
  "instance": "/api/admin/publications",
  "code": "SLUG_ALREADY_USED"
}
```

Les champs standards utilisés sont notamment :

```text
type
title
status
detail
instance
```

Le projet ajoute l’extension :

```text
code
```

---

# 11. Code d’erreur métier

`code` constitue l’identifiant stable d’une erreur applicative.

Convention :

```text
SCREAMING_SNAKE_CASE
```

Exemples :

```text
SLUG_ALREADY_USED

RESOURCE_NOT_FOUND

MEDIA_STILL_REFERENCED

INVALID_PUBLICATION_TRANSITION

SERIES_POSITION_ALREADY_USED
```

Le frontend peut utiliser :

```text
code
```

pour sa logique.

Il ne doit jamais dépendre du texte contenu dans :

```text
detail
```

Exemple incorrect côté frontend :

```text
if (error.detail === "Ce slug est déjà utilisé.") {
    ...
}
```

Le texte peut évoluer ou être traduit.

Préférer conceptuellement :

```text
if (error.code === "SLUG_ALREADY_USED") {
    ...
}
```

---

# 12. Réponses de succès

Les réponses de succès ordinaires ne sont pas enveloppées artificiellement.

Exemple :

```json
{
  "title": "Portfolio full-stack",
  "slug": "portfolio-full-stack",
  "featured": true
}
```

et non :

```json
{
  "success": true,
  "data": {
    "title": "Portfolio full-stack"
  }
}
```

Le protocole HTTP fournit déjà :

```text
status
headers
media type
```

L’enveloppe `success/data` n’ajouterait aucune information utile.

La seule exception générale concerne les collections paginées, car leur enveloppe transporte de vraies métadonnées.

---

# 13. Pagination

La pagination suit les conventions Spring Data.

Paramètres :

```text
page
size
sort
```

---

## Numérotation

Les pages sont **0-based**.

```text
page=0
→ première page

page=1
→ deuxième page
```

Ce choix est cohérent avec :

```text
Spring Data Pageable
Angular Material MatPaginator
```

et évite les conversions `+1` / `-1` entre les différentes couches.

---

## Tailles par défaut

Conformément à la décision D16 :

```text
public → 10 éléments
admin  → 20 éléments
```

Maximum autorisé :

```text
100
```

Conceptuellement :

```text
PUBLIC_PAGE_SIZE = 10
ADMIN_PAGE_SIZE  = 20
MAX_PAGE_SIZE    = 100
```

Une requête ne doit pas pouvoir demander arbitrairement :

```text
size=1000000
```

---

# 14. Format d’une réponse paginée

Le backend n’expose jamais directement :

```text
org.springframework.data.domain.Page
```

La réponse utilise un contrat stable :

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 37,
  "totalPages": 4,
  "first": true,
  "last": false
}
```

Contrat Java partagé :

```text
PageResponse<T>
```

Structure conceptuelle :

```java
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
```

La représentation JSON de `Page` appartient à Spring Data et ne constitue pas le contrat HTTP du projet.

---

# 15. Tri

Le tri utilise la convention Spring Data :

```text
sort=champ,direction
```

Exemples :

```text
sort=publishedAt,desc

sort=title,asc
```

Exemple complet :

```text
GET /api/public/publications
    ?page=0
    &size=10
    &sort=publishedAt,desc
```

Les champs autorisés au tri doivent être contrôlés par l’API.

Le client ne doit pas pouvoir transformer arbitrairement n’importe quel champ interne de persistence en paramètre de tri exposé.

---

# 16. Filtres

Les filtres sont exprimés avec des paramètres de requête.

Exemple :

```text
GET /api/public/publications?type=ARTICLE
```

Autres exemples possibles :

```text
/api/public/publications?category=spring-boot

/api/public/projects?technology=java

/api/public/search?q=postgresql
```

Les filtres doivent utiliser des noms métier et non des noms de colonnes PostgreSQL.

---

# 17. Transitions d’état

Les transitions métier importantes ne sont pas traitées comme une simple écriture arbitraire de champ.

Le cycle éditorial :

```text
DRAFT
IN_REVIEW
SCHEDULED
PUBLISHED
ARCHIVED
```

possède des règles propres.

Une modification de statut utilise donc une route dédiée au statut.

Exemple :

```text
POST /api/admin/publications/{id}/status
```

Corps :

```json
{
  "status": "PUBLISHED",
  "publishedAt": "2026-10-01T09:00:00Z"
}
```

Cette opération peut échouer avec :

```text
409 Conflict
```

si la transition viole un invariant.

L’objectif est d’éviter qu’un simple :

```text
PATCH /publications/{id}
```

permette de contourner les règles du cycle éditorial en considérant `status` comme un champ ordinaire.

---

# 18. DTO

Les contrats HTTP utilisent des DTO dédiés.

Les entités JPA :

```text
ne sont jamais retournées directement
ne sont jamais acceptées directement comme corps HTTP
```

Exemple d’organisation (structure de l’ADR 0001 ; les fichiers `Admin*` et `*Request` arriveront à l’étape 36) :

```text
project/
└── web/
    ├── controller/
    │   ├── PublicProjectController.java
    │   └── AdminProjectController.java
    └── dto/
        ├── ProjectResponse.java
        ├── ProjectSummaryResponse.java
        ├── AdminProjectResponse.java
        ├── CreateProjectRequest.java
        └── UpdateProjectRequest.java
```

---

# 19. Représentations publique et administrative

Une même ressource métier peut avoir plusieurs représentations HTTP.

Exemple :

```text
Publication
```

peut produire :

```text
PublicationSummaryResponse
PublicationResponse
AdminPublicationResponse
```

La représentation publique peut exposer :

```text
title
slug
summary
contentMarkdown
publishedAt
category
tags
```

La représentation d’administration peut également contenir :

```text
id
status
seoTitle
seoDescription
createdAt
updatedAt
```

Il ne faut pas créer un DTO unique contenant tous les champs avec une série de valeurs `null` selon la route.

Les contrats public et administration répondent à des usages différents.

---

# 20. DTO Java en records

Les DTO utilisent préférentiellement les `record` Java.

Exemple :

```java
public record ProjectSummaryResponse(
        String title,
        String slug,
        String shortDescription,
        boolean featured) {
}
```

Les avantages recherchés sont :

* immutabilité ;
* contrat explicite ;
* peu de code cérémoniel ;
* égalité structurelle ;
* bonne intégration avec Jackson.

Les DTO ne contiennent pas de logique métier.

---

# 21. Validation des requêtes

Les DTO d’entrée utilisent Jakarta Validation lorsque pertinent.

Exemple conceptuel :

```java
public record CreateProjectRequest(
        @NotBlank String title,
        @NotBlank String shortDescription) {
}
```

Quelques distinctions importantes :

```text
@NotNull
→ valeur non nulle

@NotEmpty
→ collection ou chaîne non vide

@NotBlank
→ chaîne contenant au moins un caractère non blanc
```

La validation du DTO protège le contrat HTTP.

Elle ne remplace pas les invariants métier du domaine.

---

# 22. Convention de nommage Java

## Contrôleurs

Les contrôleurs annoncent explicitement leur audience.

```text
PublicProjectController

AdminProjectController

PublicPublicationController

AdminPublicationController
```

Éviter :

```text
ProjectController
PublicationController
```

---

## DTO de réponse

Exemples :

```text
ProjectResponse

ProjectSummaryResponse

AdminProjectResponse

PublicationResponse

PublicationSummaryResponse

AdminPublicationResponse
```

---

## DTO de requête

Exemples :

```text
CreateProjectRequest

UpdateProjectRequest

CreatePublicationRequest

UpdatePublicationRequest

ChangePublicationStatusRequest
```

---

# 23. Packages Java

> Mis à jour le 2026-09-24 pour refléter le code réel (voir [ADR 0001](decisions/0001-architecture-interne-des-modules.md)).

Les contrôleurs et DTO HTTP résident dans :

```text
<module>.web.controller   → Public*Controller, Admin*Controller
<module>.web.dto          → *Request, *Response
```

Exemple :

```text
com.scalke.portfolio.backend.profile.web.controller.PublicProfileController
com.scalke.portfolio.backend.profile.web.dto.ProfileResponse
```

Une entité JPA appartient à :

```text
<module>.infrastructure.persistence.jpa.entity
```

et n’apparaît jamais dans `..web..` : les DTO sont construits à partir du modèle métier (`<module>.domain.model`), pas à partir des entités.

Structure complète d’un module : `04-architecture-backend.md` §6.

---

# 24. Convention JSON

Les propriétés JSON utilisent :

```text
lowerCamelCase
```

Exemples :

```json
{
  "shortDescription": "...",
  "publishedAt": "...",
  "seoDescription": "...",
  "totalElements": 37
}
```

Éviter :

```text
snake_case
PascalCase
kebab-case
```

pour les propriétés JSON.

---

# 25. Enums

Les enums sont exposés avec leur valeur explicite en majuscules.

Exemples :

```json
{
  "type": "ARTICLE",
  "status": "PUBLISHED"
}
```

Autres exemples :

```text
NEWS
DRAFT
IN_REVIEW
SCHEDULED
ARCHIVED
```

Le frontend doit considérer ces valeurs comme faisant partie du contrat HTTP.

---

# 26. Dates et heures

## Instant précis

Les instants utilisent ISO-8601 en UTC.

Exemple :

```text
2026-10-01T09:00:00Z
```

Typiquement :

```text
publishedAt
createdAt
updatedAt
```

Le backend doit privilégier une représentation temporelle adaptée à un instant absolu, par exemple :

```text
Instant
```

lorsque cela correspond à la sémantique métier.

---

## Dates sans heure

Les dates métier sans heure utilisent :

```text
YYYY-MM-DD
```

Exemple :

```text
2026-09-18
```

Typiquement :

```text
startDate
endDate
issuedAt
expiresAt
```

Une date sans heure ne doit pas être transformée artificiellement en timestamp minuit UTC.

---

# 27. Valeurs nulles

Les champs optionnels restent présents dans la représentation JSON avec :

```text
null
```

Exemple :

```json
{
  "startDate": "2026-01-01",
  "endDate": null
}
```

Préférer ce contrat stable à une propriété parfois présente et parfois absente.

Le frontend peut ainsi utiliser :

```text
endDate: string | null
```

plutôt que devoir distinguer :

```text
undefined
null
valeur
```

Le projet n’active donc pas globalement une politique du type :

```text
@JsonInclude(NON_NULL)
```

---

# 28. Encodage

Les échanges utilisent :

```text
UTF-8
```

Les réponses JSON utilisent :

```text
application/json
```

Les erreurs Problem Details utilisent :

```text
application/problem+json
```

---

# 29. Visibilité temporelle des publications

Une publication planifiée est visible publiquement uniquement lorsque :

```text
status = SCHEDULED
AND
publishedAt <= now
```

La valeur de :

```text
now
```

doit provenir de l’horloge applicative prévue par l’architecture, afin que ce comportement soit testable.

Les routes publiques appliquent la visibilité avant de produire le DTO.

Ainsi :

```text
contenu non visible
→ aucune représentation publique
→ 404
```

---

# 30. Ressources publiques prévues

Les principales ressources publiques de la V1 sont conceptuellement :

```text
/api/public/profile

/api/public/projects
/api/public/projects/{slug}

/api/public/publications
/api/public/publications/{slug}

/api/public/series
/api/public/series/{slug}

/api/public/categories
/api/public/tags

/api/public/search

/api/public/contact-messages
```

La présence dans cette liste ne signifie pas que tous les endpoints doivent être créés immédiatement.

## Contrats implémentés

### `GET /api/public/profile` (étapes 14 à 16)

```text
200 → ProfileResponse
{
  displayName, professionalTitle, shortBio,
  aboutMarkdown | null, publicLocation | null, publicEmail | null,
  links[]          { label, url },
  skillGroups[]    { category, skills[] { name } },
  experiences[]    { organization, title, location, startDate, endDate | null, description },
  educations[]     { institution, degree, field, location, startDate, endDate | null, description },
  certifications[] { name, issuer, issuedAt, expiresAt | null, credentialUrl | null }
}
404 → ProblemDetail, code RESOURCE_NOT_FOUND (aucun profil)
```

Règles propres à ce contrat :

* tableaux déjà triés dans l’ordre d’affichage (`displayOrder`, puis date décroissante, puis identifiant — D-L) ; un tableau vide vaut `[]`, jamais `null` ;
* ni identifiant technique ni `displayOrder` exposés (C03, D-R) : l’ordre du tableau fait foi ;
* `endDate: null` signifie « en cours » ; `expiresAt: null` signifie « sans expiration » (D-P) ;
* dates au format `YYYY-MM-DD` (§26) ;
* contrat vérifié par `PublicProfileIT` sur la sérialisation réelle.

### `GET /api/public/projects` (étape 17)

```text
?page=0&size=10          page 0-based ; size par défaut 10, plafonnée à 100 ; sort ignoré (ordre fixe)
200 → PageResponse<ProjectSummaryResponse>
{
  content[] { title, slug, shortDescription, stage, startDate, endDate | null, featured },
  page, size, totalElements, totalPages, first, last
}
```

### `GET /api/public/projects/{slug}` (étape 17)

```text
200 → ProjectResponse
{
  title, slug, shortDescription, descriptionMarkdown, stage,
  startDate, endDate | null, repositoryUrl | null, demoUrl | null, featured
}
404 → ProblemDetail, code RESOURCE_NOT_FOUND (slug inconnu, projet DRAFT ou ARCHIVED : réponse identique)
```

Règles propres à ces contrats :

* seuls les projets `PUBLISHED` existent pour l’API publique (invariant 11, D-U) : ils sont seuls listés et comptés dans `totalElements` ;
* ordre fixe : `displayOrder` croissant, date de début décroissante, puis identifiant (D-V) ;
* `stage` vaut `IN_PROGRESS` si et seulement si `endDate` est `null` (invariant 21, D-T) ;
* ni `id`, ni `displayOrder`, ni `visibility` (D-W) ; la liste n’inclut pas `descriptionMarkdown` ;
* technologies, couverture et captures arriveront aux étapes 18 et 27 ;
* contrats vérifiés par `PublicProjectControllerTest` et `PublicProjectIT`.

Chaque module introduit ses routes lors de son étape d’implémentation.

---

# 31. Ressources administratives prévues

Les principales ressources administratives sont conceptuellement :

```text
/api/admin/profile

/api/admin/projects

/api/admin/publications

/api/admin/series

/api/admin/categories

/api/admin/tags

/api/admin/media

/api/admin/contact-messages
```

Les routes d’authentification seront définies lors de l’implémentation du module `security`.

---

# 32. Règles ArchUnit associées

Les conventions Java importantes sont protégées automatiquement par `ApiConventionsTest` :

```text
toute classe terminant par Controller → réside dans ..web.controller..
tout Controller                       → commence par Public ou Admin
toute entité JPA (@Entity)            → réside dans ..infrastructure.persistence.jpa.entity..
```

Ces règles complètent celles de `docs/04-architecture-backend.md` §12.

Le but est que les conventions importantes provoquent :

```text
BUILD FAILURE
```

lorsqu’elles sont violées.

---

# 33. Pagination partagée

Le package :

```text
com.scalke.portfolio.backend.shared.api
```

contient les contrats HTTP transversaux.

Structure en place depuis l’étape 17 (D-V) :

```text
shared/
├── api/
│   ├── PageResponse.java     contrat HTTP ; PageResponse.from(PageResult)
│   └── ApiPaging.java        PUBLIC_PAGE_SIZE = 10, ADMIN_PAGE_SIZE = 20, MAX_PAGE_SIZE = 100
└── domain/model/
    ├── PageQuery.java        demande de page passée au port (page, size)
    └── PageResult.java       page renvoyée par le port (content, page, size, totalElements)
```

`PageResponse<T>` constitue le contrat unique des collections paginées.

Chaîne d’une lecture paginée :

```text
HTTP ?page=&size=
  → Pageable (Spring Data Web : @PageableDefault(size = ApiPaging.PUBLIC_PAGE_SIZE ou ADMIN_PAGE_SIZE))
  → PageQuery                                  (contrôleur)
  → cas d’usage → port → adaptateur JPA        (PageRequest + tri fixe, puis PageResult)
  → PageResult.map(XxxResponse::from)
  → PageResponse.from(...)
```

Règles :

* la taille maximale est appliquée à la résolution HTTP par `spring.data.web.pageable.max-page-size: 100` (`application.yaml`) : une taille supérieure est ramenée à 100, une page négative à 0 (comportement Spring Data) ; `PublicProjectControllerTest` vérifie que cette valeur reste égale à `ApiPaging.MAX_PAGE_SIZE` ;
* un port ne reçoit jamais `Pageable` et ne renvoie jamais `Page` : le domaine ne dépend pas de Spring (ADR 0001) ;
* le tri est fixé par le port tant qu’aucun tri client n’est nécessaire ; le paramètre `sort` est alors ignoré (§15).

---

# 34. Exemple de flux réussi

```text
GET /api/public/publications
    ?page=0
    &size=10
    &sort=publishedAt,desc

        ↓

Spring Security
/api/public/** autorisé anonymement

        ↓

PublicPublicationController

        ↓

Pageable
page = 0
size = 10
publishedAt DESC

        ↓

PublicationQueryService

        ↓

règles de visibilité publique

        ↓

PublicationRepository

        ↓

PostgreSQL

        ↓

Page<Publication>

        ↓

Page.map(PublicationSummaryResponse::from)

        ↓

PageResponse.from(...)

        ↓

200 application/json
```

---

# 35. Exemple de flux d’erreur métier

```text
POST /api/admin/publications

        ↓

AdminPublicationController

        ↓

validation du DTO

        ↓

PublicationService

        ↓

slug déjà utilisé

        ↓

exception métier

        ↓

gestionnaire global d’erreurs

        ↓

ProblemDetail
status = 409
code = SLUG_ALREADY_USED

        ↓

409 application/problem+json
```

La gestion globale de cette chaîne est implémentée à l’étape suivante.

---

# 36. Conventions à ne pas introduire

La V1 n’introduit pas :

```text
GraphQL

HATEOAS

JSON:API

enveloppe globale success/data

version /api/v1 sans besoin

DTO générique unique pour public et admin

exposition directe des entités JPA

pagination 1-based

codes HTTP métier inventés

format d’erreur propriétaire
```

Ces fonctionnalités ne sont pas interdites par principe.

Elles sont simplement inutiles pour le périmètre actuel.

---

# 37. Résumé des décisions

| Réf | Décision                                                                               |
| --- | -------------------------------------------------------------------------------------- |
| C01 | Routes séparées en `/api/public/**` et `/api/admin/**`                                 |
| C02 | Ressources au pluriel et noms composés en `kebab-case`                                 |
| C03 | Slug pour les ressources publiques, identifiant technique opaque pour l’administration |
| C04 | Verbes et codes HTTP standards                                                         |
| C05 | Transitions métier importantes exposées explicitement                                  |
| C06 | Erreurs au format RFC 9457 avec extension `code`                                       |
| C07 | Pagination 0-based, 10 public, 20 admin, maximum 100                                   |
| C08 | Aucune enveloppe globale de succès                                                     |
| C09 | JSON `lowerCamelCase`, ISO-8601, enums en majuscules                                   |
| C10 | Champs optionnels présents avec `null`                                                 |
| C11 | DTO en records et contrôleurs préfixés `Public` / `Admin`                              |


### Codes d’erreur actuellement implémentés

| Code                             | HTTP | Usage                                         |
| -------------------------------- | ---: | --------------------------------------------- |
| `RESOURCE_NOT_FOUND`             |  404 | Ressource inexistante ou non accessible       |
| `VALIDATION_FAILED`              |  400 | Échec de validation des champs                |
| `MALFORMED_REQUEST`              |  400 | Requête HTTP ou JSON invalide                 |
| `INTERNAL_ERROR`                 |  500 | Erreur serveur inattendue                     |
| `SLUG_ALREADY_USED`              |  409 | Slug déjà utilisé                             |
| `INVALID_PUBLICATION_TRANSITION` |  409 | Transition d’état de publication interdite    |
| `SERIES_POSITION_ALREADY_USED`   |  409 | Position déjà occupée dans une série          |
| `NEWS_CANNOT_JOIN_SERIES`        |  409 | Une `NEWS` ne peut pas appartenir à une série |
| `MEDIA_STILL_REFERENCED`         |  409 | Média encore référencé                        |
| `UNSUPPORTED_MEDIA_FORMAT`       |  409 | Format de média non accepté                   |
---

# 38. Règles fondamentales

```text
API publique
→ ne révèle que les contenus publiquement visibles

API admin
→ manipule les ressources par identifiant technique

DTO
→ contrat HTTP

entité JPA
→ détail interne

ProblemDetail
→ format unique des erreurs

code
→ identifiant stable de l’erreur

detail
→ message destiné à l’humain

PageResponse
→ contrat unique de pagination

slug
→ identifiant public lisible

HTTP status
→ porte le résultat de l’opération
```

---

# 39. Documents liés

Ce document doit rester cohérent avec :

```text
docs/01-perimetre-v1.md
docs/02-modele-metier.md
docs/04-architecture-backend.md
docs/conventions-git.md
```

Toute modification importante du contrat API doit mettre à jour ce document avant ou en même temps que l’implémentation concernée.

---

# 40. Résultat

Le backend possède désormais un contrat HTTP explicite et commun à tous les modules.

Les futurs contrôleurs disposent déjà de règles pour :

```text
routes
identifiants
verbes HTTP
status HTTP
DTO
erreurs
pagination
dates
JSON
noms Java
```

Un nouveau module ne doit donc pas inventer ses propres conventions.

L’API reste cohérente, prévisible et indépendante des détails internes de persistence.
