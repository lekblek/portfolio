import { Component, computed, effect, inject, input, linkedSignal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { toApiError } from '../../../core/api/api-error';
import { SeriesSummary } from '../../../core/api/api-types';
import { Page } from '../../../core/api/page';
import { injectResponseStatus } from '../../../core/platform/response-status';
import { Seo } from '../../../core/seo/seo';
import { SeriesCard } from '../../../shared/content/series-card';
import { EmptyState } from '../../../shared/ui/empty-state';
import { ErrorState } from '../../../shared/ui/error-state';
import { Pagination } from '../../../shared/ui/pagination';
import { seriesListResource } from '../data/series.resources';

const DESCRIPTION =
  'Séries d’articles de Blek Ngossanga\u202f: sujets suivis en plusieurs chapitres, dans l’ordre de lecture.';

/**
 * Liste des séries publiques : registre paginé (`?page=` en base 1 dans l'URL, page invalide →
 * première), page précédente gardée pendant le chargement de la suivante.
 */
@Component({
  selector: 'app-series-list',
  imports: [EmptyState, ErrorState, Pagination, RouterLink, SeriesCard],
  host: { class: 'block page-container wrap-break-word' },
  template: `
    <div class="grid gap-3 pt-section pb-block lg:grid-cols-12 lg:gap-8">
      @if (shown(); as current) {
        <p class="text-sm font-semibold text-ink-muted tabular-nums lg:col-span-3 lg:pt-3">
          {{ current.totalElements }} {{ current.totalElements <= 1 ? 'série' : 'séries' }}
        </p>
      }
      <div class="min-w-0 lg:col-span-9 lg:col-start-4">
        <h1 class="text-3xl leading-tight tracking-title">Séries</h1>
        @if (series.isLoading() && shown()) {
          @defer (on timer(300ms)) {
            <p role="status" class="mt-2 text-sm text-ink-muted">Chargement…</p>
          }
        }
      </div>
    </div>

    @if (error(); as failure) {
      <div class="lg:grid lg:grid-cols-12 lg:gap-8">
        <div class="lg:col-span-9 lg:col-start-4">
          <app-error-state
            title="La liste des séries n’a pas pu être chargée."
            [detail]="failure.detail"
            (retry)="series.reload()"
          />
        </div>
      </div>
    } @else if (shown(); as current) {
      @if (current.content.length > 0) {
        <ul class="divide-y divide-rule border-t border-rule">
          @for (entry of current.content; track entry.slug; let index = $index) {
            <li class="py-block">
              <app-series-card [series]="entry" [coverPriority]="index === priorityCover()" />
            </li>
          }
        </ul>
        <div class="border-t border-rule pt-block lg:grid lg:grid-cols-12 lg:gap-8">
          <app-pagination
            class="block lg:col-span-9 lg:col-start-4"
            [page]="currentPage()"
            [totalPages]="current.totalPages"
          />
        </div>
      } @else {
        <div class="lg:grid lg:grid-cols-12 lg:gap-8">
          <div class="lg:col-span-9 lg:col-start-4">
            @if (current.totalElements > 0) {
              <app-empty-state [message]="outOfRangeMessage(current.totalPages)">
                <a [routerLink]="[]" [queryParams]="{ page: null }">Revenir à la première page</a>
              </app-empty-state>
            } @else {
              <app-empty-state message="Aucune série n’est encore publiée.">
                <a routerLink="/articles">Voir les articles</a>
              </app-empty-state>
            }
          </div>
        </div>
      }
    } @else {
      <!-- Premier affichage dans le navigateur seulement : le rendu serveur attend la réponse -->
      @defer (on timer(300ms)) {
        <p role="status" class="border-t border-rule pt-block text-ink-muted">Chargement…</p>
      }
    }
  `,
})
export class SeriesList {
  /** `?page=` de l'URL (base 1). */
  readonly page = input<string>();

  protected readonly currentPage = computed(() => {
    const page = Number(this.page());
    return Number.isInteger(page) && page >= 1 ? page : 1;
  });

  protected readonly series = seriesListResource(() => ({ page: this.currentPage() }));

  /** Dernière page reçue, gardée pendant le chargement de la suivante. */
  protected readonly shown = linkedSignal<
    Page<SeriesSummary> | undefined,
    Page<SeriesSummary> | undefined
  >({
    source: () => (this.series.hasValue() ? this.series.value() : undefined),
    computation: (next, previous) => next ?? previous?.value,
  });

  /**
   * Seule couverture chargée en priorité : la première de la page, quelle que soit sa ligne. Les
   * lignes sans image sont courtes : avec le jeu de démonstration, la première couverture est l'image
   * LCP même en quatrième ligne (D-EV) ; une image préchargée sous la ligne de flottaison sur mobile
   * coûte moins qu'une image LCP chargée tard. -1 : aucune couverture.
   */
  protected readonly priorityCover = computed(
    () => this.shown()?.content.findIndex((entry) => entry.cover !== null) ?? -1,
  );

  protected readonly error = computed(() =>
    this.series.error() ? toApiError(this.series.error()) : null,
  );

  protected outOfRangeMessage(totalPages: number): string {
    return `Cette page n’existe pas\u202f: la liste compte ${totalPages} ${totalPages === 1 ? 'page' : 'pages'}.`;
  }

  private readonly seo = inject(Seo);
  private readonly setResponseStatus = injectResponseStatus();

  constructor() {
    effect(() => {
      if (this.error()) {
        this.setResponseStatus(503);
        return;
      }
      const current = this.series.hasValue() ? this.series.value() : undefined;
      if (current === undefined) {
        return;
      }
      const outOfRange = current.content.length === 0 && current.totalElements > 0;
      if (outOfRange) {
        this.setResponseStatus(404);
      }
      const page = this.currentPage();
      this.seo.set({
        title: page > 1 ? `Séries, page ${page}` : 'Séries',
        description: DESCRIPTION,
        path: page > 1 ? `/series?page=${page}` : '/series',
        noindex: outOfRange,
      });
    });
  }
}
