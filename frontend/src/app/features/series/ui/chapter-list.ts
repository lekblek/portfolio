import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SeriesChapter } from '../../../core/api/api-types';
import { readingTimeLabel } from '../../../shared/content/publication-entry';
import { formatDay, isoDay } from '../../../shared/format/date';

/**
 * Table des chapitres d'une série, dans l'ordre de lecture (`<ol>` : la position est une vraie
 * séquence) : numéro, date et temps de lecture dans la colonne de repères, titre lié à l'article
 * et résumé. Même grille que les registres (`styles/register.css`).
 */
@Component({
  selector: 'app-chapter-list',
  imports: [RouterLink],
  template: `
    <ol class="@container divide-y divide-rule">
      @for (chapter of chapters(); track chapter.slug) {
        <li class="py-block first:pt-0">
          <article class="register-entry">
            <h3 class="register-title">
              <a [routerLink]="['/articles', chapter.slug]">{{ chapter.title }}</a>
            </h3>
            <p class="register-meta">
              <span class="font-semibold"
                >Chapitre {{ chapter.position }}<span class="sr-only">, </span></span
              >
              <time
                class="text-ink-muted tabular-nums"
                [attr.datetime]="iso(chapter.publishedAt)"
                >{{ day(chapter.publishedAt) }}</time
              ><span class="sr-only">, </span>
              <span class="text-ink-muted">{{ reading(chapter.readingTimeMinutes) }}</span>
            </p>
            <p class="register-summary max-w-prose font-text leading-prose">
              {{ chapter.summary }}
            </p>
          </article>
        </li>
      }
    </ol>
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
