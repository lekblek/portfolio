import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PublicationSummary } from '../../core/api/api-types';
import { formatDay, isoDay } from '../format/date';
import { TermLinks } from '../ui/term-link';
import { CARD_SIZES, ContentPlate, LEAD_SIZES } from './content-plate';
import { publicationListPath, publicationPath, readingTimeLabel } from './content-labels';

/**
 * Carte d'article (DS09, `styles/plates.css`) : planche (couverture ou planche vide), catégorie
 * (lien vers la liste filtrée), date et temps de lecture, titre lié, résumé. `lead` : article de
 * tête, planche à gauche, résumé plus long et tags. Le titre est le lien principal ; sa cible
 * couvre la carte, la catégorie et les tags restent atteignables au-dessus.
 */
@Component({
  selector: 'app-publication-card',
  imports: [ContentPlate, RouterLink, TermLinks],
  host: { class: '@container block' },
  template: `
    <article class="card" [class.card-lead]="lead()">
      @if (headingLevel() === 2) {
        <h2 class="card-title">
          <a class="card-link" [routerLink]="link()">{{ publication().title }}</a>
        </h2>
      } @else {
        <h3 class="card-title">
          <a class="card-link" [routerLink]="link()">{{ publication().title }}</a>
        </h3>
      }
      <p class="card-meta">
        @if (publication().category; as category) {
          <a
            class="card-category"
            [routerLink]="listPath()"
            [queryParams]="{ category: category.slug }"
            [attr.aria-current]="category.slug === currentCategory() ? 'true' : null"
            ><span class="sr-only">Catégorie&nbsp;: </span>{{ category.name }}</a
          ><span class="sr-only">, </span>
        }
        <time class="tabular-nums" [attr.datetime]="datetime()">{{ date() }}</time
        ><span class="sr-only">, </span>
        <span>{{ readingTime() }}</span>
      </p>
      <p class="card-summary">{{ publication().summary }}</p>
      @if (lead() && publication().tags.length > 0) {
        <div class="card-terms">
          <app-term-links
            [terms]="publication().tags"
            [label]="'Tags de ' + publication().title"
            [path]="listPath()"
            param="tag"
            [current]="currentTag()"
          />
        </div>
      }
      <!-- Une carte qui passe en tête change de planche : sizes n'est lu qu'à la création (NG02953) -->
      @if (lead()) {
        <app-content-plate
          class="card-plate"
          [image]="publication().cover"
          [title]="publication().title"
          [label]="publication().category?.name ?? null"
          [wide]="true"
          [priority]="coverPriority()"
          [sizes]="leadSizes"
        />
      } @else {
        <app-content-plate
          class="card-plate"
          [image]="publication().cover"
          [title]="publication().title"
          [label]="publication().category?.name ?? null"
          [priority]="coverPriority()"
          [sizes]="cardSizes"
        />
      }
    </article>
  `,
  styles: `
    .card-category {
      display: inline-flex;
      align-items: center;
      min-height: calc(var(--spacing) * 6);
      font-weight: var(--font-weight-semibold);
    }

    .card-category[aria-current] {
      color: var(--color-ink);
    }
  `,
})
export class PublicationCard {
  readonly publication = input.required<PublicationSummary>();
  /** `<h2>` dans une liste, `<h3>` sous une section (accueil). */
  readonly headingLevel = input<2 | 3>(2);
  readonly lead = input(false);
  /** Filtres en cours, marqués parmi les liens. */
  readonly currentCategory = input<string | null>(null);
  readonly currentTag = input<string | null>(null);
  /** Couverture du haut de page (LCP) : chargée en priorité. */
  readonly coverPriority = input(false);

  protected readonly cardSizes = CARD_SIZES;
  protected readonly leadSizes = LEAD_SIZES;
  protected readonly listPath = computed(() => publicationListPath(this.publication().type));
  protected readonly link = computed(() =>
    publicationPath(this.publication().type, this.publication().slug),
  );
  protected readonly date = computed(() => formatDay(this.publication().publishedAt));
  protected readonly datetime = computed(() => isoDay(this.publication().publishedAt));
  protected readonly readingTime = computed(() =>
    readingTimeLabel(this.publication().readingTimeMinutes),
  );
}
