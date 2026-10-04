import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SeriesSummary } from '../../core/api/api-types';
import { markdownExcerpt } from '../format/excerpt';
import { ContentPlate } from './content-plate';
import { chapterCountLabel } from './content-labels';

/**
 * Carte de série (DS09) : planche 16:9 (couverture ou planche vide), nombre de chapitres, titre
 * lié à la série, premier paragraphe de la description. Planche à gauche dès 40 rem de conteneur,
 * au-dessus en dessous. Le titre est le seul lien ; sa cible couvre la carte.
 */
@Component({
  selector: 'app-series-card',
  imports: [ContentPlate, RouterLink],
  host: { class: '@container block' },
  template: `
    <article class="card card-series">
      @if (headingLevel() === 2) {
        <h2 class="card-title">
          <a class="card-link" [routerLink]="link()">{{ series().title }}</a>
        </h2>
      } @else {
        <h3 class="card-title">
          <a class="card-link" [routerLink]="link()">{{ series().title }}</a>
        </h3>
      }
      <p class="card-meta">
        <strong class="tabular-nums">{{ chapters() }}</strong>
      </p>
      @if (excerpt()) {
        <p class="card-summary">{{ excerpt() }}</p>
      }
      <app-content-plate
        class="card-plate"
        [image]="series().cover"
        [title]="series().title"
        [wide]="true"
        [priority]="coverPriority()"
        sizes="(min-width: 80rem) 32rem, (min-width: 48rem) 40vw, 100vw"
      />
    </article>
  `,
  styles: `
    @container (min-width: 40rem) {
      .card-series {
        grid-template-columns: repeat(12, minmax(0, 1fr));
        grid-template-rows: auto auto 1fr;
        grid-template-areas: none;
        column-gap: calc(var(--spacing) * 8);
      }

      .card-series .card-plate {
        grid-column: 1 / span 5;
        grid-row: 1 / span 3;
        margin-block-end: 0;
      }

      .card-series .card-meta,
      .card-series .card-title,
      .card-series .card-summary {
        grid-column: 6 / span 7;
      }

      .card-series .card-meta {
        grid-row: 1;
      }

      .card-series .card-title {
        grid-row: 2;
        font-size: var(--text-2xl);
      }

      .card-series .card-summary {
        grid-row: 3;
        max-width: var(--container-prose);
      }
    }
  `,
})
export class SeriesCard {
  readonly series = input.required<SeriesSummary>();
  /** `<h2>` dans la liste des séries, `<h3>` sous une section (accueil). */
  readonly headingLevel = input<2 | 3>(2);
  /** Couverture du haut de page (LCP) : chargée en priorité. */
  readonly coverPriority = input(false);

  protected readonly link = computed(() => ['/series', this.series().slug]);
  protected readonly chapters = computed(() => chapterCountLabel(this.series().chapterCount));
  protected readonly excerpt = computed(() =>
    markdownExcerpt(this.series().descriptionMarkdown, 280),
  );
}
