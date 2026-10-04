import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SeriesChapter } from '../../../core/api/api-types';
import { readingTimeLabel } from '../../../shared/content/content-labels';
import { formatDay, isoDay } from '../../../shared/format/date';

/**
 * Parcours des chapitres d'une série, dans l'ordre de lecture (`<ol>` : la position est une vraie
 * séquence, DS09) : repère numéroté relié au suivant par un filet, titre lié à l'article, date et
 * temps de lecture, résumé. Dès 48 rem de conteneur, le repère occupe une colonne à gauche.
 */
@Component({
  selector: 'app-chapter-list',
  imports: [RouterLink],
  host: { class: '@container block' },
  template: `
    <ol class="chapters">
      @for (chapter of chapters(); track chapter.slug) {
        <li class="chapter">
          <span class="chapter-step tabular-nums" aria-hidden="true">{{ chapter.position }}</span>
          <article class="chapter-body">
            <h3 class="chapter-title">
              <a [routerLink]="['/articles', chapter.slug]"
                ><span class="sr-only">Chapitre {{ chapter.position }}&nbsp;: </span
                >{{ chapter.title }}</a
              >
            </h3>
            <p class="chapter-meta">
              <time class="tabular-nums" [attr.datetime]="iso(chapter.publishedAt)">{{
                day(chapter.publishedAt)
              }}</time
              ><span class="sr-only">, </span>
              <span aria-hidden="true">·</span>
              <span>{{ reading(chapter.readingTimeMinutes) }}</span>
            </p>
            <p class="chapter-summary">{{ chapter.summary }}</p>
          </article>
        </li>
      }
    </ol>
  `,
  styles: `
    .chapter {
      position: relative;
      display: grid;
      grid-template-columns: calc(var(--spacing) * 10) minmax(0, 1fr);
      column-gap: calc(var(--spacing) * 4);
      padding-block-end: var(--spacing-block);
    }

    /* Filet qui relie un repère au suivant */
    .chapter:not(:last-child)::before {
      content: '';
      position: absolute;
      inset-block: calc(var(--spacing) * 10) 0;
      inset-inline-start: calc(var(--spacing) * 5);
      border-inline-start: var(--border-rule) solid var(--color-rule);
    }

    .chapter-step {
      display: grid;
      place-items: center;
      width: calc(var(--spacing) * 10);
      height: calc(var(--spacing) * 10);
      border: var(--border-strong) solid var(--color-ink);
      border-radius: var(--radius-control);
      background: var(--color-paper);
      font-weight: var(--font-weight-semibold);
    }

    .chapter-body {
      display: grid;
      gap: calc(var(--spacing) * 2);
      padding-block-start: calc(var(--spacing) * 1);
    }

    .chapter-title {
      font-size: var(--text-xl);
      line-height: var(--leading-snug);
      letter-spacing: var(--tracking-heading);
      text-wrap: balance;
    }

    .chapter-title a {
      color: var(--color-ink);
      text-decoration-line: none;
    }

    .chapter-title a:hover,
    .chapter-title a:focus-visible {
      color: var(--color-accent-strong);
      text-decoration-line: underline;
    }

    .chapter-meta {
      display: flex;
      flex-wrap: wrap;
      gap: calc(var(--spacing) * 2);
      color: var(--color-ink-muted);
      font-size: var(--text-sm);
    }

    .chapter-summary {
      max-width: var(--container-prose);
      font-family: var(--font-text);
      line-height: var(--leading-prose);
    }

    @container (min-width: 48rem) {
      .chapter {
        grid-template-columns: calc(var(--spacing) * 12) minmax(0, 1fr);
        column-gap: calc(var(--spacing) * 6);
      }

      .chapter-step {
        width: calc(var(--spacing) * 12);
        height: calc(var(--spacing) * 12);
        font-size: var(--text-lg);
      }

      .chapter:not(:last-child)::before {
        inset-block-start: calc(var(--spacing) * 12);
        inset-inline-start: calc(var(--spacing) * 6);
      }
    }
  `,
})
export class ChapterList {
  readonly chapters = input.required<readonly SeriesChapter[]>();

  protected day(instant: string): string {
    return formatDay(instant);
  }

  protected iso(instant: string): string {
    return isoDay(instant);
  }

  protected reading(minutes: number): string {
    return readingTimeLabel(minutes);
  }
}
