import { NgOptimizedImage } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PublicationSummary, PublicationType } from '../../core/api/api-types';
import { formatDay, isoDay } from '../format/date';
import { TermLinks } from '../ui/term-link';

const LIST_PATHS: Record<PublicationType, string> = {
  ARTICLE: '/articles',
  NEWS: '/news',
};

/** Liste des publications d'un type : `/articles` ou `/news`. */
export function publicationListPath(type: PublicationType): string {
  return LIST_PATHS[type];
}

/** Adresse canonique d'une publication, selon son type. */
export function publicationPath(type: PublicationType, slug: string): string {
  return `${LIST_PATHS[type]}/${slug}`;
}

/** « 4 min de lecture ». */
export function readingTimeLabel(minutes: number): string {
  return `${minutes}\u00a0min de lecture`;
}

/**
 * Ligne du registre des publications (listes, accueil) : date et temps de lecture, titre lié à la
 * publication, résumé, catégorie et tags (liens vers la liste filtrée de même type), couverture
 * s'il y en a une. Même grille que l'entrée de projet : emplacement 3:2 pour la couverture
 * (vignette sur petit écran), 12 colonnes dès 60 rem de conteneur.
 */
@Component({
  selector: 'app-publication-entry',
  imports: [NgOptimizedImage, RouterLink, TermLinks],
  template: `
    <article class="entry" [class.entry-with-cover]="publication().cover">
      @if (headingLevel() === 2) {
        <h2 class="entry-title">
          <a [routerLink]="link()">{{ publication().title }}</a>
        </h2>
      } @else {
        <h3 class="entry-title">
          <a [routerLink]="link()">{{ publication().title }}</a>
        </h3>
      }
      <p class="entry-meta">
        <time class="font-medium tabular-nums" [attr.datetime]="datetime()">{{ date() }}</time
        ><span class="sr-only">, </span>
        <span class="text-ink-muted">{{ readingTime() }}</span>
      </p>
      <p class="entry-summary max-w-prose font-text leading-prose">{{ publication().summary }}</p>
      @if (publication().category || publication().tags.length > 0) {
        <div class="entry-terms">
          @if (publication().category; as category) {
            <a
              class="entry-category"
              [routerLink]="listPath()"
              [queryParams]="{ category: category.slug }"
              [attr.aria-current]="category.slug === currentCategory() ? 'true' : null"
              ><span class="sr-only">Catégorie&nbsp;: </span>{{ category.name }}</a
            >
          }
          @if (publication().tags.length > 0) {
            <app-term-links
              [terms]="publication().tags"
              [label]="'Tags de ' + publication().title"
              [path]="listPath()"
              param="tag"
              [current]="currentTag()"
            />
          }
        </div>
      }
      @if (publication().cover; as cover) {
        <div class="entry-cover">
          <img [ngSrc]="cover.url" fill [alt]="cover.altText ?? ''" [priority]="coverPriority()" />
        </div>
      }
    </article>
  `,
  styles: `
    .entry {
      display: grid;
      grid-template-columns: minmax(0, 1fr);
      grid-template-areas: 'meta' 'title' 'summary' 'terms';
      column-gap: calc(var(--spacing) * 4);
      row-gap: calc(var(--spacing) * 2);
    }

    .entry-with-cover {
      grid-template-columns: minmax(0, 1fr) calc(var(--spacing) * 24);
      grid-template-areas: 'meta cover' 'title cover' 'summary summary' 'terms terms';
    }

    .entry-title {
      grid-area: title;
      font-size: var(--text-xl);
      letter-spacing: var(--tracking-heading);
    }

    .entry-title a {
      color: var(--color-ink);
      text-decoration-line: none;
    }

    .entry-title a:hover,
    .entry-title a:focus-visible {
      color: var(--color-accent-strong);
      text-decoration-line: underline;
    }

    .entry-meta {
      grid-area: meta;
      display: flex;
      flex-wrap: wrap;
      column-gap: calc(var(--spacing) * 2);
      font-size: var(--text-sm);
    }

    .entry-summary {
      grid-area: summary;
    }

    .entry-terms {
      grid-area: terms;
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      column-gap: calc(var(--spacing) * 4);
      margin-block-start: calc(var(--spacing) * 1);
    }

    .entry-category {
      display: inline-flex;
      align-items: center;
      min-height: calc(var(--spacing) * 6);
      font-size: var(--text-sm);
      font-weight: var(--font-weight-medium);
    }

    .entry-category[aria-current] {
      color: var(--color-ink);
      font-weight: var(--font-weight-semibold);
    }

    .entry-cover {
      grid-area: cover;
      align-self: start;
      position: relative;
      aspect-ratio: 3 / 2;
      overflow: hidden;
      border: var(--border-rule) solid var(--color-rule);
      border-radius: var(--radius-media);
      background: var(--color-paper-sunken);
    }

    .entry-cover img {
      object-fit: cover;
    }

    @container (min-width: 60rem) {
      .entry,
      .entry-with-cover {
        grid-template-columns: repeat(12, minmax(0, 1fr));
        grid-template-rows: auto auto 1fr;
        grid-template-areas: none;
        column-gap: calc(var(--spacing) * 8);
        align-items: start;
      }

      .entry-meta {
        grid-column: 1 / span 3;
        grid-row: 1 / span 3;
        flex-direction: column;
        padding-block-start: calc(var(--spacing) * 1);
      }

      .entry-title,
      .entry-summary,
      .entry-terms {
        grid-column: 4 / span 6;
      }

      .entry-title {
        grid-row: 1;
      }

      .entry-summary {
        grid-row: 2;
      }

      .entry-terms {
        grid-row: 3;
      }

      .entry-cover {
        grid-column: 10 / span 3;
        grid-row: 1 / span 3;
      }
    }
  `,
})
export class PublicationEntry {
  readonly publication = input.required<PublicationSummary>();
  /** `<h2>` dans une liste, `<h3>` sous une section (accueil). */
  readonly headingLevel = input<2 | 3>(2);
  /** Filtres en cours, marqués parmi les liens. */
  readonly currentCategory = input<string | null>(null);
  readonly currentTag = input<string | null>(null);
  /** Couverture de la première ligne, au-dessus de la ligne de flottaison : chargée en priorité. */
  readonly coverPriority = input(false);

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
