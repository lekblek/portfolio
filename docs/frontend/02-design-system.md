# Système de design — Portfolio V1

Statut : **Proposé** (2026-09-30). Les règles de structure (§5 à §20) sont acceptées ; la direction visuelle (§4 : palette, polices) reste une hypothèse jusqu'à sa validation en F05, sur des prototypes réels, capturés en mobile et en bureau.

Source de vérité à partir de F06 : `frontend/src/styles/tokens.css`. Ce document explique les tokens ; il ne les duplique pas. En cas d'écart, le fichier de tokens fait foi et ce document est corrigé.

Documents liés : [`01-architecture.md`](01-architecture.md), [`../01-perimetre-v1.md`](../01-perimetre-v1.md) (§15 SEO, §16 accessibilité, D01 site en français, D13 pas de thème sombre, D17 WCAG 2.2 AA).

---

## 1. Le brief

| Question | Réponse |
|---|---|
| Sujet | le travail d'un ingénieur logiciel avec une dimension recherche et IA : projets, articles techniques, séries de tutoriels, actualités |
| Public | recruteurs et responsables techniques (lecture rapide, crédibilité), pairs ingénieurs et chercheurs (lecture longue : code, mathématiques, diagrammes) |
| Rôle premier du site | démontrer une compétence par le contenu et par la qualité d'exécution du site lui-même |
| Ton | professionnel, éditorial, technique, sobre, précis |
| Langue | français seulement (D01), typographie française |
| Contraintes | thème clair seulement (D13), WCAG 2.2 AA, rendu serveur, rapide sur mobile |

## 2. Qualités visées

Professionnel · éditorial · technique · sobre · distinctif · précis · accessible · responsive · rapide.

Le site se reconnaît à sa **typographie** et à la **qualité de lecture** de ses contenus techniques, pas à des effets.

## 3. Ce que le site n'est pas

Clichés de gabarit à ne pas utiliser, sauf décision explicite motivée par le contenu :

- dégradés violets ou bleus décoratifs, lueurs (*glow*), verre dépoli (*glassmorphism*) ;
- grilles « bento » sans raison de contenu ; contenu découpé en cartes arrondies identiques avec la même ombre grise ;
- pastilles (*pills*) sur chaque métadonnée ; un seul rayon appliqué à tout ;
- surtitres en capitales espacées au-dessus de chaque titre ; libellés en police à chasse fixe utilisés comme décoration ; flèche « → » ajoutée à chaque lien ;
- un seul mot de titre mis en couleur ou en italique ; grands titres marketing vides (« Je construis le futur ») ;
- numérotation 01 / 02 / 03 quand le contenu n'est pas une séquence ;
- fond crème chaud avec titres à empattements et accent terre cuite ; fond presque noir avec un seul accent acide ; mise en page de journal à filets fins et colonnes serrées : trois directions devenues des réflexes, écartées par défaut ;
- animations permanentes, apparitions en fondu à chaque section, effets 3D, défilement détourné (*scroll hijacking*), parallaxe.

## 4. Direction visuelle (hypothèse, validée en F05)

### 4.1 Direction recommandée : « Planche technique »

L'univers d'une planche d'ingénierie ou d'une figure scientifique : un fond blanc neutre, une encre bleu-noir, des éléments alignés sur une grille exacte, des légendes, des numéros de figure et d'équation là où le contenu en a réellement (articles). La rigueur se voit dans l'alignement et la typographie, pas dans la décoration.

- **Élément mémorable (un seul)** : la page d'article, avec une vraie mise en page de lecture (mesure confortable, marges utilisées pour la table des matières et les métadonnées sur grand écran, figures et équations légendées, code lisible).
- **Tout le reste reste calme** : accueil typographique sans image d'illustration, listes sobres, navigation discrète.

| Rôle | Hypothèse | Contraste visé |
|---|---|---|
| fond (`paper`) | `#FFFFFF` blanc neutre | — |
| fond en retrait (`paper-sunken`) : code, encadrés | gris froid très clair, ≈ `#F3F5F8` | — |
| texte (`ink`) | bleu-noir d'encre, ≈ `#1A2233` | ≥ 14:1 sur `paper` |
| texte secondaire (`ink-muted`) | ≈ `#4A5568` | ≥ 7:1 (AAA pour le texte courant) |
| filets et bordures (`rule`) | ≈ `#D5DBE3` | décoratif ; ≥ 3:1 lorsqu'une bordure délimite un contrôle |
| accent (`accent`) : liens, focus, état courant | bleu de Prusse, ≈ `#1E4F9A` | ≥ 7:1 sur `paper` |
| signaux (`danger`, `success`, `warning`) | rouge, vert, ambre sombres, jamais seuls (texte ou icône en plus) | ≥ 4,5:1 |

Typographie (familles sous licence libre OFL, auto-hébergées) :

| Rôle | Candidats | Raison |
|---|---|---|
| titres et interface | une linéale au caractère net, de la famille des grotesques d'ingénierie (candidats : Schibsted Grotesk, Hanken Grotesk, Instrument Sans) | précision, bonne tenue en grand corps, chiffres tabulaires |
| texte long (articles, À propos) | une romaine de lecture à grand œil (candidats : Source Serif 4, Literata) | lecture longue sur écran, italique réelle, bonne tenue des mathématiques voisines |
| code | une chasse fixe lisible (candidats : JetBrains Mono, IBM Plex Mono) | seulement pour du code réel, jamais pour des libellés décoratifs |

Inter, Roboto, Arial, la police système seule et les familles d'une marque connue (Geist, SF) sont écartées : ce sont des choix par défaut, pas des choix pour ce sujet.

### 4.2 Alternatives étudiées en F05

| Direction | Idée | Risque |
|---|---|---|
| « Instrument » | fond gris froid, encre graphite, un accent vert-bleu de signal, une seule superfamille (sans, serif, mono) | proximité avec l'identité d'un laboratoire industriel connu si la famille est reconnaissable |
| « Monographie » | domination d'une romaine (titres et texte), interface en linéale discrète, accent rouge sombre | glisse facilement vers le cliché éditorial chaud ; à éviter si le fond tire vers le crème |

F05 produit les trois directions sur un contenu réel (accueil, liste de projets, article avec code et formule), les compare en captures bureau et mobile, puis le propriétaire choisit. Le choix remplace cette section et passe DS01 à « Acceptée ».

---

## 5. Architecture des tokens

Trois niveaux, un seul fichier :

```text
primitifs (valeurs brutes)      --palette-ink-900: …   jamais utilisés dans un gabarit
        ↓
sémantiques (rôles)             --color-ink, --color-accent, --text-body, --space-section …
        ↓                        seuls utilisés par les gabarits et les primitives
composant (rare)                --button-height … seulement si une primitive en a besoin
```

Mise en œuvre avec Tailwind CSS 4 (`@theme`) :

- les espaces de noms par défaut de Tailwind sont **réinitialisés** (`--color-*: initial;`, `--font-*: initial;`, `--radius-*: initial;`, `--shadow-*: initial;` …) : seuls les tokens du projet produisent des utilitaires (`text-ink`, `bg-paper`, `rounded-control`). Une couleur hors système n'existe tout simplement pas ;
- chaque token sémantique est aussi une propriété CSS personnalisée (`var(--color-ink)`), utilisable dans un style de composant ;
- les primitifs portent un préfixe (`--palette-*`) hors des espaces de noms de Tailwind : ils ne génèrent aucun utilitaire.

## 6. Typographie

| Token | Usage | Hypothèse |
|---|---|---|
| `--font-display` | titres, navigation, interface | linéale retenue |
| `--font-text` | texte long (`prose`) | romaine retenue |
| `--font-code` | code | chasse fixe retenue |
| `--text-xs` … `--text-4xl` | échelle | rapport ≈ 1,25 (tierce majeure) à partir d'un corps d'interface de 1rem et d'un corps de lecture de 1,125rem ; les deux plus grands niveaux en `clamp()` |
| `--leading-*` | interlignage | 1,6 à 1,7 pour la romaine de lecture, 1,5 pour l'interface, 1,1 à 1,2 pour les grands titres |
| `--width-prose` | mesure | 65 à 72 caractères |

Règles :

- un seul `<h1>` par page ; niveaux sans saut ; `text-wrap: balance` sur les titres, `text-wrap: pretty` sur les paragraphes ;
- chiffres tabulaires (`font-variant-numeric: tabular-nums`) dans les dates alignées, tableaux et compteurs ;
- **typographie française** : guillemets « » avec espaces insécables, espace fine insécable avant `; : ! ?` dans les textes écrits par le site, points de suspension `…`, `lang="fr"` et `hyphens: auto` sur le texte long seulement ;
- pas de texte en capitales pour les libellés ; casse de phrase partout (titres, boutons).

## 7. Couleur

- Tokens sémantiques seulement dans les gabarits : `ink`, `ink-muted`, `paper`, `paper-sunken`, `rule`, `accent`, `accent-strong` (survol, actif), `focus`, `danger`, `success`, `warning`, `selection`.
- Contraste : texte ≥ 4,5:1 (visé ≥ 7:1 pour le texte courant), grand texte et composants d'interface ≥ 3:1 (WCAG 1.4.3, 1.4.11). Chaque paire est vérifiée dans le catalogue `/_ui` (F06).
- L'information ne passe jamais par la couleur seule (statuts, erreurs, lien courant).
- `color-scheme: light` ; pas de thème sombre (D13). Les tokens sémantiques permettent d'en ajouter un plus tard sans toucher les gabarits.

## 8. Espacement

- Base 4 px (`--spacing: 0.25rem`, convention de Tailwind 4).
- Pas utilisés : 1, 2, 3, 4, 6, 8, 12, 16, 24 (4 px à 96 px). Les autres multiples ne sont pas utilisés.
- Rythme vertical : `--space-flow` (entre éléments d'un bloc), `--space-block` (entre blocs), `--space-section` (entre sections), réglés par taille d'écran.

## 9. Rayons, bordures, ombres

| Token | Valeur hypothèse | Usage |
|---|---|---|
| `--radius-control` | 3–4 px | boutons, champs |
| `--radius-media` | 0–2 px | images, blocs de code |
| `--radius-full` | 9999 px | avatar seulement |
| bordure | 1 px `--color-rule` | séparation de blocs, contrôles |
| `--shadow-overlay` | une seule ombre | éléments flottants (menu, dialogue) — aucune ombre sur le contenu posé dans la page |

La hiérarchie vient de l'espace et de la typographie, pas des boîtes.

## 10. Largeurs et points de rupture

| Token | Valeur | Usage |
|---|---|---|
| `--width-prose` | ≈ 68ch | texte long |
| `--width-content` | ≈ 72rem | pages de liste, accueil |
| `--width-wide` | ≈ 88rem | article avec marges latérales (table des matières) |
| `--breakpoint-sm` | 40rem | |
| `--breakpoint-md` | 48rem | |
| `--breakpoint-lg` | 64rem | apparition des marges d'article |
| `--breakpoint-xl` | 80rem | |

Conception à partir du mobile. Les composants réutilisables s'adaptent à leur conteneur (container queries, `@container`), les gabarits de page aux points de rupture.

Largeurs contrôlées à chaque porte de qualité : 360, 390, 768, 1024, 1280, 1440 et 1920 px.

## 11. Mouvement

Principe : **aucun mouvement tant qu'il n'a pas de raison.** Une animation montre une cause et son effet (ouverture, fermeture, confirmation, changement d'état) ; elle ne décore pas.

| Token | Valeur | Usage |
|---|---|---|
| `--duration-instant` | 100 ms | retour de pression (bouton) |
| `--duration-fast` | 150 ms | survol, focus, petits changements de couleur |
| `--duration-base` | 200 ms | apparition d'un menu, d'une info-bulle |
| `--duration-slow` | 300 ms | dialogue, panneau de navigation mobile ; plafond pour l'interface |
| `--ease-out` | `cubic-bezier(0.23, 1, 0.32, 1)` | entrées |
| `--ease-in-out` | `cubic-bezier(0.77, 0, 0.175, 1)` | déplacements à l'écran |
| `--ease-exit` | `cubic-bezier(0.4, 0, 1, 1)` ou durée réduite | sorties, plus courtes que les entrées |

Règles : `transform` et `opacity` seulement ; jamais `transition: all` ; jamais depuis `scale(0)` ; transitions CSS interruptibles plutôt qu'images clés ; `@starting-style` pour les entrées ; aucune animation au chargement d'une page publique ; `prefers-reduced-motion: reduce` supprime les déplacements (les fondus très courts peuvent rester) ; aucune boucle automatique.

## 12. Plans (z-index)

| Token | Valeur | Élément |
|---|---|---|
| `--z-base` | 0 | contenu |
| `--z-sticky` | 10 | en-tête collant |
| `--z-dropdown` | 20 | menus |
| `--z-overlay` | 30 | fond de dialogue |
| `--z-dialog` | 40 | dialogues, navigation mobile |
| `--z-toast` | 50 | notifications |
| `--z-skip` | 60 | lien d'évitement |

Un `<dialog>` ouvert par `showModal()` ou un élément `popover` vit dans la couche supérieure du navigateur et n'a pas besoin de z-index.

## 13. Focus

- Style unique dans `base.css` : `:focus-visible { outline: 2px solid var(--color-focus); outline-offset: 2px; }`, contraste ≥ 3:1 avec le fond adjacent.
- Jamais `outline: none` sans remplacement équivalent.
- L'en-tête collant ne masque jamais l'élément qui a le focus (`scroll-padding-top`, WCAG 2.4.11).
- Cibles d'au moins 24 × 24 px (WCAG 2.5.8), 44 × 44 px visés sur mobile.

## 14. Icônes

- Peu d'icônes, toujours accompagnées d'un texte ou d'un nom accessible ; décoratives → `aria-hidden="true"`.
- SVG en ligne par un composant `app-icon` et un registre typé des seules icônes utilisées ; aucun fichier de police d'icônes, aucune bibliothèque complète.
- Tracés repris d'un jeu sous licence permissive (Lucide, ISC) avec mention dans les notices de licence ; trait et taille alignés sur le corps du texte voisin.

## 15. Images et médias

- Toujours `width` et `height` (fournis par l'API), `alt` venant de `altText` ; image décorative → `alt=""`.
- Rapports d'image fixés par emplacement (couverture de projet, avatar) ; recadrage par `object-fit`, jamais de déformation.
- Pas d'image d'illustration générique. Les captures de projets sont réelles (`01-perimetre-v1.md` §20).

## 16. Règles responsive

- Mobile d'abord ; aucun défilement horizontal de la page à 320 px de large (WCAG 1.4.10), seuls les blocs de code et les tableaux défilent dans leur propre conteneur.
- Navigation : liens visibles sur grand écran ; panneau accessible (bouton avec `aria-expanded`, focus contenu, `Échap` ferme) sur petit écran.
- Zoom : jamais bloqué ; mise en page intacte à 200 % ; texte redimensionnable.
- Champs de saisie en 16 px au moins sur mobile (pas de zoom automatique d'iOS).
- Zones sûres : `env(safe-area-inset-*)` sur les éléments collés aux bords.

## 17. Stratégie de style

| Niveau | Où | Règle |
|---|---|---|
| 1. tokens | `styles/tokens.css` | seule source des valeurs ; tout nouveau besoin commence par un token |
| 2. base | `styles/base.css` | éléments HTML nus (`a`, `h1`–`h4`, `code`, `hr`, sélection, focus), mouvement réduit |
| 3. utilitaires du projet | `styles/utilities.css` (`@utility`) | motifs de mise en page transverses : `page-container`, `stack`, `cluster`, `prose` |
| 4. utilitaires Tailwind dans les gabarits | `*.html` | mise en page locale (grille, espacement, alignement) avec les tokens seulement |
| 5. primitives | `shared/ui` | apparence des éléments interactifs (bouton, champ, dialogue) : variantes typées, styles encapsulés |
| 6. style local | `styles` du composant | exception : sélecteurs d'état, `@container`, cas impossibles en utilitaires ; tokens seulement |

Interdits :

- valeur arbitraire dans un gabarit (`p-[13px]`, `text-[#333]`) ; une valeur qui manque devient un token ;
- `@apply` pour reconstruire un composant en CSS (une primitive Angular le fait) ;
- la même combinaison de plus de six utilitaires répétée à trois endroits : elle devient une primitive ou un `@utility` ;
- styles propres à une page qui redéfinissent une primitive ;
- `!important`, sauf `prefers-reduced-motion` ;
- `ngClass` et `ngStyle` (liaisons `class` et `style` natives).

Contrôle : `npm run lint:styles` (F06) signale les valeurs arbitraires et les couleurs littérales dans les gabarits et les styles de composant ; ESLint signale `ngClass` / `ngStyle` ; revue à chaque porte de qualité.

## 18. Primitives

Une primitive n'est créée qu'au premier écran qui en a besoin, et seulement si elle sert au moins deux usages prévus.

| Primitive | Forme | Créée en | Remarque |
|---|---|---|---|
| `page-container`, `stack`, `cluster` | `@utility` CSS | F07 | pas de composant Angular pour de la mise en page |
| `prose` | CSS (`prose.css`) | F11 | rendu du Markdown, le HTML produit ne porte pas de classes |
| lien | styles de base + directive `appLink` si variante | F08 | soulignement lisible, état visité non utilisé |
| bouton | directive `button[appButton]`, `a[appButton]` | F08 | variantes `primary`, `secondary`, `quiet` ; `danger` en F24 ; tailles `sm`, `md` |
| icône | composant `app-icon` | F08 | registre typé |
| lien d'évitement | gabarit du shell | F09 | |
| navigation mobile | `popover` ou `<dialog>` natif | F09 | choix documenté à l'étape |
| état vide, état d'erreur, page introuvable | composants `shared/ui` | F09 | textes qui donnent une suite (« Voir tous les articles ») |
| pagination | composant `app-pagination` (liens, pas boutons) | F13 | URL `?page=` |
| terme (catégorie, tag, technologie) | lien typographique | F13 | pas une pastille par défaut |
| table des matières | composant `shared/markdown` | F14 | |
| champ (libellé, aide, erreur) | composant `app-field` + contrôles natifs stylés | F19 | `input`, `textarea`, `select`, case à cocher |
| message en ligne / région d'annonce | composant `app-alert` | F19 | `role="status"` ou `role="alert"` selon l'urgence |
| dialogue de confirmation | `<dialog>` natif enveloppé | F24 | actions destructrices de l'administration |
| notification (toast) | service + région `aria-live="polite"` | F24 | administration seulement |
| pastille de statut | composant `app-status` | F27 | brouillon, programmé, publié, archivé : texte + couleur |
| tableau de données | gabarit `<table>` stylé + tri côté serveur absent (D-V) | F24 | |
| envoi de fichier | composant `app-file-drop` | F25 | alternative clavier et bouton |
| liste ordonnable | CDK glisser-déposer + boutons monter/descendre | F26 | alternative clavier obligatoire |
| combobox à choix multiples | Angular Aria | F27 | tags, technologies |
| onglets | Angular Aria | F31 | Visuel / Markdown / Aperçu |
| menu | Angular Aria | à la demande | pas de besoin identifié |
| squelette de chargement | CSS | à la demande | seulement si un chargement client dépasse ≈ 300 ms en pratique ; le rendu serveur évite le premier chargement |
| carte générique | — | **exclue** | chaque fonctionnalité compose son entrée (`project-entry`, `publication-entry`) ; une carte générique naît seulement si trois usages identiques apparaissent |

## 19. États d'un écran

Chaque page qui affiche des données prévoit, dès sa première version :

| État | Traitement |
|---|---|
| chargement (navigation client) | contenu précédent conservé, indicateur discret après 150 à 300 ms ; aucun squelette par défaut |
| vide | phrase qui explique et propose une suite |
| erreur | cause en clair (texte de l'API si pertinent), action « Réessayer » |
| introuvable | page 404 avec statut HTTP 404 |
| contenu long | titres de 120 caractères, mots longs (`overflow-wrap: anywhere` sur les identifiants), listes de 50 éléments, code de 200 colonnes |
| contenu minimal | champs facultatifs absents (pas d'image, pas de résumé, pas de date de fin) |

## 20. Rédaction de l'interface

- Français, casse de phrase, voix active ; un bouton dit ce qu'il fait (« Publier », pas « Valider ») et la confirmation reprend le même verbe (« Publié »).
- Les erreurs n'accusent ni ne s'excusent : ce qui s'est passé, puis quoi faire.
- Pas de texte de remplissage ; aucun élément de texte sans fonction.
- Les noms techniques (langages, bibliothèques, identifiants) sont protégés de la traduction automatique (`translate="no"`).

## 21. Décisions

| Réf | Décision | Statut |
|---|---|---|
| DS01 | Direction « Planche technique » (§4.1), palette et polices définitives | **Proposée**, validée en F05 |
| DS02 | Tokens dans `@theme` de Tailwind 4, espaces de noms par défaut réinitialisés ; `tokens.css` source de vérité | Acceptée (F06) |
| DS03 | Polices auto-hébergées sous licence OFL ; aucune requête vers un service de polices tiers | Acceptée |
| DS04 | Stratégie de style en six niveaux (§17), valeurs arbitraires interdites dans les gabarits | Acceptée |
| DS05 | Primitives créées à la demande (§18) ; pas de carte générique | Acceptée |
| DS06 | Tokens de mouvement et principe « aucun mouvement sans raison » (§11) | Acceptée |
| DS07 | Thème clair seulement en V1 (D13), tokens sémantiques prêts pour un thème sombre ultérieur | Acceptée |
| DS08 | Typographie française (§6) | Acceptée |
