import { NgOptimizedImage } from '@angular/common';
import { Component, computed, input } from '@angular/core';

import { PublicImage } from '../../core/api/api-types';

/** Largeur affichée d'une planche de carte : grille de 1, 2 puis 3 colonnes. */
export const CARD_SIZES = '(min-width: 80rem) 28rem, (min-width: 48rem) 50vw, 100vw';
/** Largeur affichée d'une planche de tête : 7 colonnes sur 12 dès `xl`. */
export const LEAD_SIZES = '(min-width: 80rem) 48rem, 100vw';

/** Mots ignorés pour le repère d'une planche vide. */
const SMALL_WORDS = new Set([
  'de',
  'du',
  'des',
  'la',
  'le',
  'les',
  'l',
  'd',
  'un',
  'une',
  'et',
  'en',
  'à',
  'a',
  'pour',
  'sur',
]);

/**
 * Repère d'une planche vide : initiales des deux premiers mots significatifs du titre
 * (« Portfolio full-stack » → « PF »), en capitales.
 */
export function plateMark(title: string): string {
  const words = title
    .split(/[\s'’:,;.!?«»()–—-]+/u)
    .filter((word) => word.length > 0 && !SMALL_WORDS.has(word.toLocaleLowerCase('fr')));
  return words
    .slice(0, 2)
    .map((word) => word.charAt(0).toLocaleUpperCase('fr'))
    .join('');
}

/**
 * Planche d'un contenu (DS09, `styles/plates.css`) : sa couverture recadrée dans un rapport fixe
 * (3:2, ou 16:9 avec `wide`), ou, sans couverture, une planche vide décorative (papier quadrillé et
 * initiales du titre ou repère donné, masquée aux lecteurs d'écran : le titre suit dans la
 * carte). `sizes` dit la largeur affichée, pour que le navigateur ne charge pas plus grand que
 * nécessaire.
 */
@Component({
  selector: 'app-content-plate',
  imports: [NgOptimizedImage],
  host: { class: 'block' },
  template: `
    @if (image(); as cover) {
      <div class="plate" [class.plate-wide]="wide()">
        <!-- priority n'est lu qu'à la création de l'image : deux branches, recréées quand il change.
             sizes est fixe pour une planche : une carte qui passe en tête change de planche. -->
        @if (priority()) {
          <img [ngSrc]="cover.url" fill [sizes]="sizes()" [alt]="cover.altText ?? ''" priority />
        } @else {
          <img [ngSrc]="cover.url" fill [sizes]="sizes()" [alt]="cover.altText ?? ''" />
        }
      </div>
    } @else {
      <div class="plate plate-empty" [class.plate-wide]="wide()" aria-hidden="true">
        @if (label(); as text) {
          <span class="plate-mark plate-label">{{ text }}</span>
        } @else {
          <span class="plate-mark">{{ mark() }}</span>
        }
      </div>
    }
  `,
})
export class ContentPlate {
  readonly image = input<PublicImage | null>(null);
  /** Titre du contenu : initiales de la planche vide. */
  readonly title = input.required<string>();
  /** Repère de la planche vide à la place des initiales (catégorie d'un article). */
  readonly label = input<string | null>(null);
  readonly wide = input(false);
  /** Image du haut de page (LCP) : chargée en priorité. */
  readonly priority = input(false);
  readonly sizes = input(CARD_SIZES);

  protected readonly mark = computed(() => plateMark(this.title()));
}
