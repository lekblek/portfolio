import { NgOptimizedImage } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PublicationSummary } from '../../core/api/api-types';
import { dayAndShortMonth, formatDay, isoDay } from '../format/date';
import { publicationPath } from './content-labels';

/**
 * Dépêche (actualité, DS09) : format court, distinct de la carte d'article. Date en colonne
 * typographique (jour, mois), titre lié, résumé, vignette 3:2 facultative à droite ; ni catégorie,
 * ni tags (les filtres de la liste les gardent), ni temps de lecture (toujours court). Le titre est le seul lien ; sa cible couvre la
 * dépêche. Sur un conteneur étroit, la date passe au-dessus du titre.
 */
@Component({
  selector: 'app-news-item',
  imports: [NgOptimizedImage, RouterLink],
  host: { class: '@container block' },
  template: `
    <article class="news-item" [class.news-with-cover]="item().cover">
      @if (headingLevel() === 2) {
        <h2 class="news-title">
          <a class="card-link" [routerLink]="link()">{{ item().title }}</a>
        </h2>
      } @else {
        <h3 class="news-title">
          <a class="card-link" [routerLink]="link()">{{ item().title }}</a>
        </h3>
      }
      <p class="news-date">
        <time [attr.datetime]="datetime()">
          <span class="sr-only">{{ fullDate() }}</span>
          <span class="news-day tabular-nums" aria-hidden="true">{{ parts().day }}</span>
          <span class="news-month" aria-hidden="true">{{ parts().month }}</span>
        </time>
      </p>
      <p class="news-summary">{{ item().summary }}</p>
      @if (item().cover; as cover) {
        <div class="news-cover plate">
          <img [ngSrc]="cover.url" fill sizes="10rem" [alt]="cover.altText ?? ''" />
        </div>
      }
    </article>
  `,
  styles: `
    .news-item {
      position: relative;
      display: grid;
      grid-template-columns: minmax(0, 1fr);
      grid-template-areas: 'date' 'title' 'summary';
      row-gap: calc(var(--spacing) * 2);
    }

    .news-with-cover {
      grid-template-columns: minmax(0, 1fr) calc(var(--spacing) * 24);
      grid-template-areas: 'date cover' 'title cover' 'summary summary';
      column-gap: calc(var(--spacing) * 4);
    }

    .news-title {
      grid-area: title;
      font-size: var(--text-xl);
      line-height: var(--leading-snug);
      letter-spacing: var(--tracking-heading);
      text-wrap: balance;
    }

    .news-date {
      grid-area: date;
    }

    .news-date time {
      display: inline-flex;
      align-items: baseline;
      gap: calc(var(--spacing) * 1);
      font-size: var(--text-sm);
      font-weight: var(--font-weight-medium);
    }

    .news-summary {
      grid-area: summary;
      max-width: var(--container-prose);
      font-family: var(--font-text);
      line-height: var(--leading-prose);
    }

    .news-cover {
      grid-area: cover;
      align-self: start;
    }

    @container (min-width: 40rem) {
      .news-item,
      .news-with-cover {
        grid-template-columns: calc(var(--spacing) * 20) minmax(0, 1fr);
        grid-template-areas: 'date title' 'date summary';
        align-content: start;
        column-gap: calc(var(--spacing) * 6);
      }

      .news-with-cover {
        grid-template-columns: calc(var(--spacing) * 20) minmax(0, 1fr) calc(var(--spacing) * 40);
        grid-template-areas: 'date title cover' 'date summary cover';
      }

      .news-date time {
        flex-direction: column;
        gap: 0;
        line-height: var(--leading-tight);
      }

      .news-day {
        font-size: var(--text-3xl);
        font-weight: var(--font-weight-semibold);
        letter-spacing: var(--tracking-title);
      }

      .news-month {
        margin-block-start: calc(var(--spacing) * 1);
        color: var(--color-ink-muted);
      }
    }

    @media (hover: hover) and (pointer: fine) {
      .news-item:has(.card-link:hover) .plate {
        border-color: var(--color-accent);
      }
    }
  `,
})
export class NewsItem {
  readonly item = input.required<PublicationSummary>();
  /** `<h2>` dans la liste des actualités, `<h3>` sous une section (accueil, groupe de mois). */
  readonly headingLevel = input<2 | 3>(3);

  protected readonly link = computed(() => publicationPath(this.item().type, this.item().slug));
  protected readonly parts = computed(() => dayAndShortMonth(this.item().publishedAt));
  protected readonly fullDate = computed(() => formatDay(this.item().publishedAt));
  protected readonly datetime = computed(() => isoDay(this.item().publishedAt));
}
