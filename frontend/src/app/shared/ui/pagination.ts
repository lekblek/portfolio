import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Élément affiché : un numéro de page, ou une ellipse entre deux numéros non contigus. */
type PaginationItem = { page: number } | { gap: string };

/**
 * Pagination par liens (02-design-system §18) : la page vit dans l'URL (`?page=`, base 1), les
 * autres paramètres (filtres) sont conservés ; la première page n'a pas de paramètre. Première et
 * dernière pages, page courante et ses voisines, ellipses entre elles. La page courante porte
 * `aria-current="page"`. Rien n'est rendu pour une seule page.
 */
@Component({
  selector: 'app-pagination',
  imports: [RouterLink],
  template: `
    @if (totalPages() > 1) {
      <nav [attr.aria-label]="label()" class="pagination">
        <ul class="pagination-list">
          @if (page() > 1) {
            <li>
              <a
                class="pagination-link"
                [routerLink]="[]"
                [queryParams]="{ page: pageParam(page() - 1) }"
                queryParamsHandling="merge"
                >Précédente</a
              >
            </li>
          }
          @for (item of items(); track $index) {
            <li>
              @if ('page' in item) {
                <a
                  class="pagination-link tabular-nums"
                  [class.pagination-current]="item.page === page()"
                  [attr.aria-current]="item.page === page() ? 'page' : null"
                  [routerLink]="[]"
                  [queryParams]="{ page: pageParam(item.page) }"
                  queryParamsHandling="merge"
                  ><span class="sr-only">Page </span>{{ item.page }}</a
                >
              } @else {
                <span class="pagination-gap" aria-hidden="true">…</span>
              }
            </li>
          }
          @if (page() < totalPages()) {
            <li>
              <a
                class="pagination-link"
                [routerLink]="[]"
                [queryParams]="{ page: pageParam(page() + 1) }"
                queryParamsHandling="merge"
                >Suivante</a
              >
            </li>
          }
        </ul>
      </nav>
    }
  `,
  styles: `
    .pagination-list {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      column-gap: calc(var(--spacing) * 2);
    }

    .pagination-link,
    .pagination-gap {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: calc(var(--spacing) * 11);
      min-height: calc(var(--spacing) * 11);
      padding-inline: calc(var(--spacing) * 2);
    }

    .pagination-link {
      font-weight: var(--font-weight-medium);
    }

    .pagination-current {
      color: var(--color-ink);
      text-decoration: none;
      box-shadow: inset 0 -2px 0 var(--color-accent);
    }

    .pagination-gap {
      color: var(--color-ink-muted);
    }
  `,
})
export class Pagination {
  /** Page courante, base 1 (celle de l'URL). */
  readonly page = input.required<number>();
  readonly totalPages = input.required<number>();
  /** Nom de la navigation, s'il y en a plusieurs sur la page. */
  readonly label = input('Pagination');

  protected readonly items = computed<PaginationItem[]>(() => {
    const current = this.page();
    const last = this.totalPages();
    const pages = [...new Set([1, current - 1, current, current + 1, last])]
      .filter((page) => page >= 1 && page <= last)
      .sort((a, b) => a - b);
    return pages.flatMap((page, index) =>
      index > 0 && page - pages[index - 1] > 1 ? [{ gap: `${page}` }, { page }] : [{ page }],
    );
  });

  /** Première page : paramètre retiré, pour une seule adresse par page. */
  protected pageParam(page: number): number | null {
    return page === 1 ? null : page;
  }
}
