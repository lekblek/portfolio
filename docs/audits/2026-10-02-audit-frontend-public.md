# Audit du frontend public — avant l'administration

Date : 2026-10-02 · Branche : `develop` · HEAD : `faa83f3` (+ mouvement du site public, F20) · étape F21 de la feuille de route du frontend

Instantané daté : ce document n'est pas maintenu. L'état vivant est dans [`../progress.md`](../progress.md) ; les décisions qui en découlent sont D-EL et D-EM ([`../decisions/registre-implementation.md`](../decisions/registre-implementation.md)).

## Méthode et limites

- **Périmètre** : toutes les routes publiques (accueil, projets, articles, actualités, séries, à propos, contact, recherche, page introuvable) et leurs états : contenu, filtre sans résultat, contenu inconnu, page au-delà de la dernière, recherche vide, sans résultat et trop longue, API en échec pendant une navigation.
- **Automatique** : `frontend/e2e/public-a11y.spec.ts` (21 états rendus par le serveur, 4 pages en erreur d'API) sur les projets `desktop` (1440 × 900) et `mobile` (390 × 844, tactile) : statut HTTP, axe-core (WCAG 2.0, 2.1, 2.2, A et AA), un seul `<h1>`, repères `banner`, `main`, `contentinfo` et navigation principale, débordement horizontal à 320, 360, 768, 1024, 1280 et 1920 px. Spécifications existantes et mouvement (`e2e/motion.spec.ts`) rejouées en développement (5 exécutions) et sur le build de production derrière un mandataire local (3 exécutions).
- **Clavier** : parcours complet à la tabulation de 14 pages à 390, 640 (équivalent du zoom à 200 % d'un écran de 1280 px) et 1440 px : focus visible (contour), dans la fenêtre, non recouvert.
- **Visuel** : captures pleine page de toutes les pages à 390 et 1440 px, comparées côte à côte sur une planche (cohérence entre pages).
- **Revue d'interface** : règles des Web Interface Guidelines adaptées au projet (français, thème clair, Angular) sur `layout/public-shell`, `shared/ui`, `shared/content`, `shared/markdown`, les pages publiques et `styles/`.
- **Limites** : Chromium seulement (Firefox et Safari non vérifiés) ; aucun lecteur d'écran réel, l'arbre d'accessibilité en tient lieu (vérification avec NVDA à faire par le propriétaire) ; contenus d'amorçage du profil `dev`, courts.

## Résultats

| Contrôle | Résultat |
|---|---|
| axe, 21 états × 2 projets, 4 pages en erreur × 2 projets | 0 violation |
| Statuts HTTP du rendu serveur | 200 ; 404 (adresse, projet, article et série inconnus, page au-delà de la dernière) ; 400 (recherche de plus de 200 caractères) |
| Structure | un `<h1>` et les repères attendus sur chaque état |
| Débordement horizontal, 320 à 1920 px | aucun |
| Clavier, 14 pages × 3 largeurs | focus visible et dans la fenêtre à chaque arrêt ; un seul cas signalé par l'outil (lien de série sur deux lignes, coin supérieur gauche de sa boîte englobante hors du texte) : faux positif vérifié |
| Mouvement réduit | aucune transition de déplacement (E2E sous `reducedMotion: 'reduce'`) |
| Cohérence visuelle | repère à gauche et contenu sur 9 colonnes dès 64 rem sur toutes les pages, en une colonne en dessous ; registres alignés ; pied de page identique |

Taille des lots du navigateur (build de production, référence pour l'étape de performance) : lot initial 393,62 kB bruts / 107,26 kB transférés ; lots des pages, transférés : accueil 3,87 kB, liste des projets 2,46 kB, projet 2,87 kB, liste des publications 2,57 kB, publication 5,15 kB, liste des séries 1,99 kB, série 2,67 kB, à propos 4,18 kB, recherche 2,90 kB, contact 2,79 kB (plus les formulaires, partagés avec la connexion d'administration), page introuvable 0,87 kB ; moteur Markdown, KaTeX et Mermaid chargés à la demande par les pages qui les utilisent.

## Constats

Priorité : **P1** (bloque l'étape) · **P2** (à corriger) · **P3** (amélioration).

| ID | Priorité | Constat | Traitement |
|---|---|---|---|
| A1 | P3 | Noms accessibles « Sommaire de « … » » et « Série « … » » : espaces ordinaires à l'intérieur des guillemets | corrigé : espaces insécables (entité dans la liaison du gabarit) |
| A2 | P3 | Message d'échec du dessin d'un diagramme : espace ordinaire avant « ; » | corrigé : espace fine insécable |
| A3 | P3 | Formulaire de contact : Sujet et Message sans `autocomplete` | corrigé : `autocomplete="off"` (champs libres) |
| A4 | P3 | Titre saisi avec une espace ordinaire avant « : » (contenu d'amorçage) : à 390 px, une ligne du titre commence par « : » | contenu : règle ajoutée au guide de l'éditeur (`02-design-system.md` §20) ; aide à la saisie avec l'éditeur (F31) |
| A5 | P3 | Page d'un contenu inconnu : après l'hydratation, le navigateur repose la question à l'API (une erreur n'est pas transférée par le rendu serveur) | accepté : comportement documenté (D-EB), sans effet visible |
| A6 | — | `-webkit-tap-highlight-color` et `touch-action` non réglés sur les liens | écarté : la surbrillance native reste le retour de toucher des liens ; pas de délai de double toucher avec `width=device-width` ; boutons en `touch-action: manipulation` |
| A7 | P2 | Lecteur d'écran réel non utilisé | à faire par le propriétaire avec NVDA (parcours : accueil, article avec sommaire et notes, contact) |
| A8 | P2 | `security.allowedHosts` vide (KI-22) | déjà suivi : avant la mise en production (F35) |
| A9 | P3 | Médias servis en une seule taille (KI-38) | déjà suivi : étape de performance (F34) |

Aucun constat P1. Le mouvement ajouté avant l'audit (navigation mobile, pression du bouton « Copier », messages d'issue) n'a introduit aucune violation ni aucun débordement.
