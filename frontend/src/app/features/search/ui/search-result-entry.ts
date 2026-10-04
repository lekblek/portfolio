import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SearchResult, SearchResultType } from '../../../core/api/api-types';
import { publicationPath } from '../../../shared/content/content-labels';
import { formatDay, isoDay } from '../../../shared/format/date';

const TYPE_LABELS: Record<SearchResultType, string> = {
  ARTICLE: 'Article',
  NEWS: 'Actualité',
  PROJECT: 'Projet',
};

/** Page publique d'un résultat, selon son type. */
export function searchResultPath(result: Pick<SearchResult, 'type' | 'slug'>): string {
  return result.type === 'PROJECT'
    ? `/projects/${result.slug}`
    : publicationPath(result.type, result.slug);
}

/**
 * Ligne de résultat, sur la grille du registre : nature (article, actualité, projet : mot et
 * repère de forme, DS09) et date d'une publication, titre lié à sa page, résumé.
 */
@Component({
  selector: 'app-search-result-entry',
  imports: [RouterLink],
  template: `
    <article class="register-entry">
      <h2 class="register-title">
        <a [routerLink]="link()">{{ result().title }}</a>
      </h2>
      <p class="register-meta">
        <span class="type-label font-medium"
          ><span class="type-mark" [attr.data-type]="result().type" aria-hidden="true"></span
          >{{ typeLabel() }}</span
        >
        @if (result().publishedAt; as instant) {
          <span class="sr-only">, </span>
          <time class="text-ink-muted tabular-nums" [attr.datetime]="day(instant)">{{
            date(instant)
          }}</time>
        }
      </p>
      <p class="register-summary max-w-prose font-text leading-prose">{{ result().summary }}</p>
    </article>
  `,
  styles: `
    .type-label {
      display: inline-flex;
      align-items: center;
      gap: calc(var(--spacing) * 2);
    }
  `,
})
export class SearchResultEntry {
  readonly result = input.required<SearchResult>();

  protected readonly link = computed(() => searchResultPath(this.result()));
  protected readonly typeLabel = computed(() => TYPE_LABELS[this.result().type]);

  protected date(instant: string): string {
    return formatDay(instant);
  }

  protected day(instant: string): string {
    return isoDay(instant);
  }
}
