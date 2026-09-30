# Système de design — Portfolio V1

Statut : **Accepté** (2026-09-30). La direction visuelle (§4) a été choisie par le propriétaire en F05, sur des prototypes réels (accueil, liste de projets, article avec code, formule et figure) capturés de 320 à 1920 px ; les valeurs de §4, §6, §8, §9 et §10 sont définitives.

Source de vérité : [`frontend/src/styles/tokens.css`](../../frontend/src/styles/tokens.css) (F06). Ce document explique les tokens ; en cas d'écart, le fichier de tokens fait foi et ce document est corrigé. Les noms suivent les espaces de noms de Tailwind 4 (`--color-*`, `--text-*`, `--spacing-*`, `--container-*`, `--radius-*`…), qui produisent les utilitaires (`text-ink`, `mt-flow`, `max-w-prose`, `rounded-control`). Le catalogue de développement `/_ui` affiche chaque token appliqué et calcule les contrastes dans le navigateur.

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

## 4. Direction visuelle : « Planche technique » (DS01)

Choisie par le propriétaire le 2026-09-30, parmi trois directions prototypées sur le contenu réel (§4.6), avec un emprunt : les notes marginales de la direction « Monographie », réservées aux articles.

L'univers d'une planche d'ingénierie ou d'une figure scientifique : un fond blanc neutre, une encre bleu-noir, des éléments alignés sur une grille exacte, des repères qui nomment les zones, des légendes, des numéros de figure et d'équation là où le contenu en a réellement. La rigueur se voit dans l'alignement, la typographie et les filets, pas dans la décoration. Le caractère reste celui de l'ingénierie et de la recherche : le site n'est ni un tableau de bord, ni une revue littéraire.

- **Élément mémorable (un seul)** : la page d'article, mise en page comme une planche — sommaire en marge gauche, métadonnées puis notes en marge droite sur grand écran, figures et équations numérotées et légendées, code sur fond en retrait.
- **Rappel discret** : sur l'accueil, un cartouche (le bloc-titre d'un plan) qui réunit des faits réels : rôle, pile principale, nombre de projets et de publications, date de mise à jour.
- **Tout le reste reste calme** : accueil typographique sans image d'illustration, listes en registres sobres, navigation discrète.

### 4.1 Palette

| Token | Valeur | Usage | Contraste mesuré |
|---|---|---|---|
| `paper` | `#FFFFFF` | fond | — |
| `paper-sunken` | `#F3F5F8` | code, encadrés, fond en retrait | — |
| `ink` | `#1A2233` | texte, titres, bordure d'un contrôle | 15,90:1 sur `paper`, 14,56:1 sur `paper-sunken` |
| `ink-muted` | `#4A5568` | texte secondaire, métadonnées, légendes | 7,53:1 sur `paper`, 6,89:1 sur `paper-sunken` |
| `rule` | `#D5DBE3` | filets de séparation (décoratifs) | 1,39:1 : jamais seul pour délimiter un contrôle (`ink` dans ce cas) |
| `accent` | `#1E4F9A` (bleu de Prusse) | liens, focus, état courant | 7,95:1 sur `paper`, 7,28:1 sur `paper-sunken` |
| `accent-strong` | `#163B75` | survol et état actif d'un lien | 10,96:1 sur `paper` |
| `selection` | `#D6E2F5` | fond de la sélection de texte | `ink` 12,16:1 dessus |
| `danger` | `#A3261F` | erreur | 7,36:1 sur `paper`, 6,74:1 sur `paper-sunken` |
| `success` | `#1D6B3A` | confirmation | 6,53:1 sur `paper`, 5,98:1 sur `paper-sunken` |
| `warning` | `#855000` | avertissement | 6,68:1 sur `paper`, 6,11:1 sur `paper-sunken` |
| coloration du code | mots-clés `#8A2C5A`, chaînes `#1D6034`, annotations `#7A4A00`, reste en `ink` | code | ≥ 6,85:1 sur `paper-sunken` |

Le texte courant et le texte secondaire atteignent l'AAA (≥ 7:1) sur `paper` ; aucune paire de texte n'est sous 4,5:1. Les signaux ne portent jamais l'information seuls (texte ou icône en plus, §7). Le focus (`accent`) contraste à plus de 7:1 avec les deux fonds.

### 4.2 Typographie

Trois familles sous licence OFL, auto-hébergées par `@fontsource-variable` (version 5.3.0), sous-ensemble latin, fichiers variables `wght` seulement :

| Rôle | Famille | Fichiers et poids (woff2) | Raison |
|---|---|---|---|
| titres et interface (`--font-display`) | Schibsted Grotesk | romain ≈ 47 kB | grotesque d'ingénierie nette, bonne tenue en grand corps avec une approche serrée, chiffres tabulaires |
| lecture (`--font-text`) | Literata | romain ≈ 52 kB, italique ≈ 54 kB | romaine à grand œil conçue pour l'écran, vraie italique, bonne voisine des mathématiques |
| code (`--font-code`) | JetBrains Mono | romain ≈ 40 kB | chasse fixe lisible, distinction nette de `0`/`O` et `1`/`l` ; seulement pour du code réel |

Total ≈ 193 kB. Les feuilles `wght.css` des paquets déclarent chaque sous-ensemble avec `unicode-range` : un texte français ne télécharge que le latin (œ, €, espaces fines compris) ; les autres sous-ensembles sont copiés dans le build sans être demandés. Pas encore de préchargement (01 §15) : les noms de fichiers portent une empreinte, la décision est prise avec les mesures de F34. L'axe optique (`opsz`) de Literata n'est pas retenu : ≈ 110 kB par style pour un gain faible au corps de lecture.

Inter, Roboto, Arial, la police système seule et les familles d'une marque connue (Geist, SF, IBM Plex) sont écartées : ce sont des choix par défaut, pas des choix pour ce sujet.

### 4.3 Grille et rythme

- Grille de 12 colonnes, espacement de colonnes de 2 rem ; gouttière de page de 1 rem (mobile), 2 rem (dès 48 rem), 3 rem (dès 80 rem) ; largeur maximale `--container-content` (88 rem).
- Sur grand écran, une **colonne de repères** (3 colonnes sur 12) nomme chaque zone de la page (« Projet en cours », « Articles », « Série ») ; le contenu occupe les 9 autres. En mobile, le repère passe au-dessus de sa zone.
- Les zones sont séparées par un filet `rule` de 1 px ; l'espace vertical porte la hiérarchie (§8).
- Registres (listes de projets, de publications) : une ligne par entrée, date ou période dans une colonne fixe, titre, puis un attribut (catégorie, état) aligné à droite sur grand écran ; filet entre deux lignes.

### 4.4 Article

- Colonne de lecture en Literata, `--text-lg` (1,125 rem), interlignage 1,7, mesure `--container-prose` (43 rem, 69 à 76 caractères de 1280 à 1920 px).
- Dès `--breakpoint-xl` (80 rem) : trois colonnes (13 rem · lecture · 15 rem, 3,5 rem entre elles) ; sommaire collant en marge gauche, métadonnées en marge droite, puis les notes. En dessous : une colonne, métadonnées et sommaire (encadré d'un filet) sous le titre.
- Code : `paper-sunken`, filet `rule`, rayon `--radius-media`, JetBrains Mono à 0,875 rem, défilement horizontal dans le bloc (jamais dans la page), bloc atteignable au clavier.
- Équations centrées, numéro « (1) » aligné à droite en `ink-muted` ; figures centrées, légende « **Figure 1** texte » sous la figure ; une figure peut déborder de 3 rem de chaque côté de la mesure sur grand écran.
- Navigation de série : bloc encadré de deux filets `ink` de 1,5 px.

### 4.5 Notes marginales (emprunt à « Monographie »)

- Réservées aux articles qui ont des notes (notes de bas de page du Markdown) ; aucune autre page n'en utilise.
- Dès 80 rem : chaque note se place dans la marge droite, à hauteur du paragraphe qui l'appelle, sous les métadonnées ; linéale à 0,875 rem, `ink-muted`, numéro en `accent`, filet gauche `rule`.
- En dessous de 80 rem, et au zoom 200 % : la note revient dans le flux, juste après le paragraphe qui l'appelle, avec la même présentation.
- L'appel de note est un lien vers la note ; l'ordre du document (paragraphe, puis note) est l'ordre de lecture, identique à l'écran et au lecteur d'écran.

### 4.6 Directions écartées

| Direction | Idée | Raison de l'écart |
|---|---|---|
| « Instrument » | fond gris froid, graphite, accent vert-bleu, superfamille Source ; listes en tableaux | lecture rapide excellente, mais rendu d'outil d'administration et accent seulement AA (5,69:1) |
| « Monographie » | romaine dominante (Newsreader), accent rouge sombre, page de titre et sommaire | lecture longue élégante, mais caractère littéraire trop marqué pour le sujet et polices les plus lourdes (≈ 350 kB) ; seules ses notes marginales sont reprises (§4.5) |

---

## 5. Architecture des tokens

Trois niveaux, un seul fichier :

```text
primitifs (valeurs brutes)      --palette-ink-900: …   jamais utilisés dans un gabarit
        ↓
sémantiques (rôles)             --color-ink, --color-accent, --text-lg, --spacing-section …
        ↓                        seuls utilisés par les gabarits et les primitives
composant (rare)                --button-height … seulement si une primitive en a besoin
```

Mise en œuvre avec Tailwind CSS 4 (`@theme`) :

- les espaces de noms par défaut de Tailwind sont **réinitialisés** (`--color-*: initial;`, `--font-*: initial;`, `--radius-*: initial;`, `--shadow-*: initial;` …) : seuls les tokens du projet produisent des utilitaires (`text-ink`, `bg-paper`, `rounded-control`). Une couleur hors système n'existe tout simplement pas ;
- chaque token sémantique est aussi une propriété CSS personnalisée (`var(--color-ink)`), utilisable dans un style de composant ;
- les primitifs portent un préfixe (`--palette-*`) hors des espaces de noms de Tailwind : ils ne génèrent aucun utilitaire.

## 6. Typographie

| Token | Valeur | Usage |
|---|---|---|
| `--font-display` | Schibsted Grotesk | titres, navigation, interface, métadonnées |
| `--font-text` | Literata | texte long (`prose`), présentations, descriptions |
| `--font-code` | JetBrains Mono | code seulement |
| `--text-xs` | 0,875 rem | notes, cellules du cartouche |
| `--text-sm` | 0,9375 rem | métadonnées, dates, légendes |
| `--text-base` | 1 rem | interface |
| `--text-lg` | 1,125 rem | lecture (Literata), titres de registre |
| `--text-xl` | 1,375 rem | titre d'une entrée (projet), rôle sur l'accueil |
| `--text-2xl` | 1,5 rem | titres de section d'un article |
| `--text-3xl` | `clamp(2rem, 1.5rem + 2.2vw, 3.125rem)` | titre de page et d'article |
| `--text-4xl` | `clamp(2.5rem, 1.6rem + 3.6vw, 4.75rem)` | nom sur l'accueil (un seul usage) |
| `--leading-tight` · `--leading-snug` · `--leading-ui` · `--leading-prose` | 1,05 · 1,25 · 1,5 · 1,7 | grands titres · titres · interface · lecture |
| `--tracking-display` · `--tracking-title` · `--tracking-heading` | −0,035 em · −0,03 em · −0,02 em | `--text-4xl` · `--text-3xl` · `--text-xl` et `--text-2xl` |
| graisses | 400 · 550 · 650 | texte · liens d'action et navigation · titres et repères |
| `--container-prose` | 43 rem | mesure de lecture (69 à 76 caractères de Literata à 18 px) |

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
- Pas utilisés : 1, 2, 3, 4, 5, 6, 8, 10, 12, 14, 16, 24 (4 px à 96 px). Les autres multiples ne sont pas utilisés.
- Rythme vertical : `--spacing-flow` 1,25 rem (entre éléments d'un bloc, paragraphes), `--spacing-block` 2 rem puis 2,5 rem dès `lg` (entre blocs, hauteur d'une zone), `--spacing-section` 3 rem puis 4 rem dès `lg` (en-tête de page, avant le pied de page) ; utilitaires `mt-flow`, `py-block`, `pb-section`…
- Gouttière de page `--spacing-gutter` (`px-gutter`) : 1 rem, 2 rem dès `md`, 3 rem dès `xl` ; espacement des colonnes de la grille : 2 rem.

## 9. Rayons, bordures, ombres

| Token | Valeur | Usage |
|---|---|---|
| `--radius-control` | 3 px | boutons, champs |
| `--radius-media` | 2 px | images, blocs de code |
| `--radius-full` | 9999 px | avatar seulement |
| filet | 1 px `--color-rule` | séparation de zones, de lignes de registre, de blocs |
| trait fort | 1,5 px `--color-ink` | cartouche, bornes de la navigation de série, bordure d'un contrôle |
| `--shadow-overlay` | une seule ombre | éléments flottants (menu, dialogue) — aucune ombre sur le contenu posé dans la page |

La hiérarchie vient de l'espace et de la typographie, pas des boîtes.

## 10. Largeurs et points de rupture

| Token | Valeur | Usage |
|---|---|---|
| `--container-prose` | 43 rem | texte long, colonne de lecture (`max-w-prose`) |
| `--container-content` | 88 rem | toutes les pages (accueil, listes, article avec ses marges) (`max-w-content`) |
| `--breakpoint-sm` | 40 rem | |
| `--breakpoint-md` | 48 rem | gouttière de 2 rem ; registres sur trois colonnes |
| `--breakpoint-lg` | 64 rem | grille de 12 colonnes et colonne de repères ; navigation visible |
| `--breakpoint-xl` | 80 rem | marges d'article (sommaire, métadonnées, notes) |

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
| 3. utilitaires du projet | `styles/utilities.css` (`@utility`) | motifs de mise en page transverses : `page-container`, `stack-*`, `cluster-*` (F07), `prose` (F11) |
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
| `page-container`, `stack-*`, `cluster-*` | `@utility` CSS | **fait** (F07) | `page-container` : `max-w-content` et gouttière ; `stack-flow` / `stack-block` / `stack-section` : espace entre frères (`> * + *`) ; `cluster-<pas>` ou `cluster-flow` : ligne qui passe à la ligne, alignée sur la ligne de base ; démonstrations dans `/_ui` ; pas de composant Angular pour de la mise en page |
| `prose` | CSS global (`styles/prose.css`) | **fait** (F11) | rendu du Markdown : mesure `max-w-prose`, Literata `text-lg` / `leading-prose`, titres en Schibsted Grotesk, code sur `paper-sunken` avec filet (coloration par les tokens `code-*`), tableaux à filets défilant dans leur conteneur, citations à filet, listes de tâches avec état énoncé, liens externes suivis de ↗ (texte de remplacement vide) et annoncés « (site externe) » ; spécimen `/_ui/prose` |
| lien | styles de base (`base.css`) | **fait** (F08) | souligné au repos (1 px), `accent-strong` et trait de 2 px au survol (pointeurs fins seulement) et à l'activation, état visité non distingué, aucune transition ; lien externe : classe `external` (↗ sans texte de remplacement) et texte masqué « (site externe) », commun au Markdown et au profil depuis F12 ; pas de directive tant qu'aucune variante n'est nécessaire |
| bouton | directive `button[appButton]`, `a[appButton]` (`shared/ui/button.ts`) | **fait** (F08) | variantes `primary` (fond `ink`, survol `accent-strong`), `secondary` (trait fort `ink`), `quiet` (texte `accent`) ; `danger` en F24 ; tailles `md` (44 px) et `sm` (32 px) ; `disabled` retire de la tabulation, `aria-disabled="true"` garde le focus et ignore l'activation ; `loading` garde le libellé, pose `aria-busy` et ignore les activations (écouteur en phase de capture) ; les libellés longs passent à la ligne ; retour de pression `scale(0.97)` en 100 ms `--ease-out`, absent sous mouvement réduit, à l'état désactivé et pendant le chargement |
| icône | composant `app-icon` + registre `shared/ui/icons.ts` | **fait** (F08) | `menu`, `close` (tracés Lucide, `frontend/THIRD-PARTY-NOTICES.md`) ; 1,25 em, `currentColor`, `aria-hidden` |
| lien d'évitement | gabarit du shell (`layout/public-shell`) | **fait** (F09) | premier élément focalisable, visible au focus ; cible l'adresse courante suivie de `#contenu` (avec `<base href="/">`, un `#contenu` seul renverrait à la racine) |
| navigation principale et mobile | `layout/public-shell/site-nav.ts` | **fait** (F09) | motif « disclosure » : bouton `Menu` (`aria-expanded`, `aria-controls`) hors du `<nav>`, même liste de liens pour le bureau et le mobile, dépliée **dans le flux** sous l'identité (ni `popover` ni `<dialog>` : rien n'est recouvert, aucun focus à piéger) ; `Échap` referme et rend le focus au bouton, une navigation referme ; lien courant repéré par un filet `accent` (`aria-current="page"`) ; aucun rendu tant qu'aucune page publique n'existe ; sans animation (décision de mouvement en F20) |
| état vide, état d'erreur, page introuvable | `shared/ui/empty-state.ts`, `shared/ui/error-state.ts`, `features/not-found` | **fait** (F09) | état vide : phrase + suite projetée ; erreur : `role="alert"`, texte de l'API si fourni, « Réessayer » ; page introuvable : repère « Erreur 404 » dans la colonne de repères, statut HTTP 404, liens de sortie ajoutés avec les pages publiques |
| registre de parcours | composants de `features/about/ui` (`app-career-entry`, listes) | **fait** (F12) | propre à la page À propos : une entrée par `<li>`, titre `<h3>` puis précisions, date ou période en colonne fixe de 12 rem dès que la liste (conteneur de requête) dispose de 36 rem, filet entre deux entrées ; compétences en liste de définitions, séparateurs « · » sans texte de remplacement ; déplacé dans `shared/` seulement si une autre fonctionnalité en a besoin |
| pagination | composant `app-pagination` (liens, pas boutons) | F13 | URL `?page=` |
| terme (catégorie, tag, technologie) | lien typographique | F13 | pas une pastille par défaut |
| table des matières | composant `shared/markdown` | F14 | |
| note marginale | notes de bas de page du Markdown (`markdown-it-footnote`), rendues après leur paragraphe ; styles `prose` | F14 (rendu et marge de l'article) | §4.5 : en marge dès `xl`, dans le flux en dessous ; greffon ajouté avec la page d'article (ADR 0003) |
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
| DS01 | Direction « Planche technique » (§4) : palette, polices, grille et rythme définitifs ; notes marginales reprises de « Monographie » pour les articles seulement (§4.5) | **Acceptée** (2026-09-30, choix du propriétaire en F05) |
| DS02 | Tokens dans `@theme` de Tailwind 4, thème par défaut entièrement retiré (`--*: initial`) ; `tokens.css` source de vérité | Acceptée, mise en œuvre (F06) |
| DS03 | Polices auto-hébergées sous licence OFL ; aucune requête vers un service de polices tiers | Acceptée |
| DS04 | Stratégie de style en six niveaux (§17), valeurs arbitraires interdites dans les gabarits | Acceptée, contrôlée par `npm run lint:styles` (F06) |
| DS05 | Primitives créées à la demande (§18) ; pas de carte générique | Acceptée |
| DS06 | Tokens de mouvement et principe « aucun mouvement sans raison » (§11) | Acceptée |
| DS07 | Thème clair seulement en V1 (D13), tokens sémantiques prêts pour un thème sombre ultérieur | Acceptée |
| DS08 | Typographie française (§6) | Acceptée |
