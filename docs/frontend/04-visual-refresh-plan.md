# Passe visuelle — plan (V1, avant F31)

Statut : **réalisé** (2026-10-04), état final au §8. Plan de travail de la passe visuelle menée entre F30 et F31 ; il sert directement à l'implémentation et se met à jour avec elle (§6, §7). Décision durable correspondante : DS09 dans [`02-design-system.md`](02-design-system.md) §21 et D-FA dans le registre ([`../decisions/registre-implementation.md`](../decisions/registre-implementation.md)).

Documents liés : [`02-design-system.md`](02-design-system.md) (DS01, tokens, primitives), [`01-architecture.md`](01-architecture.md), [`../01-perimetre-v1.md`](../01-perimetre-v1.md) (§15 SEO, §16 accessibilité, D13, D17).

---

## 1. État actuel

Inspection réelle le 2026-10-03, jeu de démonstration du profil `dev` (24 projets dont 16 publiés, 16 articles, 11 actualités, 2 séries, profil complet), à 390, 768, 1440 et 1920 px : aucune violation axe, aucun débordement horizontal.

Forces à préserver :

- **Typographie** (Schibsted Grotesk, Literata, JetBrains Mono) et palette encre / bleu de Prusse : le site se reconnaît déjà à sa lecture.
- **Page d'article** (élément mémorable de DS01) : sommaire en marge, fiche et notes à droite, figures et équations numérotées, code lisible.
- **Rigueur** : rendu serveur sans appel rejoué, états vides et d'erreur, 404 réelles, pagination par liens, focus, contrastes AAA.
- **Administration** : densité d'outil, tableaux nets, statuts lisibles, actions confirmées.

Faiblesse principale : **la même ligne de registre partout** (date ou état · titre · résumé · termes · vignette 3:2 à droite · filet), sur l'accueil, les projets, les articles, les actualités, les séries et la recherche. Propre, mais le site se lit comme un tableau éditorial ; les médias du jeu de démonstration restent des vignettes de 300 px, et rien ne distingue un projet d'un article ou d'une actualité au premier coup d'œil.

## 2. Benchmark

| Référence | Ce qui fonctionne | Pourquoi | Repris ici | À ne pas reproduire |
|---|---|---|---|---|
| Brittany Chiang (brittanychiang.com) | identité fixe à gauche, contenu qui défile à droite ; parcours en colonne de dates ; projets avec capture | le visiteur garde le « qui » en vue pendant qu'il lit le « quoi » | parcours en frise datée (À propos) ; captures en vrai format | la mise en page signature (identité collante, fond sombre), devenue un gabarit copié partout |
| Josh W. Comeau (joshwcomeau.com) | catégories et « contenus populaires » distincts du fil ; personnalité forte | plusieurs entrées dans le contenu selon l'intention | une publication de tête puis une grille ; catégories en évidence | illustrations et animations ludiques, hors du ton technique |
| Rauno Freiberg (rauno.me) | retenue extrême, soin des détails d'interaction | la qualité d'exécution tient lieu de décoration | micro-interactions utiles seulement (survol d'une entrée, focus) | minimalisme sans contexte : un recruteur doit comprendre en 5 s |
| Lee Robinson, Paco Coursey, Emil Kowalski | énoncé court, listes courtes, une ligne de description | lecture immédiate | un énoncé de valeur d'une phrase, des descriptions courtes | tout-texte : nos projets ont des visuels réels, il faut s'en servir |
| Max Böck (mxb.dev) | articles longs en grille avec vignettes ; notes courtes séparées | le format suit la longueur du contenu | **actualités en dépêches** denses, distinctes des articles illustrés | métriques d'engagement affichées |
| Lynn Fisher (lynnandtonic.com) | site personnel versionné, assumé | une identité qui n'est pas un gabarit | une signature propre (la planche technique) plutôt qu'un style à la mode | refonte complète annuelle |
| Bruno Simon (bruno-simon.com) | mémorable, le site *est* la démonstration | l'expérience prouve la compétence | rien de la forme ; l'idée que le site lui-même prouve le savoir-faire | 3D, son, jeu : lourd, inaccessible, hors sujet |
| Stripe (blog d'ingénierie) | une publication de tête, puis une liste dense avec date, auteur et sujets ; filtres en marge | hiérarchie nette sans images | tête puis liste, filtres conservés | — |
| Awwwards, SiteInspire (catégorie portfolio) | grandes images, galeries, études de cas narratives, grilles éditoriales | l'image porte la preuve | couvertures structurantes, étude de cas en sections, galerie de captures | plein écran, défilement détourné, apparitions animées, typographie démesurée |
| Ghost (éditeur d'administration) | rédaction visuelle avec cartes (image, code), Markdown possible | l'auteur reste dans son texte | F31 : onglets Visuel / Markdown / Aperçu, insertion depuis la médiathèque | HTML comme source : ici le Markdown reste canonique |

Principes retenus :

1. **L'image prouve, le texte explique** : un projet ou un article qui a une couverture la montre en grand là où il est mis en avant ; ailleurs, un format de liste dense.
2. **Une forme par type de contenu** : projet = planche illustrée ; article = publication de tête puis grille ; actualité = dépêche datée sans image imposée ; série = parcours numéroté ; recherche = type annoncé par un repère distinct.
3. **Hiérarchie par composition** : une entrée de tête, puis des entrées secondaires ; jamais dix lignes identiques.
4. **Comprendre en 5 secondes** sur l'accueil : qui, quoi, domaines, meilleurs projets, écrits.
5. **Pas de cliché** : ni bento, ni dégradé, ni verre dépoli, ni ombre posée sur le contenu, ni apparition animée au défilement.

## 3. Direction retenue

**« Planche technique, illustrée »** : DS01 garde sa palette, ses polices, sa grille, ses filets et son trait fort ; les médias deviennent des **planches** (cadre à filet, rapport fixe, légende quand elle informe) qui structurent les pages de contenu. Le site doit paraître : professionnel, technique, éditorial, distinctif, crédible pour un ingénieur logiciel et IA ; riche visuellement sans décoration ; sobre sans être vide.

Ce qui change par rapport à DS01 §4 (décision DS09) :

- l'accueil n'est plus « typographique sans image » : il montre le portrait (avatar) et les couvertures des projets et articles mis en avant ;
- les listes de projets, d'articles et de séries quittent le registre pour des compositions illustrées ; le registre reste pour les actualités (en dépêches), la recherche, les chapitres et l'administration ;
- un contenu sans couverture garde sa place dans une composition illustrée grâce à une **planche vide** : papier quadrillé (`rule` sur `paper-sunken`) et repère typographique, décorative (`aria-hidden`) ;
- un survol d'entrée (pointeur fin) : filet de la planche en `accent`, image agrandie de 2 % en 300 ms ; rien sous mouvement réduit (révise D-EL).

## 4. Audit page par page

Priorités : **P1** (cette passe, structurant), **P2** (cette passe, finition), **P3** (reporté, motivé).

| Page | Constat | Problème | Modification | Composants | Prio. | Responsive | Accessibilité | Performance |
|---|---|---|---|---|---|---|---|---|
| `/` | nom, titre, phrase, CV, liens ; cartouche « En bref » en tableau ; quatre registres identiques | se lit comme un CV ; le cartouche ressemble à un formulaire administratif ; aucun visuel ; les domaines n'apparaissent pas | ouverture en deux colonnes : énoncé (nom, titre, présentation, actions « Voir les projets », CV, « Écrire un message ») et portrait en planche légendée avec faits réels ; bande « Domaines » tirée des groupes de compétences ; projets mis en avant en composition (un grand, deux moyens) ; derniers articles avec couvertures ; séries en planches ; actualités en dépêches ; invitation au contact | `home-page`, `home-intro` (nouveau), `domain-strip` (nouveau), `project-card`, `publication-card`, `series-card`, `news-item`, `title-block` (fondu dans la planche du portrait) | P1 | une colonne sous `lg` : énoncé puis portrait réduit ; domaines en 2 colonnes à `md`, 4 à `lg` | un `h1`, zones `h2`, portrait avec `alt` ; actions de 44 px | portrait prioritaire seulement s'il est l'image LCP (≥ `lg`) ; couvertures en `lazy` sauf la première planche visible |
| `/projects` | 10 lignes de registre, vignette à droite | aucune hiérarchie ; images réduites | page 1 sans filtre : premier projet en planche large (image 7/12, texte 5/12), puis grille de planches (2 colonnes dès `md`, 3 dès `xl`) ; avec filtre ou page > 1 : grille seule ; au plus 4 technologies par planche (+ n) ; état et période en tête | `project-list`, `project-card` (nouveau, remplace `project-entry` dans les listes) | P1 | 1 / 2 / 3 colonnes | titre `h2` lié, image `alt` vide si sans texte alternatif (le titre suit), ordre titre → méta → résumé | première couverture prioritaire, autres `lazy`, `sizes` par colonne |
| `/projects/:slug` | titre, résumé, code source, couverture 16:9, Markdown, captures | fiche en marge gauche pauvre ; couverture non prioritaire (NG02955) | en-tête d'étude de cas : titre, résumé, bande de faits (état, période, technologies, liens) ; couverture prioritaire ; sommaire des sections du Markdown en marge dès `xl` ; galerie : première capture en grand, suivantes en grille | `project-detail`, `table-of-contents` (réutilisé) | P1 | une colonne sous `xl`, sommaire replié | `nav` du sommaire nommé ; figures légendées | `priority` sur la couverture |
| `/articles` | registre de 10 lignes | répétitif, couvertures en vignettes | page 1 sans filtre : article de tête (couverture 16:9, résumé long) puis grille de cartes (couverture 3:2 ou planche vide, catégorie, date, lecture, titre, résumé) ; filtres conservés | `publication-list`, `publication-card` (nouveau) | P1 | 1 / 2 / 3 colonnes | catégorie lien distinct du titre ; dates `<time>` | 1re couverture prioritaire |
| `/articles/:slug` | page planche de DS01 | couverture non prioritaire (NG02955) | `priority` sur la couverture ; aucune autre modification (élément mémorable) | `publication-detail` | P2 | — | — | LCP corrigé |
| `/news` | même registre que les articles | les actualités, courtes, imitent les articles | dépêches groupées par mois : date en colonne typographique (jour, mois), titre, résumé, vignette facultative ; ni catégorie mise en avant ni carte | `publication-list` (mode actualités), `news-item` (nouveau) | P1 | date au-dessus sous `md` | groupes de mois en `h2`, liste par groupe | vignettes `lazy` |
| `/news/:slug` | gabarit d'article | couverture non prioritaire | `priority` | `publication-detail` | P2 | — | — | LCP |
| `/series` | 2 lignes de registre, beaucoup de vide | aucune identité de série | planches larges : couverture 16:9 à gauche, titre, description, nombre de chapitres, action « Voir les n chapitres » | `series-list`, `series-card` (nouveau) | P1 | image au-dessus sous `md` | action nommée avec la série | 1re couverture prioritaire |
| `/series/:slug` | titre, description, couverture géante sous un grand vide, chapitres en registre | couverture disproportionnée ; aucune action pour commencer | en-tête : texte (7/12) et couverture 3:2 (5/12) ; « Commencer par le chapitre 1 » ; chapitres en parcours numéroté relié (la numérotation est une vraie séquence) | `series-detail`, `chapter-list` | P1 | couverture sous le texte en mobile | liste ordonnée `ol` | couverture prioritaire |
| `/about` | bonne ouverture ; parcours en registre ; compétences en tableau | longues sections uniformes ; pas de frise ; certifications en lignes | frise des expériences et formations (filet vertical, repères) ; compétences en colonnes par groupe ; certifications en planches compactes | `experience-list`, `education-list`, `skill-groups`, `certification-list` | P2 | frise à une colonne sous `md` | listes sémantiques conservées | — |
| `/contact` | formulaire centré, grande colonne vide à gauche | vide, aucun contexte | deux colonnes dès `lg` : contexte (sujets, liens professionnels, CV) dans la marge, formulaire à droite | `contact-page` | P1 | contexte après le formulaire en mobile (l'action d'abord) | ordre de lecture : titre, formulaire, contexte | une requête de profil ajoutée, lue au rendu serveur |
| `/search` | champ seul, page vide ; résultats en registre « Projet / Actualité » | état initial inachevé ; types peu distincts | état initial : trois entrées (Projets, Articles, Actualités) avec leur rôle ; résultats : repère de type distinct (forme et mot), décompte par type | `search-page`, `search-result-entry` | P2 | — | type en texte, jamais seulement en forme | aucune image (P3 : couverture dans les résultats, demande l'accès du module `search` à `media`, ADR 0002) |
| en-tête / pied | identité et liens | pied pauvre (4 liens) | pied : plan du site complet (7 entrées) en colonnes ; pas d'appel au profil (D-EC) | `site-footer` | P2 | colonnes empilées | `nav` nommée | — |
| `/admin/login` | formulaire de 750 px collé à gauche | inachevé | panneau centré de 28 rem, identité du site, trait fort | `login-page` | P2 | pleine largeur en mobile | inchangé | — |
| `/admin` | deux relevés | pas d'accès direct au travail | ajouter « Derniers messages non lus » et « Brouillons récents » (5 lignes réelles chacun) et actions « Nouvel article », « Nouveau projet » ; aucun graphique (pas de série temporelle réelle) | `dashboard-page` | P2 | une colonne | listes nommées | deux requêtes de plus, petites |
| autres écrans d'administration | tableaux, filtres, statuts déjà cohérents (F24 à F30) | — | aucun changement dans cette passe | — | — | — | — | — |

## 5. Système visuel

| Sujet | Règle |
|---|---|
| rapports d'image | tokens `--aspect-cover` (3:2, planches et cartes), `--aspect-wide` (16:9, têtes et couvertures de détail), `--aspect-portrait` (4:5, portrait de l'accueil) ; `object-fit: cover` ; `width` et `height` réels (API) |
| planche (`.plate`, `styles/plates.css`) | cadre à filet `rule`, rayon `--radius-media`, fond `paper-sunken`, image recadrée ; planche vide : quadrillage `rule` de 24 px et repère typographique `ink-muted` |
| cartes | pas de carte générique (DS05) : `project-card`, `publication-card`, `series-card` composent planche + texte ; ni ombre ni fond ; le titre est le seul lien (cible étendue à la carte par un pseudo-élément, sans imbriquer d'autres liens) |
| surfaces | `paper` partout ; `paper-sunken` pour les planches, le code, la bande des domaines |
| espacements | pas existants seulement ; grille de cartes : `gap` 2 rem en colonnes, 3 rem en lignes |
| typographie | titres de carte `text-xl` (vedette `text-2xl` puis `text-3xl` dès `lg`) ; résumés Literata ; méta en `text-sm` ; résumés limités à 3 lignes dans les grilles (`line-clamp`), complets dans la tête |
| repères | les repères de zone restent dans la colonne de gauche dès `lg` ; titres de section `h2` |
| badges | `app-status-badge` reste réservé à l'administration ; types de la recherche en repère typographique avec forme (carré, rond, losange) |
| liens et boutons | actions d'en-tête en boutons (`primary` puis `secondary`) ; liens « Tous les … » en `quiet` avec le nombre |
| grilles | `card-grid` (`@utility`) : 1 colonne, 2 dès `md`, 3 dès `xl` ; composition vedette en 12 colonnes |
| mouvement | survol d'une carte (pointeur fin) : filet de la planche en `accent`, image `scale(1.02)` en `--duration-slow` `--ease-out` ; rien sous mouvement réduit ; aucune apparition au défilement |
| mobile | cartes pleine largeur, image au-dessus ; actions empilées à 44 px ; aucun débordement à 320 px |

## 6. Implémentation

Ordre suivi :

1. Tokens (`--aspect-*`) et feuille `styles/plates.css` (planche, planche vide, `card-grid`, survol) ; DS09 dans `02-design-system.md`.
2. Cartes partagées dans `shared/content` : `project-card`, `publication-card`, `series-card`, `news-item` ; les entrées de registre restent pour les chapitres et la recherche.
3. Listes : projets, articles, actualités, séries.
4. Détails : projet (étude de cas), série (en-tête et parcours), priorité LCP des couvertures (projet, article, actualité, série).
5. Accueil : énoncé et portrait, domaines, compositions.
6. À propos (frise, compétences, certifications), contact (deux colonnes), recherche, pied de page.
7. Administration : connexion, tableau de bord.
8. Tests unitaires et E2E adaptés ; boucle visuelle 390 / 768 / 1440 / 1920 ; corrections.

## 7. Definition of Done

- Chaque page du §4 contrôlée dans un navigateur à 390, 768, 1440 et 1920 px, captures relues, défauts corrigés puis recapturés.
- Aucune violation axe (WCAG 2.2 AA), aucun débordement horizontal, focus visible sur chaque carte, ordre de lecture inchangé (titre avant la méta dans le DOM).
- Aucune erreur ni avertissement en console (hors NG0913, KI-38), aucun NG02955 sur les pages de détail.
- Une seule image prioritaire par page, celle du haut de page ; les autres en `lazy` avec `sizes`.
- Lot initial du build de production stable (± 3 kB transférés) ; aucune dépendance ajoutée par la passe.
- Mouvement : seul le survol des cartes, absent sous `prefers-reduced-motion`.
- Tests unitaires et E2E verts (sélecteurs adaptés aux nouvelles compositions), y compris « aucun appel rejoué » du rendu serveur.
- Documentation : ce plan (état final), `02-design-system.md` (DS09, primitives), registre (D-FA), `progress.md`.

## 8. État final

Livré (2026-10-04), contrôlé dans un navigateur à 390, 768, 1440 et 1920 px sur 21 pages publiques et d'administration (84 captures relues) : aucune violation axe, aucun débordement, aucune erreur de console.

| Page | Livré |
|---|---|
| `/` | énoncé et trois actions (« Voir les projets », CV, « Écrire un message »), portrait en planche 4:5 légendée, cartouche en faits chiffrés (projets, publications, pile, dernière publication), bande « Domaines » (groupes de compétences), projet de tête (le premier présenté qui a une couverture) puis grille, article de tête (le plus récent illustré) à côté des derniers titres, séries en planches, actualités en dépêches, bandeau de contact à trait fort |
| `/projects` | grille de planches 3:2 (1, 2, 3 colonnes), quatre technologies au plus (« + n », la technologie filtrée toujours visible) |
| `/projects/:slug` | couverture en planche 16:9, fiche collante en marge dès `lg`, galerie : première capture en grand (sauf exactement deux) |
| `/articles` | grille de cartes ; article de tête (16:9, tags) sur la première page non filtrée quand le premier article a une couverture ; planche vide marquée de la catégorie |
| `/news` | dépêches groupées par mois : jour en grand, mois abrégé, vignette facultative ; ni catégorie ni temps de lecture |
| `/series`, `/series/:slug` | planches 16:9 ; en-tête texte et couverture côte à côte dès `xl`, « Commencer par le chapitre 1 », parcours numéroté relié |
| `/about` | frise des expériences et formations, compétences en colonnes, certifications en planches à trait fort |
| `/contact` | contexte dans la marge (sujets, liens professionnels, CV), formulaire à droite ; en mobile, le contexte suit le formulaire |
| `/search` | état initial « Parcourir » (trois types et leur rôle), repère de forme par type dans les résultats |
| pied de page | plan du site en deux colonnes sur `paper-sunken` |
| `/admin/login`, `/admin` | panneau centré à trait fort ; actions de création, messages non lus et brouillons récents (cinq, réels) |

Écarts au plan, et pourquoi :

- **Projets** : pas de projet de tête dans la liste — le premier projet de l'ordre choisi n'a pas toujours de couverture et le réordonner trahirait cet ordre ; la tête vit sur l'accueil.
- **Détail de projet** : pas de sommaire des sections (il demande le rendu Markdown dans la page, comme l'article ; gain faible sur trois sections) ; P3.
- **Recherche** : pas de couvertures ni de décompte par type (le contrat ne les donne pas ; une couverture demanderait au module `search` d'appeler `media`, frontière d'ADR 0002) ; P3.
- **Séries (liste)** : aucune action en plus du titre lié (le résumé de série ne donne pas son premier chapitre) ; l'action « Commencer » est sur la page de la série.
- **NG02955** relevé en 2026-10-03 sur les **listes** (pas sur les détails, déjà prioritaires) : la couverture prioritaire est maintenant la première des deux premières rangées de la grille.
- **Défaut trouvé par les tests** : une carte qui passe en tête changeait `sizes` d'une image déjà créée (NG02953) ; elle change désormais de planche.

Mesures (build de production) :

| État | Lot initial brut | transféré |
|---|---|---|
| avant la passe | 402,51 kB | 110,39 kB |
| après la passe | 407,75 kB | 111,17 kB (+0,78 kB, dont styles +0,6 kB) |
| après F31 | 410,11 kB | 111,89 kB |

Aucune dépendance ajoutée par la passe. Mouvement : seul le survol des cartes (filet `accent`, image à 102 % en 300 ms, pointeur fin, rien sous mouvement réduit).
