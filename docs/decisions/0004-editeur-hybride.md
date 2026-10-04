# ADR 0004 — Éditeur hybride des publications : Milkdown Kit, Markdown canonique

Statut : Acceptée
Date : 2026-10-03

## Contexte

F31 remplace la zone de texte du contenu des publications (`contentMarkdown`) par un éditeur à trois onglets : **Visuel**, **Markdown**, **Aperçu** (`01-perimetre-v1.md` §9, ancienne étape 51). Le Markdown reste la seule source métier (D-DG) : l'API le stocke, le site public le rend avec le moteur de l'ADR 0003. L'éditeur visuel n'est qu'une autre façon d'écrire ce Markdown ; jamais de HTML stocké.

Exigences :

- **fidélité** : Markdown → éditeur → Markdown sans perte de contenu ; seules des normalisations sans effet sur le rendu public sont admises, et elles sont listées ;
- ce que le site rend (ADR 0003, F15) : titres, emphase, listes, liens, citations, code en ligne, blocs de code avec langage, images avec titre (figures légendées, D-EV), tableaux GFM, notes, formules `$…$` et `$$…$$`, blocs ` ```mermaid ` ;
- intégration Angular sans autre cadriciel d'interface ; insertion d'images depuis la médiathèque (sélecteur de F26) ;
- **poids** : rien dans le lot initial ni dans les autres écrans ; chargé avec la page d'édition ;
- **accessibilité** : l'onglet Markdown reste une alternative complète ; les commandes de l'éditeur visuel sont des boutons nommés ;
- maintenance active, licence permissive.

## Options (versions publiées le 2026-09-23)

| Option | Version | Contenu | Lot minifié (esbuild, ESM) | gzip |
|---|---|---|---|---|
| **Milkdown Crepe** | 7.22.2 | éditeur prêt à l'emploi : barre et menus en **Vue 3**, blocs de code **CodeMirror 6** avec `@codemirror/language-data`, formules **KaTeX 0.18**, thèmes | 2 781 kB | 926 kB |
| **Milkdown Kit** | 7.22.2 | noyau (ProseMirror, remark), presets CommonMark et GFM, greffons (historique, écouteur…) ; aucune interface | 473 kB | 147 kB |
| Kit + `remark-math` 6.0.0 | | formules analysées et réécrites telles quelles | (compris ci-dessus) | |

Mesures du 2026-10-03, lot unique sans découpage ni CSS, usage minimal (création d'un éditeur, lecture du Markdown). Licences : MIT pour toutes.

| Critère | Crepe | Kit (retenu) |
|---|---|---|
| Fidélité du Markdown | celle du noyau (remark) ; formules par son propre greffon | celle du noyau ; formules par `remark-math` (`inlineMath`, `math`) et deux nœuds du projet, réécrits à l'identique |
| GFM, titres, listes, liens, citations, code, images, tableaux, notes | oui | oui (presets CommonMark et GFM : tableaux, barré, listes de tâches, notes) |
| Langage des blocs de code | oui (CodeMirror) | oui (attribut `language`, affiché en repère) |
| Formules | rendues par KaTeX 0.18 (le site utilise 0.19 : deux versions) | éditées comme source dans l'éditeur visuel, rendues dans l'Aperçu par le moteur public |
| Mermaid | bloc de code | bloc de code ` ```mermaid ` ; rendu dans l'Aperçu |
| Intégration Angular | embarque Vue 3 (second cadriciel dans l'administration) | aucune dépendance d'interface ; barre d'outils en Angular |
| Taille, chargement | ×6 ; `language-data` impose un découpage supplémentaire | lot paresseux de la page d'édition |
| Accessibilité | menus et barre Vue non maîtrisés (glisser, barre flottante à la sélection) | barre d'outils du projet (motif *toolbar* de l'APG), boutons nommés, dialogues natifs |
| Personnalisation | thèmes CSS, options | totale (schéma, commandes, vues) |
| Maintenance | même dépôt, mêmes versions | même dépôt, mêmes versions |

## Décision

**Milkdown Kit 7.22.2**, avec `remark-math` 6.0.0, versions figées.

- Éditeur visuel `features/admin/editor` : Kit (CommonMark, GFM, historique, écouteur), deux nœuds du projet pour les formules (`math_inline`, `math_block`, contenu texte édité comme source, réécrit en `$…$` et `$$…$$`), options d'écriture de remark fixées (puces `-`, emphase `*`, blocs clôturés).
- Barre d'outils du projet : titres de niveaux 2 et 3, gras, italique, code, lien, listes, citation, bloc de code (langage choisi), formule, diagramme Mermaid (gabarit avec `accTitle` et `accDescr`), tableau, image de la médiathèque (texte alternatif repris ou saisi, légende facultative → figure).
- Onglets `shared/ui/tabs.ts` (Angular Aria) ; les trois panneaux restent dans le document (aucune perte au changement d'onglet) ; l'Aperçu utilise `app-markdown-view`, le moteur public.
- Le Markdown du formulaire n'est remplacé que par une **modification réelle** : la normalisation produite à l'ouverture de l'éditeur visuel n'est jamais écrite (la page ne devient pas « modifiée » par un simple affichage).
- Chargement : le composant de l'éditeur visuel vit dans un bloc `@defer` ; Milkdown n'entre ni dans le lot initial ni dans le lot des autres écrans.

## Normalisations admises

Constatées sur le corpus `features/admin/editor/fixtures/*.md`, sans effet sur le rendu public (HTML identique, vérifié par les tests) :

- puce `*` ou `+` → `-` ; liste numérotée renumérotée depuis son premier numéro ; emphase `_x_` → `*x*`, gras `__x__` → `**x**` ;
- titres soulignés (`===`, `---`) → titres `#` ; blocs de code indentés → blocs clôturés ;
- tableaux réalignés (largeur des colonnes, espaces) ;
- caractères échappés ajoutés là où remark les juge ambigus (`\*`, `\_`, `\[`, `\$` hors formule) ;
- lignes vides ramenées à une seule entre blocs, espaces de fin de ligne retirés.

Toute autre différence est une perte : le test du corpus échoue.

## Conséquences

- Dépendances : `@milkdown/kit` (et ses paquets `@milkdown/*`, ProseMirror, remark 15), `remark-math` ; notices de licence complétées.
- Limites connues, consignées : l'éditeur visuel ne rend ni les formules ni les diagrammes (source éditable, rendu dans l'Aperçu) ; les notes s'éditent comme texte ; un lecteur d'écran lit l'éditeur visuel comme une zone de texte riche (ProseMirror), l'onglet Markdown reste l'alternative complète et accessible.
- À revoir si : un besoin de rendu en place des formules apparaît (KaTeX déjà présent, 0.19), ou si Milkdown cesse d'être maintenu (le Markdown reste la source : changer d'éditeur ne touche pas aux données).
