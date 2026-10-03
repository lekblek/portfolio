import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { toApiError } from '../../../../core/api/api-error';
import { Button } from '../../../../shared/ui/button';
import { DataTable } from '../../../../shared/ui/data-table';
import { EmptyState } from '../../../../shared/ui/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state';
import { Pagination } from '../../../../shared/ui/pagination';
import { seriesPageResource } from '../data/series';

/**
 * Séries (`/admin/series`) : toutes les séries, par titre, 20 par page, avec leur nombre de
 * chapitres. Pas de suppression (D-CV) : une série sans article visible n'apparaît pas sur le site.
 */
@Component({
  selector: 'app-series-list-page',
  imports: [Button, DataTable, EmptyState, ErrorState, Pagination, RouterLink],
  template: `
    <div class="flex flex-wrap items-baseline justify-between gap-4">
      <h1 class="text-2xl tracking-heading">Séries</h1>
      <a appButton routerLink="/admin/series/new">Nouvelle série</a>
    </div>

    @if (series.error(); as error) {
      <app-error-state
        class="mt-block block"
        [title]="'Les séries n’ont pas pu être chargées.'"
        [detail]="detailOf(error)"
        (retry)="series.reload()"
      />
    } @else if (series.hasValue()) {
      @let current = series.value();
      @if (current.content.length === 0) {
        @if (currentPage() > 1) {
          <app-empty-state class="mt-block block" message="Cette page de la liste est vide.">
            <a routerLink="/admin/series">Première page</a>
          </app-empty-state>
        } @else {
          <app-empty-state
            class="mt-block block"
            message="Aucune série pour le moment&#8239;: regroupez des articles qui se lisent dans l’ordre."
          />
        }
      } @else {
        <p class="mt-block text-sm text-ink-muted" role="status">{{ countText() }}</p>
        <app-data-table class="mt-3" label="Séries">
          <table class="data-table">
            <caption class="sr-only">
              Séries, par titre
            </caption>
            <thead>
              <tr>
                <th scope="col">Série</th>
                <th scope="col" class="data-table-number">Chapitres</th>
                <th scope="col" class="data-table-actions"><span class="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              @for (item of current.content; track item.id) {
                <tr>
                  <th scope="row">
                    {{ item.title }}
                    <span class="block text-sm font-normal text-ink-muted" translate="no">{{
                      item.slug
                    }}</span>
                  </th>
                  <td class="data-table-number">{{ item.chapterCount }}</td>
                  <td class="data-table-actions">
                    <a appButton variant="quiet" size="sm" [routerLink]="['/admin/series', item.id]"
                      >Modifier<span class="sr-only">
                        la série «&#160;{{ item.title }}&#160;»</span
                      ></a
                    >
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </app-data-table>
        <app-pagination
          class="mt-block block"
          label="Pages des séries"
          [page]="currentPage()"
          [totalPages]="current.totalPages"
        />
      }
    } @else {
      @defer (on timer(300ms)) {
        <p role="status" class="mt-block text-ink-muted">Chargement…</p>
      }
    }
  `,
})
export class SeriesListPage {
  /** `?page=` de l'URL (base 1). */
  readonly page = input<string>();

  protected readonly currentPage = computed(() => {
    const page = Number(this.page());
    return Number.isInteger(page) && page >= 1 ? page : 1;
  });
  protected readonly series = seriesPageResource(() => ({ page: this.currentPage() }));
  protected readonly countText = computed(() => {
    const total = this.series.hasValue() ? this.series.value().totalElements : 0;
    return `${total} ${total === 1 ? 'série' : 'séries'}`;
  });

  protected detailOf(error: unknown): string | null {
    return toApiError(error).detail;
  }
}
