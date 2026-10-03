import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { toApiError } from '../../../../core/api/api-error';
import {
  AdminPublicationSummary,
  PublicationStatus,
  PublicationType,
} from '../../../../core/api/api-types';
import { formatDay } from '../../../../shared/format/date';
import { Button } from '../../../../shared/ui/button';
import { DataTable } from '../../../../shared/ui/data-table';
import { EmptyState } from '../../../../shared/ui/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state';
import { Pagination } from '../../../../shared/ui/pagination';
import { StatusBadge } from '../../../../shared/ui/status-badge';
import {
  publicationPageResource,
  publicPath,
  STATUS_FILTERS,
  TYPE_FILTERS,
  TYPE_LABELS,
} from '../data/publications';

const TYPES = new Set<string>(TYPE_FILTERS.map((filter) => filter.value));
const STATUSES = new Set<string>(STATUS_FILTERS.map((filter) => filter.value));

/**
 * Publications (`/admin/publications`) : articles et actualités de tout statut, les dernières
 * modifiées d'abord, 20 par page. Filtres dans l'URL (`?type=`, `?status=` : statut observable),
 * appliqués par « Filtrer » et conservés par la pagination. Création par type (« Nouvel article »,
 * « Nouvelle actualité ») : le type ne change plus ensuite (D-CU).
 */
@Component({
  selector: 'app-publication-list-page',
  imports: [Button, DataTable, EmptyState, ErrorState, Pagination, RouterLink, StatusBadge],
  template: `
    <div class="flex flex-wrap items-baseline justify-between gap-4">
      <h1 class="text-2xl tracking-heading">Publications</h1>
      <div class="flex flex-wrap gap-3">
        <a appButton routerLink="/admin/publications/new/article">Nouvel article</a>
        <a appButton variant="secondary" routerLink="/admin/publications/new/news"
          >Nouvelle actualité</a
        >
      </div>
    </div>

    <form class="admin-filters" aria-label="Filtrer les publications" (submit)="filter($event)">
      <div>
        <label class="block font-semibold" for="filtre-type">Type</label>
        <select
          id="filtre-type"
          class="field-control mt-2"
          [value]="typeChoice()"
          (change)="typeChoice.set($any($event.target).value)"
        >
          <option value="">Tous</option>
          @for (filter of types; track filter.value) {
            <option [value]="filter.value" [selected]="filter.value === typeChoice()">
              {{ filter.label }}
            </option>
          }
        </select>
      </div>
      <div>
        <label class="block font-semibold" for="filtre-statut">Statut</label>
        <select
          id="filtre-statut"
          class="field-control mt-2"
          [value]="statusChoice()"
          (change)="statusChoice.set($any($event.target).value)"
        >
          <option value="">Tous</option>
          @for (filter of statuses; track filter.value) {
            <option [value]="filter.value" [selected]="filter.value === statusChoice()">
              {{ filter.label }}
            </option>
          }
        </select>
      </div>
      <div class="flex flex-wrap items-center gap-3">
        <button appButton type="submit" variant="secondary">Filtrer</button>
        @if (filtered()) {
          <a routerLink="/admin/publications">Effacer les filtres</a>
        }
      </div>
    </form>

    @if (publications.error(); as error) {
      <app-error-state
        class="mt-block block"
        [title]="'Les publications n’ont pas pu être chargées.'"
        [detail]="detailOf(error)"
        (retry)="publications.reload()"
      />
    } @else if (publications.hasValue()) {
      @let current = publications.value();
      @if (current.content.length === 0) {
        @if (currentPage() > 1) {
          <app-empty-state class="mt-block block" message="Cette page de la liste est vide.">
            <a routerLink="/admin/publications" [queryParams]="filterParams()">Première page</a>
          </app-empty-state>
        } @else if (filtered()) {
          <app-empty-state
            class="mt-block block"
            message="Aucune publication ne correspond à ces filtres."
          >
            <a routerLink="/admin/publications">Effacer les filtres</a>
          </app-empty-state>
        } @else {
          <app-empty-state
            class="mt-block block"
            message="Aucune publication pour le moment&#8239;: rédigez un premier article."
          />
        }
      } @else {
        <p class="mt-block text-sm text-ink-muted" role="status">{{ countText() }}</p>
        <app-data-table class="mt-3" label="Publications">
          <table class="data-table">
            <caption class="sr-only">
              Publications, les dernières modifiées d’abord
            </caption>
            <thead>
              <tr>
                <th scope="col">Publication</th>
                <th scope="col">Type</th>
                <th scope="col">Statut</th>
                <th scope="col">Date de publication</th>
                <th scope="col">Modifiée le</th>
                <th scope="col" class="data-table-actions"><span class="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              @for (item of current.content; track item.id) {
                <tr>
                  <th scope="row" class="admin-name">
                    {{ item.title }}
                    <span class="block text-sm font-normal text-ink-muted" translate="no">{{
                      item.slug
                    }}</span>
                  </th>
                  <td class="text-sm whitespace-nowrap">{{ typeLabels[item.type] }}</td>
                  <td><app-status-badge [status]="item.status" [feminine]="true" /></td>
                  <td class="text-sm whitespace-nowrap">{{ dateOf(item) }}</td>
                  <td class="text-sm whitespace-nowrap">{{ day(item.updatedAt) }}</td>
                  <td class="data-table-actions">
                    <a
                      appButton
                      variant="quiet"
                      size="sm"
                      [routerLink]="['/admin/publications', item.id]"
                      >Modifier<span class="sr-only"> {{ theItem(item) }}</span></a
                    >
                    @if (item.status === 'PUBLISHED') {
                      <a appButton variant="quiet" size="sm" [routerLink]="sitePath(item)"
                        >Voir<span class="sr-only"> {{ theItem(item) }} sur le site</span></a
                      >
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </app-data-table>
        <app-pagination
          class="mt-block block"
          label="Pages des publications"
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
  styles: `
    .admin-filters {
      display: grid;
      gap: calc(var(--spacing) * 4);
      align-items: end;
      margin-block-start: var(--spacing-block);
    }

    @media (width >= 48rem) {
      .admin-filters {
        grid-template-columns: repeat(2, minmax(0, calc(var(--spacing) * 64))) auto;
      }
    }

    .admin-name {
      min-width: calc(var(--spacing) * 56);
      max-width: calc(var(--spacing) * 96);
    }
  `,
})
export class PublicationListPage {
  /** `?page=` (base 1), `?type=`, `?status=` de l'URL. */
  readonly page = input<string>();
  readonly type = input<string>();
  readonly status = input<string>();

  protected readonly types = TYPE_FILTERS;
  protected readonly statuses = STATUS_FILTERS;
  protected readonly typeLabels = TYPE_LABELS;
  protected readonly day = formatDay;

  protected readonly currentPage = computed(() => {
    const page = Number(this.page());
    return Number.isInteger(page) && page >= 1 ? page : 1;
  });
  private readonly typeFilter = computed(() => {
    const type = this.type();
    return type !== undefined && TYPES.has(type) ? (type as PublicationType) : undefined;
  });
  private readonly statusFilter = computed(() => {
    const status = this.status();
    return status !== undefined && STATUSES.has(status) ? (status as PublicationStatus) : undefined;
  });
  protected readonly filtered = computed(
    () => this.typeFilter() !== undefined || this.statusFilter() !== undefined,
  );
  protected readonly filterParams = computed(() => ({
    type: this.typeFilter() ?? null,
    status: this.statusFilter() ?? null,
  }));

  protected readonly publications = publicationPageResource(() => ({
    page: this.currentPage(),
    type: this.typeFilter(),
    status: this.statusFilter(),
  }));

  protected readonly typeChoice = signal('');
  protected readonly statusChoice = signal('');

  protected readonly countText = computed(() => {
    const total = this.publications.hasValue() ? this.publications.value().totalElements : 0;
    const noun = total === 1 ? 'publication' : 'publications';
    return this.filtered() ? `${total} ${noun} correspondant aux filtres` : `${total} ${noun}`;
  });

  private readonly router = inject(Router);

  constructor() {
    effect(() => {
      this.typeChoice.set(this.typeFilter() ?? '');
      this.statusChoice.set(this.statusFilter() ?? '');
    });
  }

  protected filter(event: Event): void {
    event.preventDefault();
    void this.router.navigate([], {
      queryParams: {
        type: this.typeChoice() || null,
        status: this.statusChoice() || null,
        page: null,
      },
    });
  }

  /** Date utile selon le statut : publication, programmation, ou rien pour un brouillon. */
  protected dateOf(item: AdminPublicationSummary): string {
    if (item.publishedAt === null || item.status === 'DRAFT' || item.status === 'IN_REVIEW') {
      return '—';
    }
    return item.status === 'SCHEDULED'
      ? `prévue le ${formatDay(item.publishedAt)}`
      : formatDay(item.publishedAt);
  }

  protected sitePath(item: AdminPublicationSummary): string {
    return publicPath(item.type, item.slug);
  }

  protected theItem(item: AdminPublicationSummary): string {
    return `${item.type === 'ARTICLE' ? 'l’article' : 'l’actualité'} «\u00a0${item.title}\u00a0»`;
  }

  protected detailOf(error: unknown): string | null {
    return toApiError(error).detail;
  }
}
