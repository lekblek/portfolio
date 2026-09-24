# Décisions — index

Ce dossier répond à une question : **où une décision est-elle enregistrée, et est-elle toujours valable ?**

Les décisions du projet existent à trois niveaux. Chacun a un seul endroit de référence.

| Niveau | Exemple | Où l'enregistrer |
|---|---|---|
| Structurante (périmètre, modèle, versions, architecture, API) | « une série ne contient que des articles » | table de décisions du document concerné (voir ci-dessous) |
| Transverse et coûteuse à inverser | « architecture interne des modules » | un ADR dans ce dossier (`NNNN-titre.md`) |
| Locale à une étape d'implémentation | « tri total : displayOrder, date, id » | [`registre-implementation.md`](registre-implementation.md) |

Une décision n'est **jamais** enregistrée uniquement dans `docs/steps/` : ce dossier n'est pas versionné (décision R-1) et peut être perdu ou régénéré.

## Tables de décisions existantes

| Préfixe | Sujet | Document |
|---|---|---|
| D01 … D24 | Périmètre V1 | [`../01-perimetre-v1.md` §18](../01-perimetre-v1.md) |
| M01 … M09 | Modélisation (étape 02) | historique de l'étape 02 ; le résultat est intégré à [`../02-modele-metier.md`](../02-modele-metier.md) |
| V01 … V06 | Versions (étape 04) | intégrées à [`../03-versions-cibles.md`](../03-versions-cibles.md) |
| A01 … A13 | Architecture backend | [`../04-architecture-backend.md` §16](../04-architecture-backend.md) |
| C01 … C11 | Conventions API | [`../05-conventions-api.md` §37](../05-conventions-api.md) |
| D-A … D-Y | Décisions d'implémentation des étapes 14 à 17 | [`registre-implementation.md`](registre-implementation.md) |
| R-1 … R-7 | Organisation du dépôt et de l'outillage | [`registre-implementation.md`](registre-implementation.md) |

## ADR

| N° | Titre | Statut |
|---|---|---|
| [0001](0001-architecture-interne-des-modules.md) | Architecture interne des modules backend : ports et adaptateurs légers | Acceptée (2026-09-24) |

## Quand écrire un ADR ?

Un ADR est justifié seulement si les trois conditions sont réunies :

1. la décision concerne plusieurs modules ou toute l'application ;
2. l'inverser plus tard coûterait plus qu'une journée de travail ;
3. une alternative crédible a été écartée.

Sinon, une ligne dans le registre d'implémentation ou dans la table du document concerné suffit.

## Format d'un ADR

```text
# ADR NNNN — Titre
Statut : Proposée | Acceptée | Remplacée par NNNN | Abandonnée
Date : AAAA-MM-JJ
## Contexte
## Options étudiées
## Décision
## Conséquences
```

Un ADR accepté n'est pas réécrit : s'il change, un nouvel ADR le remplace et l'ancien passe au statut « Remplacée par ».
