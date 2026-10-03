import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { toApiError } from '../../../../core/api/api-error';
import { ContactStatus } from '../../../../core/api/api-types';
import { formatDayTime } from '../../../../shared/format/date';
import { Button } from '../../../../shared/ui/button';
import { DataTable } from '../../../../shared/ui/data-table';
import { EmptyState } from '../../../../shared/ui/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state';
import { Pagination } from '../../../../shared/ui/pagination';
import { StatusBadge } from '../../../../shared/ui/status-badge';
import { messagePageResource, STATUS_FILTERS } from '../data/messages';

const STATUSES = new Set<string>(STATUS_FILTERS.map((filter) => filter.value));

/**
 * Messages de contact (`/admin/messages`, D-CZ) : boîte de réception, les plus récents d'abord,
 * 20 par page, filtrée par statut dans l'URL (`?status=`). Le texte d'un message se lit sur sa
 * page ; la liste montre l'expéditeur, le sujet, la date et le statut.
 */
@Component({
  selector: 'app-message-list-page',
  imports: [Button, DataTable, EmptyState, ErrorState, Pagination, RouterLink, StatusBadge],
  template: `
    <h1 class="text-2xl tracking-heading">Messages</h1>

    <form class="message-filters" aria-label="Filtrer les messages" (submit)="filter($event)">
      <div>
        <label class="block font-semibold" for="filtre-statut-message">Statut</label>
        <select
          id="filtre-statut-message"
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
        @if (statusFilter()) {
          <a routerLink="/admin/messages">Effacer le filtre</a>
        }
      </div>
    </form>

    @if (messages.error(); as error) {
      <app-error-state
        class="mt-block block"
        [title]="'Les messages n’ont pas pu être chargés.'"
        [detail]="detailOf(error)"
        (retry)="messages.reload()"
      />
    } @else if (messages.hasValue()) {
      @let current = messages.value();
      @if (current.content.length === 0) {
        @if (currentPage() > 1) {
          <app-empty-state class="mt-block block" message="Cette page de la boîte est vide.">
            <a routerLink="/admin/messages" [queryParams]="{ status: statusFilter() ?? null }"
              >Première page</a
            >
          </app-empty-state>
        } @else if (statusFilter()) {
          <app-empty-state class="mt-block block" message="Aucun message avec ce statut.">
            <a routerLink="/admin/messages">Voir tous les messages</a>
          </app-empty-state>
        } @else {
          <app-empty-state
            class="mt-block block"
            message="Aucun message pour le moment&#8239;: ils arrivent du formulaire de contact du site."
          />
        }
      } @else {
        <p class="mt-block text-sm text-ink-muted" role="status">{{ countText() }}</p>
        <app-data-table class="mt-3" label="Messages">
          <table class="data-table">
            <caption class="sr-only">
              Messages de contact, les plus récents d’abord
            </caption>
            <thead>
              <tr>
                <th scope="col">Sujet</th>
                <th scope="col">Expéditeur</th>
                <th scope="col">Reçu le</th>
                <th scope="col">Statut</th>
              </tr>
            </thead>
            <tbody>
              @for (item of current.content; track item.id) {
                <tr [class.message-new]="item.status === 'NEW'">
                  <th scope="row" class="message-subject">
                    <a [routerLink]="['/admin/messages', item.id]">{{ item.subject }}</a>
                  </th>
                  <td class="text-sm">
                    {{ item.name }}
                    <span class="block text-ink-muted wrap-break-word">{{ item.email }}</span>
                  </td>
                  <td class="text-sm whitespace-nowrap">{{ dayTime(item.createdAt) }}</td>
                  <td><app-status-badge [status]="item.status" /></td>
                </tr>
              }
            </tbody>
          </table>
        </app-data-table>
        <app-pagination
          class="mt-block block"
          label="Pages des messages"
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
    .message-filters {
      display: grid;
      gap: calc(var(--spacing) * 4);
      align-items: end;
      margin-block-start: var(--spacing-block);
    }

    @media (width >= 48rem) {
      .message-filters {
        grid-template-columns: minmax(0, calc(var(--spacing) * 64)) auto;
      }
    }

    .message-subject {
      min-width: calc(var(--spacing) * 56);
      max-width: calc(var(--spacing) * 96);
    }

    .message-new .message-subject {
      font-weight: var(--font-weight-semibold);
    }
  `,
})
export class MessageListPage {
  readonly page = input<string>();
  readonly status = input<string>();

  protected readonly statuses = STATUS_FILTERS;
  protected readonly dayTime = formatDayTime;
  protected readonly currentPage = computed(() => {
    const page = Number(this.page());
    return Number.isInteger(page) && page >= 1 ? page : 1;
  });
  protected readonly statusFilter = computed(() => {
    const status = this.status();
    return status !== undefined && STATUSES.has(status) ? (status as ContactStatus) : undefined;
  });
  protected readonly messages = messagePageResource(() => ({
    page: this.currentPage(),
    status: this.statusFilter(),
  }));
  protected readonly statusChoice = signal('');
  protected readonly countText = computed(() => {
    const total = this.messages.hasValue() ? this.messages.value().totalElements : 0;
    return `${total} ${total === 1 ? 'message' : 'messages'}`;
  });

  private readonly router = inject(Router);

  constructor() {
    effect(() => this.statusChoice.set(this.statusFilter() ?? ''));
  }

  protected filter(event: Event): void {
    event.preventDefault();
    void this.router.navigate([], {
      queryParams: { status: this.statusChoice() || null, page: null },
    });
  }

  protected detailOf(error: unknown): string | null {
    return toApiError(error).detail;
  }
}
