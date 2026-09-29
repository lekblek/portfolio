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

État d’implémentation (étape 32, D-CK, D-CL) : règle appliquée par `SecurityConfiguration` ; exceptions explicites : `/api/actuator/health` et, en profil `dev` seulement, la documentation OpenAPI (`/api/v3/api-docs`, `/api/swagger-ui/index.html`). Sans authentification, une route d’administration ou non déclarée donne 401 `AUTHENTICATION_REQUIRED` ; authentifié, une route non déclarée donne 403 `ACCESS_DENIED` ; un chemin ambigu (point-virgule, double barre oblique) donne 400 `MALFORMED_REQUEST`. Aucune session n’est créée pour un visiteur anonyme. Connexion et compte administrateur : étapes 33 et 34.

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

Format : minuscules ASCII, chiffres et tirets simples (`^[a-z0-9]+(-[a-z0-9]+)*$`), 160 caractères au plus. Une valeur d’URL qui ne respecte pas ce format reçoit la même 404 qu’un slug inconnu, sans requête en base (D-BB). Changer le slug d’une publication déjà publiée est refusé avec `SLUG_LOCKED` (409, D-BC).

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

slug d’un contenu déjà publié

position déjà occupée dans une série

tentative d’ajouter une NEWS à une série

média encore référencé

transition éditoriale interdite
```

---

## 413 — Content Too Large et 415 — Unsupported Media Type

Utilisés pour un fichier envoyé (D-BS) : `MEDIA_TOO_LARGE` (413) au-delà de la taille permise pour son format, `UNSUPPORTED_MEDIA_FORMAT` (415) si son contenu n’est pas un format accepté (PNG, JPEG, WebP, PDF, reconnus par signature).

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

Depuis D-DB, chaque réponse (succès ou erreur, y compris un refus de sécurité) porte l’en-tête `X-Request-Id` ; une 500 `INTERNAL_ERROR` ajoute l’extension `requestId`, même valeur, à citer pour retrouver l’incident dans les journaux. Un `X-Request-Id` reçu du mandataire inverse est repris s’il ne contient que `[A-Za-z0-9._-]` (64 caractères au plus) ; sinon un identifiant aléatoire le remplace.

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

NEWS_CANNOT_JOIN_SERIES
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
/api/public/publications?category=spring-boot       (implémenté à l’étape 20, avec ?tag=)

/api/public/projects?technology=java        (implémenté à l’étape 18)

/api/public/search?q=postgresql
```

Un filtre sur un **vocabulaire ouvert** (slug de technologie, de catégorie, de tag) dont la valeur ne correspond à rien renvoie une page vide, jamais une erreur. Un filtre sur un **ensemble fermé** du contrat (énumération, ex. `type=ARTICLE|NEWS`) refuse toute autre valeur : 400 `MALFORMED_REQUEST` (D-AC, D-AK).

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
  "status": "SCHEDULED",
  "publishedAt": "2026-10-01T09:00:00Z"
}
```

Pour toute autre cible, `publishedAt` est absent : `{ "status": "PUBLISHED" }`.

Cette opération peut échouer avec :

```text
409 Conflict
```

si la transition viole un invariant.

État d’implémentation (étape 21, D-AU) : les règles de transition sont dans le domaine (`Publication.transitionTo`) et le cas d’usage `ChangePublicationStatusUseCase` existe ; la route ci-dessus est exposée depuis l’étape 36.3, derrière l’authentification (D-CU). Le champ `publishedAt` du corps n’est accepté que pour `SCHEDULED` (date strictement future) ; toute transition refusée donne 409 `INVALID_PUBLICATION_TRANSITION`. Table des transitions : `02-modele-metier.md` §15.

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

Exemple d’organisation (structure de l’ADR 0001 ; fichiers `Admin*` et `*Request` réels depuis l’étape 36, par exemple `publication.web.dto`) :

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

Précision de l’étape 19 (D-AJ) : pour les publications, `seoTitle` et `seoDescription` figurent aussi dans le **détail public**, car le rendu serveur Angular en a besoin pour les balises `<title>` et `<meta>`. `status`, `createdAt` et `updatedAt` restent réservés à l’administration.

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

Implémentation (étapes 19 à 21, D-AG, D-AH, D-AR, D-AY) : pour les lectures, la règle est écrite une seule fois en SQL (`PublicationSpecifications.visibleAt`) et combinée aux filtres ; le domaine en possède une version Java (`Publication.isVisibleAt`, utilisée par les transitions), et un test de concordance garantit que les deux désignent les mêmes publications ; « maintenant » vient du bean `Clock` (`shared.infrastructure.ClockConfiguration`), remplacé par une horloge fixe dans les tests d’intégration.

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
/api/public/publications/{slug}/series

/api/public/media/{storageKey}

/api/public/categories
/api/public/tags

/api/public/search

/api/public/contact-messages
```

La présence dans cette liste ne signifie pas que tous les endpoints doivent être créés immédiatement.

## Contrats implémentés

Le contrat OpenAPI généré par springdoc est versionné dans [`docs/api/openapi.json`](api/openapi.json) (D-DG) : `OpenApiContractIT` échoue dès que l'API diffère du fichier. Après un changement voulu, régénérer avec `./mvnw verify -Dit.test=OpenApiContractIT -Dopenapi.update=true` et relire le diff du fichier avec le code. Les sections suivantes décrivent les règles que le schéma ne dit pas (visibilité, ordre, effets).

### `GET /api/public/profile` (étapes 14 à 16 et 27.3)

```text
200 → ProfileResponse
{
  displayName, professionalTitle, shortBio,
  aboutMarkdown | null, publicLocation | null, publicEmail | null,
  avatar { url, width, height, altText | null } | null,
  cv { url, sizeBytes } | null,
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
* `avatar` : image publique, même forme que les images des projets (D-BW) ; `cv` : document PDF, `url` vers `GET /api/public/media/{storageKey}` et taille en octets (D-BX) ; `null` s’ils sont absents ;
* contrat vérifié par `PublicProfileIT` sur la sérialisation réelle.

### `GET /api/public/projects` (étapes 17 et 18)

```text
?page=0&size=10          page 0-based ; size par défaut 10, plafonnée à 100 ; sort ignoré (ordre fixe)
?technology=<slug>       facultatif ; seuls les projets utilisant cette technologie ; slug inconnu → page vide
200 → PageResponse<ProjectSummaryResponse>
{
  content[] { title, slug, shortDescription, stage, startDate, endDate | null, featured,
              cover { url, width, height, altText | null } | null,
              technologies[] { name, slug } },
  page, size, totalElements, totalPages, first, last
}
```

### `GET /api/public/projects/{slug}` (étapes 17, 18 et 27.2)

```text
200 → ProjectResponse
{
  title, slug, shortDescription, descriptionMarkdown, stage,
  startDate, endDate | null, repositoryUrl | null, demoUrl | null, featured,
  cover { url, width, height, altText | null } | null,
  screenshots[] { image { url, width, height, altText | null }, caption | null },
  technologies[] { name, slug }
}
404 → ProblemDetail, code RESOURCE_NOT_FOUND (slug inconnu ou mal formé, projet DRAFT ou ARCHIVED : réponse identique)
```

Règles propres à ces contrats :

* seuls les projets `PUBLISHED` existent pour l’API publique (invariant 11, D-U) : ils sont seuls listés et comptés dans `totalElements` ;
* ordre fixe : `displayOrder` croissant, date de début décroissante, puis identifiant (D-V) ;
* `stage` vaut `IN_PROGRESS` si et seulement si `endDate` est `null` (invariant 21, D-T) ;
* ni `id`, ni `displayOrder`, ni `visibility` (D-W) ; la liste n’inclut pas `descriptionMarkdown` ;
* `technologies` : dans l’ordre du vocabulaire, `[]` si aucune ; le `slug` sert de valeur au filtre `technology` (D-AC) ; le filtre restreint les projets, pas la liste de leurs technologies ;
* images (D-BW) : `{ url, width, height, altText }`, forme commune à toutes les images publiques ; `url` pointe vers `GET /api/public/media/{storageKey}` ; `width` et `height` en pixels réservent la place de l’image avant son chargement ; `altText` vaut `null` si aucun texte n’a été saisi ;
* `cover` : `null` sans couverture, dans la liste et le détail ; `screenshots` : détail seulement, dans leur ordre d’affichage, `[]` si aucune ;
* contrats vérifiés par `PublicProjectControllerTest` et `PublicProjectIT`.

### `GET /api/public/publications` (étapes 19, 20 et 27.4)

```text
?page=0&size=10          page 0-based ; size par défaut 10, plafonnée à 100 ; sort ignoré (ordre fixe)
?type=ARTICLE|NEWS       facultatif ; toute autre valeur → 400 MALFORMED_REQUEST
?category=<slug>         facultatif ; slug inconnu → page vide
?tag=<slug>              facultatif ; slug inconnu → page vide ; filtres cumulables (ET)
200 → PageResponse<PublicationSummaryResponse>
{
  content[] { type, title, slug, summary, publishedAt, featured, readingTimeMinutes,
              cover { url, width, height, altText | null } | null,
              category { name, slug } | null, tags[] { name, slug } },
  page, size, totalElements, totalPages, first, last
}
```

### `GET /api/public/publications/{slug}` (étapes 19, 20 et 27.4)

```text
200 → PublicationResponse
{
  type, title, slug, summary, contentMarkdown, publishedAt, featured, readingTimeMinutes,
  cover { url, width, height, altText | null } | null,
  category { name, slug } | null, tags[] { name, slug },
  seoTitle | null, seoDescription | null
}
404 → ProblemDetail, code RESOURCE_NOT_FOUND (slug inconnu ou mal formé, DRAFT, IN_REVIEW, ARCHIVED,
                                              SCHEDULED dont la date n'est pas passée : réponse identique)
```

Règles propres à ces contrats :

* visibles : `PUBLISHED`, et `SCHEDULED` dont `publishedAt <= maintenant` (§29, D-AH) ; seules les publications visibles sont listées et comptées ;
* ordre fixe : `publishedAt` décroissant, puis identifiant décroissant (D-AK) ;
* `publishedAt` : instant ISO-8601 UTC (§26) ;
* `readingTimeMinutes` : calculé depuis le Markdown, 200 mots par minute, au moins 1 (D12) ;
* ni `id`, ni `status`, ni dates d’audit (D-AJ) ; la liste n’inclut ni le contenu ni les champs SEO ;
* `category` : `null` si non classée ; `tags` : triés par nom, `[]` si aucun ; les slugs servent de valeurs aux filtres (D-AP, D-AQ) ;
* `cover` : image publique (même forme que pour les projets, D-BW), `null` sans couverture (D-BY) ;
* contrats vérifiés par `PublicPublicationControllerTest` et `PublicPublicationIT`.

### `GET /api/public/series` (étapes 23 et 27.4)

```text
?page=0&size=10          page 0-based ; size par défaut 10, plafonnée à 100 ; sort ignoré (ordre fixe)
200 → PageResponse<SeriesSummaryResponse>
{
  content[] { title, slug, descriptionMarkdown, cover { url, width, height, altText | null } | null, chapterCount },
  page, size, totalElements, totalPages, first, last
}
```

### `GET /api/public/series/{slug}` (étapes 23 et 27.4)

```text
200 → SeriesResponse
{
  title, slug, descriptionMarkdown,
  cover { url, width, height, altText | null } | null,
  chapters[] { position, title, slug, summary, publishedAt, readingTimeMinutes }
}
404 → ProblemDetail, code RESOURCE_NOT_FOUND (slug inconnu ou mal formé, série sans article visible : réponse identique)
```

Règles propres à ces contrats :

* une série est publique si et seulement si au moins un de ses articles est visible (§29) ; seules ces séries sont listées et comptées (D-BG) ;
* ordre fixe : titre sans tenir compte de la casse, puis identifiant (D-BI) ;
* `chapterCount` et `chapters` ne comptent que les articles visibles ; `position` est leur rang dans la série à partir de 1, jamais la position stockée ;
* `chapters[].slug` mène à `GET /api/public/publications/{slug}` ;
* ni `id`, ni positions internes ; `cover` : image publique, `null` sans couverture (D-BY) ;
* contrats vérifiés par `PublicSeriesControllerTest` et `PublicSeriesIT`.

### `GET /api/public/publications/{slug}/series` (étape 24)

```text
200 → SeriesNavigationResponse
{
  series { title, slug },
  position, chapterCount,
  previous { position, title, slug } | null,
  next { position, title, slug } | null
}
404 → ProblemDetail, code RESOURCE_NOT_FOUND
      « Publication introuvable. »                        slug inconnu ou mal formé, publication invisible
      « Cette publication n'appartient à aucune série. »  publication visible hors série (NEWS comprises)
```

Règles propres à ce contrat :

* sous-ressource d’une publication servie par le module `series`, qui possède la relation (D-BL) ;
* positions et voisins calculés parmi les seuls articles visibles : un article masqué est sauté (D-BM) ;
* progression : `position` sur `chapterCount`, calculée par le client ;
* contrat vérifié par `PublicSeriesNavigationControllerTest` et `PublicSeriesIT`.

### `GET /api/public/media/{storageKey}` (étape 25)

```text
200 → contenu brut du fichier (pas de JSON)
      Content-Type             type MIME du format de la clé : image/png, image/jpeg, image/webp, application/pdf
      Content-Length           taille du fichier
      Cache-Control            max-age=31536000, public, immutable
      X-Content-Type-Options   nosniff
404 → ProblemDetail, code RESOURCE_NOT_FOUND « Média introuvable. » (clé inconnue ou mal formée : réponse identique)
```

Règles propres à ce contrat :

* `storageKey` : 32 caractères hexadécimaux et l’extension du format (`png`, `jpg`, `webp`, `pdf`) ; jamais un chemin (D-BP) ;
* une clé ne change jamais de contenu : un nouvel envoi crée une nouvelle clé, d’où le cache immuable ;
* un fichier est accessible à quiconque connaît sa clé, imprévisible ; les contenus publient ces adresses (`url` des images, depuis l’étape 27.2) ;
* contrat vérifié par `PublicMediaControllerTest` et `PublicMediaIT`.

### `GET /api/public/search` (étapes 28 et 29)

```text
?q=<texte>               obligatoire ; lu comme une recherche web : "expression", or, -exclusion ;
                         absent → 400 MALFORMED_REQUEST ; plus de 200 caractères → 400 VALIDATION_FAILED ;
                         vide ou blanc → page vide
?page=0&size=10          page 0-based ; size par défaut 10, plafonnée à 100 ; sort ignoré (ordre fixe)
200 → PageResponse<SearchResultResponse>
{
  content[] { type, title, slug, summary, publishedAt | null },
  page, size, totalElements, totalPages, first, last
}
400 → ProblemDetail, code MALFORMED_REQUEST, ou VALIDATION_FAILED avec errors[] { field: "q", message }
```

Règles propres à ce contrat :

* seuls les articles et news visibles (§29) et les projets `PUBLISHED` sont trouvés et comptés (D09, D-CC) ;
* `type` : `ARTICLE`, `NEWS` ou `PROJECT` ; le lien se construit avec `slug` : `/api/public/publications/{slug}` pour une publication, `/api/public/projects/{slug}` pour un projet ;
* `summary` : résumé d’une publication, description courte d’un projet ; `publishedAt` : instant ISO-8601 UTC (§26), `null` pour un projet ;
* ordre fixe : pertinence décroissante (titre et tags ou technologies, puis résumé ou description courte, puis contenu), puis publications avant projets, puis le plus récemment créé (D-CD) ;
* recherche insensible aux accents et à la casse, sur les racines des mots français (`développer` trouve `développement`), mots vides ignorés (D-BZ) ;
* ni identifiant ni pertinence exposés (D-CE) ;
* contrat vérifié par `PublicSearchControllerTest` et `PublicSearchIT`.

En-têtes de sécurité de toute réponse (D-CR) : `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Content-Security-Policy: frame-ancestors 'none'`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store` sauf quand la réponse fixe son propre cache (médias) ; HSTS sur HTTPS.

Chaque module introduit ses routes lors de son étape d’implémentation.

---

### Session de l’administrateur (étape 34)

```text
POST   /api/admin/session     { login, password } → 200 AdminSessionResponse { login, lastLoginAt }
                              401 INVALID_CREDENTIALS (identifiant inconnu, mauvais mot de passe, compte désactivé : même réponse)
                              429 TOO_MANY_LOGIN_ATTEMPTS + Retry-After (5 échecs en 15 minutes depuis la même adresse)
                              400 VALIDATION_FAILED (champ absent) ; 403 ACCESS_DENIED (jeton CSRF absent)
GET    /api/admin/session     200 AdminSessionResponse ; 401 AUTHENTICATION_REQUIRED sans session
DELETE /api/admin/session     204, session invalidée (jeton CSRF requis)
```

Règles propres à ce contrat (D-CO, D-CP, D-CQ) :

* session serveur, cookie `JSESSIONID` `HttpOnly`, `Secure`, `SameSite=Strict`, 30 minutes d’inactivité (D-CR) ; aucune session pour un visiteur anonyme ; identifiant de session renouvelé à la connexion ;
* CSRF : toute écriture envoie l’en-tête `X-XSRF-TOKEN` avec la valeur du cookie `XSRF-TOKEN`, déposé par les réponses de `/api/admin/**` et renouvelé à la connexion (comportement par défaut du `HttpClient` d’Angular) ;
* `lastLoginAt` : instant ISO-8601 UTC de la connexion en cours ;
* contrat vérifié par `AdminSessionIT`.

### Catégories et tags (étape 36.1)

```text
GET    /api/admin/categories        200 AdminCategoryResponse[] { id, name, slug, description | null }, par nom
POST   /api/admin/categories        { name, slug?, description? } → 201 + Location, AdminCategoryResponse
PUT    /api/admin/categories/{id}   { name, slug?, description? } → 200 AdminCategoryResponse
DELETE /api/admin/categories/{id}   204
GET    /api/admin/tags              200 AdminTagResponse[] { id, name, slug }, par nom
POST   /api/admin/tags              { name, slug? } → 201 + Location, AdminTagResponse
PUT    /api/admin/tags/{id}         { name, slug? } → 200 AdminTagResponse
DELETE /api/admin/tags/{id}         204
400 VALIDATION_FAILED   nom blanc ou sans lettre ni chiffre, trop long ; slug mal formé ou trop long
404 RESOURCE_NOT_FOUND  identifiant inconnu
409 NAME_ALREADY_USED   nom déjà pris, casse ignorée ; 409 TERM_STILL_USED à la suppression d'un terme utilisé
```

Règles propres à ces contrats (D-CS) :

* session de l’administrateur et jeton CSRF obligatoires (401 et 403 sinon) ;
* `slug` facultatif : généré depuis le nom à la création, conservé à la modification ; s’il est déjà pris, le premier suffixe libre est ajouté (`backend-2`) ;
* bornes : catégorie `name` 80, `slug` 80, `description` 500 ; tag `name` 60, `slug` 60 ;
* contrats vérifiés par `AdminCategoryIT` et `AdminTagIT`.

### Médias (étape 36.2)

```text
POST   /api/admin/media        multipart/form-data : file, altText? → 201 + Location, AdminMediaResponse
GET    /api/admin/media        200 PageResponse<AdminMediaResponse>, 20 par page, les plus récents d'abord
GET    /api/admin/media/{id}   200 AdminMediaResponse
PATCH  /api/admin/media/{id}   { altText } → 200 AdminMediaResponse
DELETE /api/admin/media/{id}   204 (entrée et fichier supprimés)

AdminMediaResponse { id, url, originalName, format (PNG | JPEG | WEBP | PDF), mimeType, sizeBytes,
                     width | null, height | null, altText | null, createdAt }

400 VALIDATION_FAILED         altText de plus de 300 caractères ; 400 MALFORMED_REQUEST sans partie file
404 RESOURCE_NOT_FOUND        identifiant inconnu
409 MEDIA_STILL_REFERENCED    suppression d'un média utilisé par un contenu
413 MEDIA_TOO_LARGE           image de plus de 5 Mio, PDF de plus de 10 Mio, requête de plus de 11 Mio
415 UNSUPPORTED_MEDIA_FORMAT  contenu qui n'est ni PNG, ni JPEG, ni WebP, ni PDF
```

Règles propres à ces contrats (D-CT) :

* session de l’administrateur et jeton CSRF obligatoires (401 et 403 sinon), comme toute l’administration (D-CS) ;
* le format est reconnu par le contenu, jamais par le nom ni par le type annoncé (D-BR) ; `originalName` est le dernier segment du nom envoyé ;
* `url` est l’adresse publique du fichier (`GET /api/public/media/{storageKey}`) ; un contenu référence le média par son `id` ;
* seul le texte alternatif se modifie : remplacer un fichier, c’est envoyer un nouveau média ; `altText` vide ou absent → `null` ;
* au-delà de 12 Mio, Tomcat coupe la connexion au lieu de répondre 413 : l’interface vérifie la taille avant l’envoi ;
* contrats vérifiés par `AdminMediaIT` ; limite de la requête sur un vrai serveur par `HttpServerSecurityIT`.

### Publications (étape 36.3)

```text
GET    /api/admin/publications              200 PageResponse<AdminPublicationSummaryResponse>, 20 par page,
                                            tous statuts, les dernières modifiées d'abord
GET    /api/admin/publications/{id}         200 AdminPublicationResponse
POST   /api/admin/publications              { type, title, slug?, summary, contentMarkdown, featured?, categoryId?,
                                              tagIds?, coverMediaId?, seoTitle?, seoDescription? }
                                            → 201 + Location, AdminPublicationResponse (brouillon)
PUT    /api/admin/publications/{id}         la même saisie sans type → 200 AdminPublicationResponse
POST   /api/admin/publications/{id}/status  { status, publishedAt? } → 200 AdminPublicationResponse (§17)

AdminPublicationResponse { id, type, title, slug, slugLocked, summary, contentMarkdown, status, publishedAt | null,
                           featured, categoryId | null, tagIds, coverMediaId | null, seoTitle | null,
                           seoDescription | null, createdAt, updatedAt }
AdminPublicationSummaryResponse { id, type, title, slug, status, publishedAt | null, featured, updatedAt }

400 VALIDATION_FAILED               titre vide ou sans lettre ni chiffre, bornes dépassées, slug mal formé, type ou
                                    statut absent ; catégorie, tag ou couverture inconnus, couverture PDF (champ)
400 MALFORMED_REQUEST               JSON illisible, type ou statut inexistant
404 RESOURCE_NOT_FOUND              identifiant inconnu
409 SLUG_LOCKED                     nouveau slug d'une publication déjà publiée (D11)
409 INVALID_PUBLICATION_TRANSITION  transition interdite ou date de planification non future
```

Règles propres à ces contrats (D-CU) :

* session de l’administrateur et jeton CSRF obligatoires (401 et 403 sinon) ;
* une publication naît brouillon ; son type ne change plus ; son statut ne change que par la route `/status` ;
* `slug` facultatif : généré depuis le titre à la création, conservé à la modification ; s’il est pris, le premier suffixe libre est ajouté ; après la première publication, un slug différent est refusé ;
* `status` est le statut observable : une publication planifiée dont la date est passée est `PUBLISHED` ; `slugLocked` dit si le slug peut encore changer ;
* bornes : `title` 160, `summary` 500, `contentMarkdown` 100 000 caractères (vide permis), `seoTitle` 120, `seoDescription` 300 ; champs SEO vides → `null`, `tagIds` absent → aucun tag, `featured` absent → `false` ;
* pas de suppression : l’archivage (`ARCHIVED`) retire une publication du site ;
* contrats vérifiés par `AdminPublicationIT`.

### Séries (étape 36.4)

```text
GET    /api/admin/series                 200 PageResponse<AdminSeriesSummaryResponse> { id, title, slug, chapterCount },
                                         20 par page, par titre
GET    /api/admin/series/{id}            200 AdminSeriesResponse
POST   /api/admin/series                 { title, slug?, descriptionMarkdown, coverMediaId? } → 201 + Location
PUT    /api/admin/series/{id}            même saisie → 200 AdminSeriesResponse (chapitres inchangés)
PUT    /api/admin/series/{id}/chapters   { publicationIds } → 200 AdminSeriesResponse (liste remplacée)

AdminSeriesResponse { id, title, slug, slugLocked, descriptionMarkdown, coverMediaId | null,
                      chapters: [{ position, publicationId, title, slug, status }] }

400 VALIDATION_FAILED          titre vide ou sans lettre ni chiffre, bornes dépassées, slug mal formé ;
                               article répété ou inconnu (publicationIds), couverture inconnue ou PDF (coverMediaId)
404 RESOURCE_NOT_FOUND         identifiant inconnu
409 SLUG_LOCKED                nouveau slug d'une série dont un article a déjà été public
409 NEWS_CANNOT_JOIN_SERIES    une actualité parmi les chapitres
409 ARTICLE_ALREADY_IN_SERIES  un article déjà rangé dans une autre série
```

Règles propres à ces contrats (D-CV) :

* session de l’administrateur et jeton CSRF obligatoires (401 et 403 sinon) ;
* une série naît sans chapitre, donc invisible sur le site tant qu’aucun de ses articles n’est visible (D-BG) ;
* `publicationIds` donne l’ordre de lecture complet : la liste remplace l’ancienne, les positions valent 1, 2, … ; une liste vide retire tous les chapitres ;
* `status` d’un chapitre est le statut observable de son article ; `slugLocked` dit si le slug peut encore changer ;
* bornes : `title` 160, `descriptionMarkdown` 10 000 caractères (vide permis) ;
* pas de suppression ;
* contrats vérifiés par `AdminSeriesIT`.

### Technologies (étape 36.5)

```text
GET    /api/admin/technologies        200 AdminTechnologyResponse[] { id, name, slug, displayOrder }, ordre du vocabulaire
POST   /api/admin/technologies        { name, slug?, displayOrder? } → 201 + Location, AdminTechnologyResponse
PUT    /api/admin/technologies/{id}   { name, slug?, displayOrder? } → 200 AdminTechnologyResponse
DELETE /api/admin/technologies/{id}   204
400 VALIDATION_FAILED   nom vide ou sans lettre ni chiffre, plus de 80 caractères ; slug mal formé ; ordre négatif
404 RESOURCE_NOT_FOUND  identifiant inconnu
409 NAME_ALREADY_USED   nom déjà pris, casse ignorée ; 409 TERM_STILL_USED à la suppression d'une technologie utilisée
```

Mêmes règles que les tags (D-CS, D-CW) ; `displayOrder` absent → 0 ; contrats vérifiés par `AdminTechnologyIT`.

### Projets (étape 36.5)

```text
GET    /api/admin/projects        200 PageResponse<AdminProjectSummaryResponse>
                                  { id, title, slug, visibility, stage, featured, displayOrder }, 20 par page,
                                  toutes visibilités, ordre d'affichage public
GET    /api/admin/projects/{id}   200 AdminProjectResponse
POST   /api/admin/projects        SaveProjectRequest → 201 + Location, AdminProjectResponse
PUT    /api/admin/projects/{id}   SaveProjectRequest → 200 AdminProjectResponse

SaveProjectRequest { title, slug?, shortDescription, descriptionMarkdown, stage, visibility, startDate, endDate?,
                     repositoryUrl?, demoUrl?, featured?, displayOrder?, technologyIds?, coverMediaId?,
                     screenshots?: [{ mediaId, caption? }] }
AdminProjectResponse { id, title, slug, slugLocked, shortDescription, descriptionMarkdown, stage, visibility,
                       startDate, endDate | null, repositoryUrl | null, demoUrl | null, featured, displayOrder,
                       technologyIds, coverMediaId | null, screenshots: [{ mediaId, caption | null }] }

400 VALIDATION_FAILED  bornes, slug mal formé, adresse qui n'est pas http(s), ordre négatif ; fin avant début
                       (endDate), stage incohérent avec la date de fin (stage), technologie inconnue (technologyIds),
                       couverture ou capture inconnue ou PDF, capture répétée (coverMediaId, screenshots)
404 RESOURCE_NOT_FOUND identifiant inconnu
409 SLUG_LOCKED        nouveau slug d'un projet déjà publié (D11)
```

Règles propres à ces contrats (D-CX) :

* session de l’administrateur et jeton CSRF obligatoires (401 et 403 sinon) ;
* une requête porte toute la saisie, visibilité comprise ; technologies et captures sont remplacées d’un bloc ; les captures sont affichées dans l’ordre de la liste ;
* `IN_PROGRESS` exige l’absence de `endDate`, `COMPLETED` sa présence (invariant 21) ;
* `slug` facultatif : généré depuis le titre à la création, conservé à la modification ; suffixé s’il est pris ; figé dès que le projet a été publié, même archivé ensuite (`slugLocked`) ;
* bornes : `title` 160, `shortDescription` 500, `descriptionMarkdown` 100 000 caractères, adresses 2 048, `caption` 300 ;
* pas de suppression : l’archivage retire un projet du site ;
* contrats vérifiés par `AdminProjectIT`.

### Profil (étape 36.6)

```text
GET /api/admin/profile   200 AdminProfileResponse ; 404 RESOURCE_NOT_FOUND tant qu'aucun profil n'est enregistré
PUT /api/admin/profile   SaveProfileRequest → 200 AdminProfileResponse (création ou remplacement complet)

SaveProfileRequest = AdminProfileResponse {
  displayName, professionalTitle, shortBio, aboutMarkdown?, publicLocation?, publicEmail?,
  avatarMediaId?, cvMediaId?,
  links?:          [{ label, url }],
  skills?:         [{ name, category }],
  experiences?:    [{ organization, title, location, startDate, endDate?, description }],
  educations?:     [{ institution, degree, field, location, startDate, endDate?, description }],
  certifications?: [{ name, issuer, issuedAt, expiresAt?, credentialUrl? }]
}

400 VALIDATION_FAILED  champ obligatoire vide, bornes des colonnes, textes longs de plus de 10 000 caractères,
                       adresse qui n'est pas http(s), courriel mal formé ; fin avant début (experiences[i].endDate,
                       educations[i].endDate), expiration avant délivrance (certifications[i].expiresAt),
                       compétence répétée, casse ignorée (skills), avatar qui n'est pas une image (avatarMediaId),
                       CV qui n'est pas un PDF (cvMediaId)
```

Règles propres à ce contrat (D-CY) :

* session de l’administrateur et jeton CSRF obligatoires (401 et 403 sinon) ;
* le profil est unique : `PUT` le crée s’il n’existe pas, sinon remplace tout, collections comprises ; une collection absente devient vide ;
* chaque collection est affichée dans l’ordre de sa liste ; les lignes n’ont pas d’identifiant ;
* la réponse a la forme de la requête : un formulaire peut renvoyer ce qu’il a lu ;
* contrat vérifié par `AdminProfileIT`.

### Messages de contact (étape 36.7)

```text
GET  /api/admin/contact-messages[?status=]   200 PageResponse<AdminContactMessageSummaryResponse>
                                             { id, name, email, subject, status, createdAt }, 20 par page,
                                             les plus récents d'abord
GET  /api/admin/contact-messages/{id}        200 AdminContactMessageResponse
                                             { id, name, email, subject, message, status, createdAt, updatedAt }
POST /api/admin/contact-messages/{id}/status { status } → 200 AdminContactMessageResponse

400 MALFORMED_REQUEST                   statut inexistant (filtre ou corps) ; 400 VALIDATION_FAILED statut absent
404 RESOURCE_NOT_FOUND                  identifiant inconnu
409 INVALID_CONTACT_MESSAGE_TRANSITION  statut qui n'est pas après le statut actuel (NEW → READ → PROCESSED → ARCHIVED)
```

Règles propres à ces contrats (D-CZ) :

* session de l’administrateur et jeton CSRF obligatoires (401 et 403 sinon) ;
* lire un message ne change pas son statut ; le statut avance par sa route, en sautant éventuellement des étapes, jamais en arrière (invariant 29) ;
* pas de suppression ;
* contrats vérifiés par `AdminContactMessageIT`.

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

Flux réel depuis les étapes 19 et 20 (la sécurité s’ajoutera en tête à l’étape 32) :

```text
GET /api/public/publications?page=0&size=10&tag=java

        ↓

PublicPublicationController
  Pageable (taille bornée) → PageQuery ; paramètres → PublicationCriteria

        ↓

ListVisiblePublicationsUseCase                      (transaction en lecture)
  maintenant = Clock ; slugs → identifiants via TaxonomyQueryService

        ↓

PublicationRepository (port) → PublicationRepositoryAdapter
  Specification : visibleAt(maintenant) ∧ filtres ; tri publishedAt desc, id desc

        ↓

PostgreSQL → PageResult<Publication>

        ↓

VisiblePublicationAssembler (catégories, tags) → PageResult<VisiblePublication>

        ↓

map(PublicationSummaryResponse::from) → PageResponse.from(...)

        ↓

200 application/json
```

Le paramètre `sort` n’est pas pris en compte : l’ordre est fixe (D-V).

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
| `AUTHENTICATION_REQUIRED`        |  401 | Authentification absente ou invalide (D-CL)   |
| `INVALID_CREDENTIALS`            |  401 | Connexion refusée, cause jamais précisée (D-CO) |
| `TOO_MANY_LOGIN_ATTEMPTS`        |  429 | Trop d’échecs de connexion, `Retry-After` (D-CQ) |
| `ACCESS_DENIED`                  |  403 | Accès refusé, dont CSRF (D-CL)                |
| `VALIDATION_FAILED`              |  400 | Échec de validation des champs                |
| `MALFORMED_REQUEST`              |  400 | Requête HTTP ou JSON invalide                 |
| `INTERNAL_ERROR`                 |  500 | Erreur serveur inattendue                     |
| `SLUG_ALREADY_USED`              |  409 | Slug déjà utilisé                             |
| `NAME_ALREADY_USED`              |  409 | Nom de terme déjà pris, casse ignorée (D-CS)  |
| `TERM_STILL_USED`                |  409 | Terme encore utilisé par une publication (D-CS) |
| `SLUG_LOCKED`                    |  409 | Slug d’un contenu déjà publié (D11)           |
| `INVALID_PUBLICATION_TRANSITION` |  409 | Transition d’état de publication interdite    |
| `NEWS_CANNOT_JOIN_SERIES`        |  409 | Une `NEWS` ne peut pas appartenir à une série |
| `ARTICLE_ALREADY_IN_SERIES`      |  409 | Article déjà rangé dans une autre série (D-CV) |
| `MEDIA_STILL_REFERENCED`         |  409 | Média encore référencé                        |
| `UNSUPPORTED_MEDIA_FORMAT`       |  415 | Format de média non accepté                   |
| `MEDIA_TOO_LARGE`                |  413 | Fichier trop volumineux (format ou requête)   |
| `INVALID_CONTACT_MESSAGE_TRANSITION` | 409 | Retour en arrière dans le cycle d’un message de contact (D-CH) ; levé par le cas d’usage, route à l’étape 36 |
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
