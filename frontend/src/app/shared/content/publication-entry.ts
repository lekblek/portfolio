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
    <article class="register-entry" [class.register-with-cover]="publication().cover">
      @if (headingLevel() === 2) {
        <h2 class="register-title">
          <a [routerLink]="link()">{{ publication().title }}</a>
        </h2>
      } @else {
        <h3 class="register-title">
          <a [routerLink]="link()">{{ publication().title }}</a>
        </h3>
      }
      <p class="register-meta">
        <time class="font-medium tabular-nums" [attr.datetime]="datetime()">{{ date() }}</time
        ><span class="sr-only">, </span>
        <span class="text-ink-muted">{{ readingTime() }}</span>
      </p>
      <p class="register-summary max-w-prose font-text leading-prose">
        {{ publication().summary }}
      </p>
      @if (publication().category || publication().tags.length > 0) {
        <div class="register-terms">
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
  styles: `
    .register-terms {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      column-gap: calc(var(--spacing) * 4);
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
