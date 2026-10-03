# ADR 0003 — Rendu du Markdown : analyseur, coloration syntaxique, sûreté

Statut : Acceptée
Date : 2026-09-30

## Contexte

Les contenus longs arrivent de l'API en Markdown brut (`aboutMarkdown`, `descriptionMarkdown`, `contentMarkdown`, D-DG) ; le frontend les transforme en HTML (`01-architecture.md` §12). Le même moteur sert le site public (rendu serveur) et l'aperçu de l'administration (navigateur). La décision est transverse, coûteuse à inverser, et plusieurs options sont crédibles.

Exigences :

- rendu serveur **et** navigateur, synchrone, sans DOM côté serveur ;
- **sûreté** : un contenu saisi dans l'administration ne doit jamais produire de script, de gestionnaire d'événement ni de lien `javascript:`, même si le compte d'administration était compromis ;
- GFM (tableaux, barré, liens automatiques, listes de tâches), titres ancrés (identifiants stables, accents français), table des matières, code coloré, liens externes marqués ;
- compatibilité avec KaTeX et Mermaid (F15) et avec les notes marginales des articles (F14) ;
- poids : le moteur vit dans un lot chargé à la demande, jamais dans le lot initial ;
- maintenance active.

## Mesures (2026-09-30)

Poids d'un lot minifié par esbuild, format ESM navigateur, pour l'usage réel du moteur (configuration et appel) :

| Option | Version | Minifié | gzip |
|---|---|---|---|
| `marked` + `marked-footnote` | 18.0.14 | 47,0 kB | 14,5 kB |
| `markdown-it` (`html: false`, `linkify`) | 15.0.2 | 96,7 kB | 40,4 kB |
| `markdown-it` + `markdown-it-footnote` | 15.0.2 / 4.0.0 | 101,7 kB | 41,8 kB |
| `highlight.js` (noyau + 10 langages) | 11.12.0 | 67,8 kB | 21,6 kB |
| `shiki` (noyau, moteur JS, 10 langages, 1 thème) | 4.4.3 | 793,8 kB | 135,6 kB |
| `prismjs` + 7 composants | 1.30.0 | 37,0 kB | 14,5 kB |
| `dompurify` (navigateur) | 3.4.16 | 29,5 kB | 11,4 kB |
| `sanitize-html` (navigateur) | 2.17.7 | 152,4 kB | 56,2 kB |

Corpus de 10 attaques (balise `script`, attributs `on…`, bloc HTML, liens `javascript:` / `vbscript:` / `data:text/html`, casse et entités mêlées, lien automatique, image, titre de lien piégé), sorties réellement dangereuses :

| Analyseur | Configuration | Sorties dangereuses |
|---|---|---|
| `marked` | par défaut | **8 / 10** (HTML brut recopié, protocoles non filtrés) |
| `marked` | durci : `html` échappé, `link` et `image` filtrés | 0 / 10 |
| `markdown-it` | par défaut (`html: false`) | **0 / 10** |

Assainisseur d'Angular (`[innerHTML]`, version 22.2) : il retire l'attribut **`id`** (absent de sa liste d'attributs), les éléments `input`, `math` et `svg`. Il ne peut donc pas être la dernière barrière d'un contenu à titres ancrés, notes et listes de tâches : chaque rendu perdrait ses ancres et produirait un avertissement.

## Options étudiées

**Analyseur.**

- `marked` : le plus léger (14,5 kB) et actif ; mais recopie le HTML brut et n'impose aucun filtre de protocole : la sûreté dépend de surcharges à tenir sur chaque type de jeton qui émet une URL ou du HTML, y compris ceux ajoutés plus tard par une extension (KaTeX, notes). Conformité CommonMark partielle.
- `markdown-it` : sûr par défaut (`html: false` échappe tout HTML brut dès l'analyse ; `validateLink` s'applique à tous les liens, liens automatiques et images) ; conforme à CommonMark ; greffons KaTeX maintenus (`@vscode/markdown-it-katex`, utilisé par VS Code) ; jetons manipulables pour la table des matières et le déplacement des notes. 26 kB gzip de plus que `marked`.

**Coloration.**

- `shiki` : rendu fidèle, mais 135,6 kB gzip, initialisation asynchrone, et couleurs en **styles en ligne** (hors tokens du système de design, retirées par l'assainisseur d'Angular, contraires à la future politique de sécurité du contenu).
- `prismjs` : léger, mais objet global modifié par effets de bord, branche 1.x en maintenance de sécurité seulement.
- `highlight.js` : classes CSS (`hljs-keyword`…) coloriées par nos tokens, synchrone, rendu serveur possible, maintenu, noyau à langages enregistrés un par un.

**Sûreté.**

- `sanitize-html` : 56,2 kB dans le navigateur (analyseur HTML et `postcss` embarqués).
- `isomorphic-dompurify` : DOMPurify (11,4 kB) dans le navigateur mais `jsdom` côté serveur (dépendance lourde, coût à chaque rendu) ; DOMPurify ne prend pas en charge le DOM simulé d'Angular.
- HTML brut désactivé + validation des liens : sûreté **par construction** — aucun HTML saisi n'atteint la sortie, seuls nos règles de rendu produisent des balises, avec des attributs connus.

## Décision

- **Analyseur : `markdown-it` 15** (`html: false`, `linkify: true`, `typographer: false`), une instance configurée une fois.
- **Coloration : `highlight.js` 11**, noyau et langages enregistrés explicitement (`java`, `typescript`, `javascript`, `json`, `bash`, `sql`, `xml`, `css`, `python`, `yaml`) ; langage inconnu ou absent : code échappé sans coloration.
- **Sûreté par construction**, sans bibliothèque d'assainissement :
  1. HTML brut toujours échappé (`html: false`) ;
  2. `validateLink` du projet : seuls `http:`, `https:`, `mailto:` et les adresses relatives sont acceptés (liens et images) ; tout autre protocole n'est pas transformé en lien (le Markdown reste affiché tel qu'il est écrit) ;
  3. balises et attributs produits par nos seules règles : identifiants de titres réduits à `[a-z0-9-]`, classes fixes, adresses validées ;
  4. insertion par `bypassSecurityTrustHtml`, **uniquement** dans `shared/markdown`, justifiée en commentaire, parce que l'assainisseur d'Angular retire les `id` ;
  5. un corpus d'attaques dans les tests du moteur, qui échoue à la moindre régression ;
  6. tout greffon ajouté plus tard (KaTeX, notes, Mermaid) doit préserver ces invariants et étendre le corpus.
- **Pas de greffon avant son besoin** : notes de bas de page (`markdown-it-footnote`) avec la page d'article (F14), KaTeX et Mermaid en F15 (complément de cet ADR).
- **Complément F15 — formules et diagrammes** :
  - **KaTeX 0.19.0** (version figée), délimiteurs du projet `$…$` (dans le texte) et `$$…$$` (bloc, seul sur sa ou ses lignes) analysés par deux règles du moteur, sans greffon (`@vscode/markdown-it-katex` importerait KaTeX dans tous les lots qui contiennent le moteur). `$` suivi d'une espace ou `$` fermant suivi d'un chiffre ne sont pas des formules ; `\$` reste un dollar. Rendu `htmlAndMathml`, `throwOnError: false` (formule invalide affichée et signalée, couleur du token `danger`), `trust: false` (aucun lien ni attribut). **KaTeX n'est fourni qu'au serveur** (`SERVER_MATH_RENDERER`, `app.config.server.ts`) ; chaque formule rendue est confiée à `TransferState`, et le navigateur la reprend telle quelle à l'hydratation : aucun JavaScript de KaTeX au premier affichage (vérifié sur le build de production). Une formule inconnue (navigation dans le navigateur) charge KaTeX à la demande (lot paresseux de 63,97 kB transférés) puis relance le rendu. Feuille de style et polices `woff2` de KaTeX copiées du paquet (`/katex/`) et ajoutées au `<head>` des seules pages qui ont des formules.
  - **Mermaid 11.17.2** (version figée) plutôt que 12.0.0 : la 12.0.0 tire `chevrotain` 11 et un `lodash-es` vulnérable (injection de code par `_.template`, pollution de prototype ; `npm audit`), la 11.17.2 non, une fois `lodash-es` porté à 4.18.1 dans l'intervalle permis (0 vulnérabilité). Bloc ` ```mermaid ` rendu par le serveur comme son source (repli lisible sans JavaScript), dessiné dans le navigateur quand il approche de la fenêtre (`IntersectionObserver`, marge de 200 px) ; Mermaid chargé à ce moment seulement (`mermaid-core` 27,14 kB transférés et ses diagrammes), `securityLevel: 'strict'` (non modifiable par une directive), thème `base` aux couleurs et police des tokens ; `accTitle` / `accDescr` deviennent `<title>` / `<desc>` du SVG ; source conservé dans un `<details>` ; échec annoncé, source affiché. Dépendances CommonJS de Mermaid déclarées dans `allowedCommonJsDependencies` (lot paresseux seulement).
- **Complément F27 — figures légendées (D-EV)** : une image seule dans son paragraphe et pourvue d'un titre (`![texte alternatif](adresse "Légende")`) devient `<figure class="figure">` avec `<figcaption>` « Figure n — légende », numérotée dans l'ordre du contenu ; la légende est échappée, le titre retiré de l'image ; une image dans une phrase, ou sans titre, reste une image. Règle `project_figures` du moteur, couverte par le corpus de tests.
- **Complément F14** : `markdown-it-footnote` 4.0.0 (version figée) pour la seule syntaxe ; le rendu est celui du projet (note placée après le bloc de premier niveau qui l'appelle, `role="note"`, liens rattachés au chemin de la page) ; déclaration de types locale (`src/markdown-it-footnote.d.ts`). Poids mesuré du lot paresseux du moteur : 57,00 → 58,76 kB transférés. Barre des blocs de code (langage, bouton « Copier ») produite par la règle `fence` elle-même, sur option (`codeToolbar`) : aucune balise hors des règles du moteur.
- Titres décalés sous le `<h1>` de la page : le plus haut niveau présent dans le contenu devient `<h2>` (aucun saut de niveau) ; identifiants stables, accents retirés, unicité par suffixe.
- Liens internes à la page (`#…`) préfixés par le chemin de la page : avec `<base href="/">`, un `#…` seul renverrait à la racine.

## Conséquences

- Lot chargé à la demande : ≈ 40 kB (analyseur) + ≈ 22 kB (coloration) gzip. Il n'est demandé qu'avec une page qui affiche du Markdown ; sur une page rendue par le serveur, le contenu est déjà dans le HTML, et une hydratation différée (`@defer (hydrate never)`) évitera de le charger au premier affichage : décidé avec la première page qui l'utilise (F12).
- La sûreté ne dépend ni de la confiance dans l'administrateur, ni de l'assainisseur d'Angular : elle repose sur la configuration de l'analyseur et sur le corpus de tests ; la politique de sécurité du contenu (F35) ajoute une dernière barrière.
- F15 réexamine l'assainissement : KaTeX produit du MathML et des styles en ligne, Mermaid du SVG.
- Remplacer l'analyseur imposerait de réécrire les règles de rendu (titres, code, liens, listes de tâches) et le corpus : c'est la raison de cet ADR.
