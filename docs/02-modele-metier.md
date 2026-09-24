# Modèle métier V1 — Portfolio

## 1. Objectif

Ce document décrit le modèle métier conceptuel de la V1.

Il définit :

* les concepts ;
* leurs responsabilités ;
* leurs relations ;
* leurs cardinalités ;
* leurs cycles de vie ;
* leurs invariants.

Il ne définit pas encore :

* les tables PostgreSQL ;
* les annotations JPA ;
* les DTO ;
* les endpoints REST ;
* les migrations Flyway.

---

# 2. Vue globale

```text
Profile
├── Skill
├── Experience
├── Education
├── Certification
└── ProfessionalLink

Project
├── Technology
└── ProjectScreenshot

Publication
├── Category
├── Tag
└── Media

Series
└── SeriesItem

Media

ContactMessage

AdminAccount
```

---

# 3. Racines d'agrégat

Racines principales :

```text
Profile
Project
Publication
Series
Category
Tag
Technology
Media
ContactMessage
AdminAccount
```

Éléments composés :

```text
Skill
Experience
Education
Certification
ProfessionalLink
ProjectScreenshot
SeriesItem
```

Les éléments composés ne possèdent pas d'API métier autonome.

---

# 4. Profile

`Profile` représente le propriétaire professionnel du portfolio.

La V1 contient un seul profil.

```text
Profile
├── displayName
├── professionalTitle
├── shortBio
├── aboutMarkdown
├── publicLocation
├── publicEmail
├── avatarMedia
└── cvMedia
```

Relations :

```text
Profile 1 → 0..* Skill
Profile 1 → 0..* Experience
Profile 1 → 0..* Education
Profile 1 → 0..* Certification
Profile 1 → 0..* ProfessionalLink
```

---

# 5. Skill

```text
Skill
├── name
├── category
└── displayOrder
```

Aucun niveau `BEGINNER/EXPERT` n'est imposé dans la V1.

---

# 6. Experience

```text
Experience
├── organization
├── title
├── location
├── startDate
├── endDate?
├── description
└── displayOrder
```

`endDate = null` signifie que l'expérience est en cours.

---

# 7. Education

```text
Education
├── institution
├── degree
├── field
├── location
├── startDate
├── endDate?
├── description
└── displayOrder
```

---

# 8. Certification

```text
Certification
├── name
├── issuer
├── issuedAt
├── expiresAt?
├── credentialUrl?
└── displayOrder
```

---

# 9. ProfessionalLink

```text
ProfessionalLink
├── label
├── url
└── displayOrder
```

Il est composé dans `Profile`.

Il n'existe pas indépendamment du profil.

---

# 10. Project

```text
Project
├── title
├── slug
├── shortDescription
├── descriptionMarkdown
├── stage
├── visibility
├── startDate
├── endDate?
├── repositoryUrl?
├── demoUrl?
├── coverMedia?
├── featured
└── displayOrder
```

## ProjectStage

```text
IN_PROGRESS
COMPLETED
```

`stage` est cohérent avec la période : `IN_PROGRESS` si et seulement si `endDate` est absente (invariant 21, ajouté à l’étape 17, D-T). Il reste indépendant de la visibilité (D19).

## ProjectVisibility

```text
DRAFT
PUBLISHED
ARCHIVED
```

Seul un projet `PUBLISHED` est exposé publiquement.

État d’implémentation (étapes 17 et 18) : tous les attributs ci-dessus et les technologies ; `coverMedia` et `ProjectScreenshot` arrivent avec le catalogue `Media` à l’étape 27 (D-AD). Le slug est unique et au format kebab-case, garanti par PostgreSQL (D-X).

---

# 11. Technology

`Technology` représente le vocabulaire technique utilisé par les projets.

```text
Technology
├── name
├── slug
└── displayOrder
```

Relation :

```text
Project N ↔ N Technology
```

Exemples :

```text
Java
Spring Boot
Angular
PostgreSQL
Docker
PyTorch
```

`Technology` est distinct de `Tag`.

État d’implémentation (étape 18) : module `project`, tables `technology` et `project_technology`. Les technologies d’un projet sont toujours présentées dans l’ordre du vocabulaire (`displayOrder`, puis nom) ; un projet n’a pas d’ordre propre pour ses technologies (D-Z).

---

# 12. ProjectScreenshot

```text
ProjectScreenshot
├── media
├── caption?
└── displayOrder
```

Relation :

```text
Project 1 → 0..* ProjectScreenshot
ProjectScreenshot N → 1 Media
```

`ProjectScreenshot` est composé dans `Project`.

La suppression d'une capture ne supprime pas automatiquement le `Media`.

---

# 13. Publication

Une publication représente soit :

```text
ARTICLE
```

soit :

```text
NEWS
```

Structure :

```text
Publication
├── type
├── title
├── slug
├── summary
├── contentMarkdown
├── coverMedia?
├── status
├── publishedAt?
├── category?
├── tags
├── featured
├── seoTitle?
├── seoDescription?
├── createdAt
└── updatedAt
```

Le Markdown est la source canonique.

Aucun `contentHtml` métier n'est nécessaire.

État d’implémentation (étapes 19 et 20) : tous les attributs ci-dessus sauf `coverMedia` (étape 27) ; le temps de lecture est calculé à partir du Markdown (D12, D-AJ). Le slug est unique pour l’ensemble des publications (D-AL).

---

# 14. PublicationType

```text
ARTICLE
NEWS
```

`ARTICLE` peut appartenir à une série.

`NEWS` ne peut jamais appartenir à une série.

---

# 15. PublicationStatus

```text
DRAFT
IN_REVIEW
SCHEDULED
PUBLISHED
ARCHIVED
```

Règles essentielles :

```text
DRAFT
→ jamais public

IN_REVIEW
→ jamais public

SCHEDULED
→ public seulement si publishedAt <= now

PUBLISHED
→ public

ARCHIVED
→ jamais public
```

La comparaison temporelle utilise une horloge fournie par l'application et non un appel dispersé à l'heure système.

État d’implémentation (étape 19) : la visibilité est appliquée par les lectures publiques avec l’horloge applicative (D-AG, D-AH). Les transitions entre statuts arrivent à l’étape 21.

---

# 16. Slugs

Les ressources suivantes possèdent un slug public :

```text
Project
Publication
Series
Category
Tag
Technology
```

Un slug est :

* compatible URL ;
* unique dans son espace ;
* validé côté serveur.

Après la première publication publique d'un contenu, son slug est considéré comme stable.

---

# 17. Category

```text
Category
├── name
├── slug
└── description?
```

Relation :

```text
Category 1 ← 0..* Publication
Publication → 0..1 Category
```

Une publication possède au maximum une catégorie principale.

État d’implémentation (étape 20) : module `taxonomy`, table `category` ; la publication référence sa catégorie par identifiant (`publication.category_id`, ADR 0002).

---

# 18. Tag

```text
Tag
├── name
└── slug
```

Relation :

```text
Publication N ↔ N Tag
```

Les tags sont éditoriaux.

Ils ne constituent pas le catalogue des technologies des projets.

État d’implémentation (étape 20) : module `taxonomy`, table `tag` ; association `publication_tag` possédée par le module `publication` (ADR 0002). Les tags n’ont pas d’ordre propre : ils sont présentés par ordre alphabétique.

---

# 19. Series

```text
Series
├── title
├── slug
├── descriptionMarkdown
└── coverMedia?
```

Une série ne possède pas de workflow éditorial propre dans la V1.

Elle organise des articles.

---

# 20. SeriesItem

```text
SeriesItem
├── publication
└── position
```

Relations :

```text
Series 1 → 0..* SeriesItem
SeriesItem → 1 Publication
```

Invariants :

```text
Publication.type = ARTICLE

un article appartient au maximum à une série

(series, position) est unique

position > 0
```

`SeriesItem` n'existe pas sans `Series`.

---

# 21. Media

```text
Media
├── storageKey
├── originalName
├── mimeType
├── size
├── width?
├── height?
├── altText?
└── createdAt
```

Formats :

```text
PNG
JPEG
WebP
PDF
```

Le domaine connaît :

```text
storageKey
```

mais pas le chemin physique utilisé par `LocalMediaStorage`.

---

# 22. Suppression des médias

Un `Media` référencé ne doit pas pouvoir être supprimé silencieusement.

Flux :

```text
demande de suppression
 ↓
recherche des références
 ↓
référencé ?
 ├── oui → refus
 └── non → suppression
```

La suppression d'un :

```text
Project
Publication
Series
```

ne supprime donc pas automatiquement les médias référencés.

---

# 23. ContactMessage

```text
ContactMessage
├── name
├── email
├── subject
├── message
├── status
├── createdAt
└── updatedAt
```

Aucune adresse IP n'est persistée par défaut.

Le rate limiting est une préoccupation de sécurité de la requête, pas une propriété métier du message.

---

# 24. ContactStatus

```text
NEW
READ
PROCESSED
ARCHIVED
```

Transitions normales :

```text
NEW
 ↓
READ
 ↓
PROCESSED
 ↓
ARCHIVED
```

---

# 25. Notification email du contact

Flux :

```text
validation
 ↓
sauvegarde du ContactMessage
 ↓
commit réussi
 ↓
tentative de notification email
```

Une erreur SMTP ne peut pas annuler la sauvegarde.

La V1 ne modélise pas un historique complet de livraison d'emails.

Une telle fonctionnalité pourra devenir un sous-système séparé ultérieurement.

---

# 26. AdminAccount

```text
AdminAccount
├── login
├── passwordHash
├── enabled
├── lastLoginAt?
├── createdAt
└── updatedAt
```

Il existe un seul compte administrateur en V1.

Le modèle ne dépend pas d'un algorithme de hash précis.

Il exige seulement que le mot de passe soit stocké à travers le mécanisme sécurisé retenu par Spring Security.

---

# 27. Search

`Search` n'est pas une entité.

C'est une capacité de lecture portant sur :

```text
Publication publiquement visible
Project PUBLISHED
```

La recherche n'indexe pas :

```text
brouillons
contenus archivés
profil
messages de contact
administration
```

---

# 28. Relations principales

```text
Profile
 ├── Skill *
 ├── Experience *
 ├── Education *
 ├── Certification *
 ├── ProfessionalLink *
 ├── avatar → Media ?
 └── CV → Media ?

Project
 ├── Technology *
 ├── ProjectScreenshot *
 └── cover → Media ?

ProjectScreenshot
 └── Media

Publication
 ├── Category ?
 ├── Tag *
 └── cover → Media ?

Series
 ├── SeriesItem *
 └── cover → Media ?

SeriesItem
 └── Publication ARTICLE
```

---

# 29. Diagramme conceptuel

```mermaid
erDiagram

    PROFILE ||--o{ SKILL : contains
    PROFILE ||--o{ EXPERIENCE : contains
    PROFILE ||--o{ EDUCATION : contains
    PROFILE ||--o{ CERTIFICATION : contains
    PROFILE ||--o{ PROFESSIONAL_LINK : contains

    PROJECT }o--o{ TECHNOLOGY : uses
    PROJECT ||--o{ PROJECT_SCREENSHOT : contains
    PROJECT_SCREENSHOT }o--|| MEDIA : references

    PUBLICATION }o--o| CATEGORY : classified_as
    PUBLICATION }o--o{ TAG : tagged_with
    PUBLICATION }o--o| MEDIA : cover

    SERIES ||--o{ SERIES_ITEM : contains
    SERIES_ITEM }o--|| PUBLICATION : references
    SERIES }o--o| MEDIA : cover
```

---

# 30. Invariants métier

Les règles suivantes doivent être garanties par le backend et, lorsque pertinent, renforcées par la base :

1. une `NEWS` n'appartient jamais à une série ;
2. un article appartient au maximum à une série ;
3. une position n'existe qu'une fois dans une même série ;
4. une publication possède au maximum une catégorie ;
5. un slug respecte l'unicité définie ;
6. un slug publié devient stable ;
7. `DRAFT` n'est jamais public ;
8. `IN_REVIEW` n'est jamais public ;
9. `ARCHIVED` n'est jamais public ;
10. `SCHEDULED` dépend de `publishedAt` et de l'horloge applicative ;
11. seuls les projets `PUBLISHED` sont publics ;
12. un média doit respecter les types et tailles autorisés ;
13. un média référencé ne peut pas être supprimé ;
14. un échec SMTP ne supprime jamais un message sauvegardé ;
15. le mot de passe administrateur n'est jamais stocké en clair ;
16. une entité JPA ne constitue jamais directement le contrat d'API.
17. une période (`Experience`, `Education`, `Project`) ne se termine jamais avant de commencer ; `endDate = null` signifie « en cours » ;
18. une `Certification` n'expire jamais avant sa date de délivrance ;
19. le nom d'une `Skill` est unique dans le profil ;
20. il existe au plus un `Profile` ;
21. un `Project` est `IN_PROGRESS` si et seulement s'il n'a pas de date de fin ; sa période respecte l'invariant 17 ;
22. le nom (sans tenir compte de la casse) et le slug d'une `Technology` sont uniques ; une technologie utilisée par un projet ne peut pas être supprimée ;
23. un `Project` référence au plus une fois la même `Technology` ;
24. une `Publication` `SCHEDULED` ou `PUBLISHED` possède toujours une date de publication (`publishedAt`) ;
25. le nom (sans tenir compte de la casse) et le slug d'une `Category`, d'un `Tag`, sont uniques dans leur vocabulaire ; un terme utilisé par une publication ne peut pas être supprimé.

Les invariants 17 à 25 ont été ajoutés pendant l'implémentation (étapes 14 à 20) ; ils sont garantis par PostgreSQL (`CHECK`, `UNIQUE`, clés étrangères) et, pour 17, 18, 21, 23 et 24, par une garde Java dans le modèle métier. Les invariants 7 à 10 (visibilité des publications) sont appliqués par les lectures publiques depuis l'étape 19. Voir [`decisions/registre-implementation.md`](decisions/registre-implementation.md).

---

# 31. Concepts volontairement absents

La V1 ne possède pas :

```text
PublicUser
Author
Role métier multiple
Permission
Comment
Like
Reaction
NewsletterSubscriber
Payment
ArticleRevision
ViewCounter
Recommendation
Translation
EmailDeliveryHistory
```

Ils ne doivent pas apparaître « pour anticiper ».

---

# 32. Découpage backend

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

`shared` doit contenir uniquement des primitives réellement transversales.

Il ne doit pas devenir un dossier général pour les classes mal classées.

---

# 33. Résultat

Le modèle de la V1 possède des frontières cohérentes, suffisamment de règles pour guider les migrations et aucune abstraction majeure sans besoin fonctionnel.

La traduction vers PostgreSQL/JPA pourra commencer ensuite sans modifier les décisions fonctionnelles du périmètre.
