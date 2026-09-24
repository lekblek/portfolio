# ADR 0002 — Communication entre modules : identifiants et façades de lecture

Statut : **Acceptée** (2026-09-24)
Date : 2026-09-24

## Contexte

Jusqu'à l'étape 19, aucun module métier n'utilisait un autre module. L'étape 20 crée le module `taxonomy` (catégories, tags) et rattache ses termes aux publications, conformément au graphe de `04-architecture-backend.md` §4 (`publication → taxonomy`).

`04` §8 souhaitait déjà qu'un module passe par une façade publique de l'autre plutôt que par ses internes, « à terme ». Aucune règle ne le vérifiait : rien n'empêchait `publication` de référencer les entités JPA ou les ports de `taxonomy`.

Le besoin concret : une publication a au plus une catégorie et des tags ; les lectures publiques affichent leurs noms et slugs et filtrent par slug.

## Options étudiées

### A. Relations JPA vers les entités de l'autre module

`PublicationEntity` porte `@ManyToOne CategoryEntity` et `@ManyToMany TagEntity` (entités de `taxonomy`) ; filtres en JPQL (`p.category.slug = :slug`).

- \+ le moins de code ; jointures et chargement par lot fournis par Hibernate ;
- − `publication.infrastructure` dépend de `taxonomy.infrastructure` : toute évolution de la persistance de `taxonomy` peut casser `publication` ;
- − les requêtes de `publication` lisent les tables de `taxonomy` : plus aucun module n'est seul propriétaire de ses tables ;
- − le couplage se propage à chaque module suivant (`series → publication`, `search → publication, project`).

### B. Identifiants et façade de lecture — **retenue**

`publication` ne stocke que des identifiants (`category_id`, table `publication_tag`) protégés par des clés étrangères. Les termes sont obtenus par une façade du module `taxonomy` (`TaxonomyQueryService`) qui renvoie ses records de `domain.model`. Les slugs des filtres sont traduits en identifiants par cette façade.

- \+ chaque module reste propriétaire de ses tables et de sa persistance ; la dépendance ne passe que par un contrat de lecture explicite ;
- \+ l'intégrité reste garantie par PostgreSQL (clés étrangères, `ON DELETE RESTRICT`) ;
- \+ nombre de requêtes constant et mesuré (5 par page de publications, D-AS) ;
- − une façade et un assemblage des termes à écrire (≈ 3 classes) ;
- − une requête de plus que l'option A par page.

### C. Événements ou copie des termes (dénormalisation)

- − complexité (synchronisation, cohérence à terme) sans aucun besoin dans un monolithe à une seule base. Écartée.

## Décision

Option **B**, avec les règles suivantes.

1. **Ce qu'un module peut utiliser d'un autre module** : ses records `domain.model` et ses services `application` (cas d'usage ou façade de lecture `<Module>QueryService`). **Jamais** ses ports (`domain.port`), sa persistance (`infrastructure`) ni son web.
2. **Les données d'un autre module se référencent par identifiant**, avec une clé étrangère en base ; aucune relation JPA ne traverse une frontière de module.
3. **Une façade de lecture** est une classe concrète du module appelé (`<module>.application.query.<Module>QueryService`), transactionnelle en lecture (elle rejoint la transaction de l'appelant), qui ne renvoie que des records du domaine. Elle ne contient que des méthodes utilisées (même règle que les ports, ADR 0001 §2) et regroupe les lectures par lot (une requête pour un ensemble d'identifiants).
4. **La composition** (associer une publication à ses termes) a lieu dans la couche `application` du module appelant.

## Conséquences

- Règle ArchUnit `ModuleBoundariesTest.modules_only_use_each_other_through_domain_models_and_application_services` : fait échouer le build si un module dépend des `domain.port`, `infrastructure` ou `web` d'un autre (validée en introduisant une violation temporaire).
- Les tests d'un module peuvent utiliser les ports d'un autre pour préparer leurs données (ArchUnit n'importe pas les classes de test).
- `series → publication` (étape 23) et `search → publication, project` (étapes 28-29) suivront les mêmes règles ; `04` §8 n'est plus « à terme » mais la règle.
- Première application : `publication` et `taxonomy` à l'étape 20 (décisions D-AN à D-AT du registre).
