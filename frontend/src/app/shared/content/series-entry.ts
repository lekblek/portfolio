import { NgOptimizedImage } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SeriesSummary } from '../../core/api/api-types';
import { markdownExcerpt } from '../format/excerpt';

/** « 1 chapitre », « 4 chapitres ». */
export function chapterCountLabel(count: number): string {
  return `${count} ${count <= 1 ? 'chapitre' : 'chapitres'}`;
}

/**
 * Ligne du registre des séries (liste, accueil) : nombre de chapitres, titre lié à la série,
 * premier paragraphe de la description en texte brut, couverture s'il y en a une.
 */
@Component({
  selector: 'app-series-entry',
  imports: [NgOptimizedImage, RouterLink],
  template: `
    <article class="register-entry" [class.register-with-cover]="series().cover">
      @if (headingLevel() === 2) {
        <h2 class="register-title">
          <a [routerLink]="link()">{{ series().title }}</a>
        </h2>
      } @else {
        <h3 class="register-title">
          <a [routerLink]="link()">{{ series().title }}</a>
        </h3>
      }
      <p class="register-meta">
        <span class="font-medium tabular-nums">{{ chapters() }}</span>
      </p>
      @if (excerpt()) {
        <p class="register-summary max-w-prose font-text leading-prose">{{ excerpt() }}</p>
      }
      @if (series().cover; as cover) {
        <div class="register-cover">
          <!-- priority n'est lu qu'à la création de l'image : deux branches, recréées quand il change -->
          @if (coverPriority()) {
            <img [ngSrc]="cover.url" fill [alt]="cover.altText ?? ''" priority />
          } @else {
            <img [ngSrc]="cover.url" fill [alt]="cover.altText ?? ''" />
          }
        </div>
      }
    </article>
  `,
})
export class SeriesEntry {
  readonly series = input.required<SeriesSummary>();
  /** `<h2>` dans la liste des séries, `<h3>` sous une section (accueil). */
  readonly headingLevel = input<2 | 3>(2);
  /** Couverture de la première ligne, au-dessus de la ligne de flottaison : chargée en priorité. */
  readonly coverPriority = input(false);

  protected readonly link = computed(() => ['/series', this.series().slug]);
  protected readonly chapters = computed(() => chapterCountLabel(this.series().chapterCount));
  protected readonly excerpt = computed(() =>
    markdownExcerpt(this.series().descriptionMarkdown, 280),
  );
}
