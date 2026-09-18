# Périmètre V1 — Portfolio

## 1. Objectif de la V1

La V1 est un **portfolio professionnel public et administrable**, déployé en HTTPS sur un VPS.

Elle permet :

* de présenter le profil et le parcours professionnel ;
* de présenter des projets ;
* de publier des articles techniques ;
* de publier des actualités ;
* de construire des séries de tutoriels ;
* d'administrer le contenu sans modifier le code ;
* de démontrer une architecture full-stack professionnelle grâce aux tests, à la sécurité, à la CI/CD et au déploiement.

Toute fonctionnalité qui ne contribue pas directement à la **présentation professionnelle**, à la **publication de contenu** ou à la **démonstration technique** est hors V1.

---

## 2. Site public

Routes prévues :

```text
/
/about

/projects
/projects/:slug

/articles
/articles/:slug

/news
/news/:slug

/series
/series/:slug

/search
/contact

/sitemap.xml
/robots.txt

404
```

---

## 3. Administration

```text
/admin/login
/admin
/admin/profile
/admin/projects
/admin/publications
/admin/series
/admin/categories
/admin/tags
/admin/media
/admin/contacts
/admin/settings
```

Un seul compte administrateur existe en V1.

Aucune inscription publique.

---

## 4. Profil

Le profil permet de gérer :

* nom ;
* titre professionnel ;
* présentation courte ;
* présentation détaillée ;
* localisation publique éventuelle ;
* avatar ;
* liens professionnels ;
* compétences ;
* expériences ;
* formations ;
* certifications ;
* CV PDF.

---

## 5. Projets

Un projet contient notamment :

```text
title
slug
shortDescription
descriptionMarkdown

stage
visibility

startDate
endDate

repositoryUrl
demoUrl

technologies
coverMedia
screenshots

featured
displayOrder
```

### État métier

```text
IN_PROGRESS
COMPLETED
```

### Visibilité

```text
DRAFT
PUBLISHED
ARCHIVED
```

Seuls les projets `PUBLISHED` sont accessibles publiquement et indexés dans la recherche.

---

## 6. Publications

Une publication possède un type :

```text
ARTICLE
NEWS
```

Les deux utilisent le même modèle métier.

Une publication possède notamment :

```text
title
slug
summary
contentMarkdown
coverMedia
status
publishedAt
category
tags
seoTitle
seoDescription
featured
```

Une publication possède au maximum **une catégorie principale** et peut posséder plusieurs tags.

---

## 7. Cycle éditorial

```text
DRAFT
IN_REVIEW
SCHEDULED
PUBLISHED
ARCHIVED
```

`IN_REVIEW` reste disponible sans workflow multi-utilisateur en V1.

Une publication `SCHEDULED` devient publiquement visible lorsque :

```text
publishedAt <= currentTime
```

La visibilité effective est calculée à la lecture.

La logique temporelle devra utiliser une horloge injectable afin d'être testable de manière déterministe.

---

## 8. Séries

Une série possède :

```text
title
slug
description
coverMedia
```

Elle contient une liste ordonnée d'articles.

Une série :

* ne contient que des `ARTICLE` ;
* ne contient jamais de `NEWS` ;
* expose la position du chapitre ;
* permet la navigation précédent / suivant ;
* peut recevoir de nouveaux chapitres ultérieurement.

Un article appartient au maximum à une série en V1.

La série ne possède pas de workflow éditorial spécifique dans la V1.

---

## 9. Éditeur

L'administration fournit :

```text
[ Visuel ] [ Markdown ] [ Aperçu ]
```

Le Markdown constitue la source éditoriale canonique.

Support :

* Markdown ;
* GFM ;
* titres ;
* listes ;
* tableaux ;
* liens ;
* images ;
* citations ;
* blocs de code ;
* coloration syntaxique ;
* KaTeX ;
* Mermaid ;
* table des matières ;
* aperçu avant publication.

Le HTML généré est dérivé du Markdown et n'est pas une deuxième source éditoriale.

---

## 10. Taxonomie

La V1 possède :

```text
Category
Tag
```

Une publication :

```text
0..1 Category
0..* Tag
```

Les technologies utilisées par les projets constituent un vocabulaire distinct des tags éditoriaux.

---

## 11. Recherche

La recherche couvre :

* articles publiquement visibles ;
* news publiquement visibles ;
* projets `PUBLISHED`.

PostgreSQL Full-Text Search est utilisé.

Pondération indicative :

```text
titre       → forte
tags        → forte
résumé      → moyenne
contenu     → normale
```

Pagination :

```text
public → 10
admin  → 20
```

---

## 12. Médias

Formats V1 :

```text
PNG
JPEG
WebP
PDF
```

Limites :

```text
images → 5 Mo
PDF    → 10 Mo
```

Abstraction :

```text
MediaStorage
```

Implémentation V1 :

```text
LocalMediaStorage
```

Le domaine manipule une clé de stockage, jamais directement un chemin filesystem.

---

## 13. Contact

Flux :

```text
requête
 ↓
validation
 ↓
honeypot + rate limiting
 ↓
sauvegarde
 ↓
tentative de notification email
```

La sauvegarde du message est prioritaire.

Un échec SMTP :

```text
ne rollback jamais le message sauvegardé
```

L'adresse IP utilisée pour le rate limiting n'est pas une donnée métier persistée par défaut.

---

## 14. Authentification

La V1 utilise :

* Spring Security ;
* session serveur ;
* cookie sécurisé ;
* CSRF ;
* un seul compte administrateur ;
* stockage du mot de passe via un `PasswordEncoder` sécurisé.

Aucun secret n'est versionné dans Git.

Le choix précis de l'algorithme de hash appartient à l'étape d'implémentation sécurité.

---

## 15. SEO

La V1 comprend :

* SSR ou prerender lorsque pertinent ;
* `<title>` dynamique ;
* meta description ;
* canonical ;
* Open Graph ;
* sitemap dynamique ;
* robots.txt ;
* données structurées pertinentes.

---

## 16. Accessibilité

Cible :

```text
WCAG 2.2 AA
```

Les contrôles porteront notamment sur :

* navigation clavier ;
* focus ;
* contraste ;
* structure des titres ;
* formulaires ;
* textes alternatifs ;
* composants interactifs.

---

## 17. Déploiement

Production :

```text
VPS
 ↓
Docker Compose
 ↓
Caddy
 ↓
HTTPS
 ↓
Angular SSR
Spring Boot
PostgreSQL
```

La V1 inclut :

* volumes persistants ;
* health checks ;
* logs ;
* sauvegarde ;
* test de restauration ;
* rollback documenté.

---

## 18. Décisions structurantes

| Réf | Décision                                                                                 |
| --- | ---------------------------------------------------------------------------------------- |
| D01 | Site monolingue français                                                                 |
| D02 | Une entité `Publication` avec `ARTICLE` / `NEWS`                                         |
| D03 | Publication planifiée résolue à la lecture avec horloge injectable                       |
| D04 | `IN_REVIEW` conservé sans workflow multi-utilisateur                                     |
| D05 | Une série contient seulement des articles ; un article appartient au maximum à une série |
| D06 | CV téléversé comme PDF                                                                   |
| D07 | Aperçu brouillon uniquement dans l'administration                                        |
| D08 | PNG/JPEG/WebP/PDF uniquement                                                             |
| D09 | FTS sur publications publiques et projets publiés                                        |
| D10 | Honeypot + rate limiting sans CAPTCHA tiers en V1                                        |
| D11 | Slug immuable après première publication                                                 |
| D12 | Temps de lecture calculé ; aucun compteur de vues                                        |
| D13 | Pas de thème sombre en V1                                                                |
| D14 | Échec email non transactionnel avec la sauvegarde du contact                             |
| D15 | Sitemap généré à partir du contenu réellement public                                     |
| D16 | Pagination 10 public / 20 admin                                                          |
| D17 | Accessibilité cible WCAG 2.2 AA                                                          |
| D18 | Admin initialisé depuis configuration sécurisée                                          |
| D19 | État métier et visibilité d'un projet sont séparés                                       |
| D20 | Une publication possède au maximum une catégorie et plusieurs tags                       |
| D21 | Technologies des projets et tags éditoriaux sont deux vocabulaires distincts             |
| D22 | Rendu serveur à la demande par défaut ; prerender décidé route par route à l'étape 52.1 |
| D23 | compose.yaml racine pour le développement local ; deploy/compose.yaml réservé à la production, mais on va le mettre dans deploy en ajoutant .dev |
---

## 19. Hors V1

Hors périmètre :

* multi-utilisateurs ;
* rôles éditoriaux ;
* comptes lecteurs ;
* commentaires ;
* likes ;
* réactions ;
* newsletter complète ;
* paiement ;
* réseau social ;
* application mobile ;
* IA ;
* moteur de recommandation ;
* microservices ;
* Kafka ;
* Kubernetes ;
* Elasticsearch/OpenSearch ;
* GraphQL ;
* multilingue ;
* thème sombre ;
* compteur de vues ;
* analytics avancées ;
* historique des versions ;
* import/export massif ;
* génération automatique du CV ;
* édition collaborative ;
* autosave distant ;
* notifications push ;
* redirections historiques automatiques.

---

## 20. Contenu minimum avant `v1.0.0`

### Profil

* profil complet ;
* CV ;
* au moins 6 compétences ;
* au moins 1 expérience ;
* au moins 1 formation.

### Projets

* au moins 3 projets ;
* au moins 2 mis en avant ;
* captures réelles.

### Publications

* au moins 3 articles ;
* au moins 2 articles dans une même série ;
* au moins 1 news.

### Taxonomie

* catégories pertinentes ;
* tags cohérents.

Les données fictives de démonstration ne comptent pas comme contenu final.

---

## 21. Definition of Done

La V1 n'est terminée que si :

```text
[ ] site public fonctionnel
[ ] administration protégée
[ ] profil administrable
[ ] projets administrables
[ ] articles fonctionnels
[ ] news fonctionnelles
[ ] séries fonctionnelles
[ ] éditeur Markdown fonctionnel
[ ] KaTeX fonctionnel
[ ] Mermaid fonctionnel
[ ] recherche PostgreSQL fonctionnelle
[ ] médias fonctionnels
[ ] formulaire de contact fonctionnel
[ ] notification email fonctionnelle
[ ] SSR/prerender vérifié
[ ] SEO essentiel vérifié
[ ] tests critiques verts
[ ] responsive vérifié
[ ] accessibilité WCAG 2.2 AA auditée
[ ] Docker fonctionnel
[ ] HTTPS fonctionnel
[ ] sauvegarde fonctionnelle
[ ] restauration testée
[ ] health checks fonctionnels
[ ] déploiement documenté
[ ] CI verte
[ ] aucun secret dans Git
[ ] README à jour
[ ] contenu réel minimum présent
[ ] tag v1.0.0 créé
```
