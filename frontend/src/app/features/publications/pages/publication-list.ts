import { Component, computed, effect, inject, input, linkedSignal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { toApiError } from '../../../core/api/api-error';
import { PublicationSummary, PublicationType } from '../../../core/api/api-types';
import { Page } from '../../../core/api/page';
import { injectResponseStatus } from '../../../core/platform/response-status';
import { Seo } from '../../../core/seo/seo';
import { NewsItem } from '../../../shared/content/news-item';
import { PublicationCard } from '../../../shared/content/publication-card';
import { publicationListPath } from '../../../shared/content/content-labels';
import { formatPublicationMonth } from '../../../shared/format/date';
import { EmptyState } from '../../../shared/ui/empty-state';
import { ErrorState } from '../../../shared/ui/error-state';
import { Pagination } from '../../../shared/ui/pagination';
import { publicationListResource } from '../data/publications.resources';
import { PUBLICATION_LABELS } from './publication-labels';

/**
 * Liste des articles ou des actualités (type donné par la route), paginée, filtrée par catégorie
 * et par tag (DS09) : articles en grille de cartes, le premier en tête quand il ouvre la liste
 * (page 1, sans filtre) et qu'il a une couverture ; actualités en dépêches groupées par mois. L'état vit dans l'URL (`?page=` en base 1, `?category=`, `?tag=` : slugs),
 * lu en entrées ; une page invalide vaut la première. Chaque filtre actif est affiché et
 * retirable seul. Pendant un changement, la page précédente reste affichée.
 */
@Component({
  selector: 'app-publication-list',
  imports: [EmptyState, ErrorState, NewsItem, Pagination, PublicationCard, RouterLink],
  host: { class: 'block page-container wrap-break-word' },
  styles: `
    .news-month {
      display: grid;
      gap: calc(var(--spacing) * 4);
      padding-block: var(--spacing-block);
    }

    .news-month + .news-month {
      border-top: var(--border-rule) solid var(--color-rule);
    }

    .news-month-title {
      font-size: var(--text-lg);
      letter-spacing: var(--tracking-heading);
    }

    .news-month-title::first-letter {
      text-transform: uppercase;
    }

    @media (min-width: 64rem) {
      .news-month {
        grid-template-columns: repeat(12, minmax(0, 1fr));
        column-gap: calc(var(--spacing) * 8);
      }

      .news-month-title {
        grid-column: 1 / span 3;
      }

      .news-month > ul {
        grid-column: 4 / span 9;
      }
    }

    .card-grid-lead {
      grid-column: 1 / -1;
      padding-block-end: var(--spacing-block);
      border-bottom: var(--border-rule) solid var(--color-rule);
    }
  `,
  template: `
    <div class="grid gap-3 pt-section pb-block lg:grid-cols-12 lg:gap-8">
      @if (shown(); as current) {
        <p class="text-sm font-semibold text-ink-muted tabular-nums lg:col-span-3 lg:pt-3">
          {{ countLabel(current.totalElements) }}
        </p>
      }
      <div class="min-w-0 lg:col-span-9 lg:col-start-4">
        <h1 class="text-3xl leading-tight tracking-title">{{ labels().list }}</h1>
        @if (categoryFilter() || tagFilter()) {
          <ul class="mt-flow flex flex-col gap-1" aria-label="Filtres actifs">
            @if (categoryFilter(); as slug) {
              <li class="flex flex-wrap items-baseline gap-x-4">
                <span
                  >Catégorie&nbsp;: <strong>{{ categoryName() ?? slug }}</strong></span
                >
                <a
                  [routerLink]="[]"
                  [queryParams]="{ category: null, page: null }"
                  queryParamsHandling="merge"
                  >Retirer ce filtre<span class="sr-only"> de catégorie</span></a
                >
              </li>
            }
            @if (tagFilter(); as slug) {
              <li class="flex flex-wrap items-baseline gap-x-4">
                <span
                  >Tag&nbsp;: <strong translate="no">{{ tagName() ?? slug }}</strong></span
                >
                <a
                  [routerLink]="[]"
                  [queryParams]="{ tag: null, page: null }"
                  queryParamsHandling="merge"
                  >Retirer ce filtre<span class="sr-only"> de tag</span></a
                >
              </li>
            }
          </ul>
        }
        @if (publications.isLoading() && shown()) {
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
            [title]="labels().listError"
            [detail]="failure.detail"
            (retry)="publications.reload()"
          />
        </div>
      </div>
    } @else if (shown(); as current) {
      @if (current.content.length > 0) {
        @if (type() === 'NEWS') {
          <div class="border-t border-rule pb-section">
            @for (group of months(); track group.month) {
              <section class="news-month" [attr.aria-labelledby]="'mois-' + $index">
                <h2 [id]="'mois-' + $index" class="news-month-title">{{ group.month }}</h2>
                <ul class="divide-y divide-rule">
                  @for (item of group.items; track item.slug) {
                    <li class="py-block first:pt-0">
                      <app-news-item [item]="item" />
                    </li>
                  }
                </ul>
              </section>
            }
          </div>
        } @else {
          <ul class="card-grid card-grid-3 border-t border-rule pt-block pb-section">
            @for (publication of current.content; track publication.slug; let index = $index) {
              <li [class.card-grid-lead]="index === 0 && hasLead()">
                <app-publication-card
                  [publication]="publication"
                  [lead]="index === 0 && hasLead()"
                  [currentCategory]="categoryFilter()"
                  [currentTag]="tagFilter()"
                  [coverPriority]="index === priorityCover()"
                />
              </li>
            }
          </ul>
        }
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
                <a [routerLink]="[]" [queryParams]="{ page: null }" queryParamsHandling="merge">
                  Revenir à la première page
                </a>
              </app-empty-state>
            } @else if (categoryFilter() || tagFilter()) {
              <app-empty-state [message]="labels().noMatch">
                <a [routerLink]="[]" [queryParams]="{}">Retirer les filtres</a>
              </app-empty-state>
            } @else {
              <app-empty-state [message]="labels().none">
                <a routerLink="/projects">Voir les projets</a>
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
export class PublicationList {
  /** Type des publications listées (donnée de la route). */
  readonly type = input.required<PublicationType>();
  /** `?page=` de l'URL (base 1). */
  readonly page = input<string>();
  /** `?category=` et `?tag=` de l'URL : slugs. */
  readonly category = input<string>();
  readonly tag = input<string>();

  protected readonly labels = computed(() => PUBLICATION_LABELS[this.type()]);
  protected readonly currentPage = computed(() => {
    const page = Number(this.page());
    return Number.isInteger(page) && page >= 1 ? page : 1;
  });
  protected readonly categoryFilter = computed(() => this.category()?.trim() || null);
  protected readonly tagFilter = computed(() => this.tag()?.trim() || null);

  protected readonly publications = publicationListResource(() => ({
    type: this.type(),
    page: this.currentPage(),
    category: this.categoryFilter(),
    tag: this.tagFilter(),
  }));

  /** Dernière page reçue, gardée pendant le chargement de la suivante. */
  protected readonly shown = linkedSignal<
    Page<PublicationSummary> | undefined,
    Page<PublicationSummary> | undefined
  >({
    source: () => (this.publications.hasValue() ? this.publications.value() : undefined),
    computation: (next, previous) => next ?? previous?.value,
  });

  /** Article de tête : le premier de la liste non filtrée, page 1, s'il a une couverture. */
  protected readonly hasLead = computed(
    () =>
      this.type() === 'ARTICLE' &&
      this.currentPage() === 1 &&
      !this.categoryFilter() &&
      !this.tagFilter() &&
      !!this.shown()?.content[0]?.cover,
  );

  /**
   * Seule couverture chargée en priorité (articles) : la première de la page si elle est dans les
   * deux premières rangées de la grille (haut de page dès 1280 px, où les planches vides de la
   * première rangée laissent l'image LCP en deuxième rangée) ; plus bas, aucune. -1 : aucune.
   */
  protected readonly priorityCover = computed(() => {
    if (this.type() === 'NEWS') {
      return -1;
    }
    const index = this.shown()?.content.findIndex((entry) => entry.cover !== null) ?? -1;
    return index < 6 ? index : -1;
  });

  /** Actualités groupées par mois de publication, dans l'ordre de la liste. */
  protected readonly months = computed(() => {
    const groups: { month: string; items: PublicationSummary[] }[] = [];
    for (const item of this.shown()?.content ?? []) {
      const month = formatPublicationMonth(item.publishedAt);
      const last = groups.at(-1);
      if (last?.month === month) {
        last.items.push(item);
      } else {
        groups.push({ month, items: [item] });
      }
    }
    return groups;
  });

  protected readonly error = computed(() =>
    this.publications.error() ? toApiError(this.publications.error()) : null,
  );

  /** Noms des filtres, lus dans les publications affichées ; les slugs à défaut. */
  protected readonly categoryName = computed(
    () =>
      this.shown()?.content.find(
        (publication) => publication.category?.slug === this.categoryFilter(),
      )?.category?.name ?? null,
  );
  protected readonly tagName = computed(
    () =>
      this.shown()
        ?.content.flatMap((publication) => publication.tags)
        .find((tag) => tag.slug === this.tagFilter())?.name ?? null,
  );

  private readonly seo = inject(Seo);
  private readonly setResponseStatus = injectResponseStatus();

  constructor() {
    effect(() => {
      const current = this.publications.hasValue() ? this.publications.value() : undefined;
      if (this.error()) {
        this.setResponseStatus(503);
        return;
      }
      if (current === undefined) {
        return;
      }
      const outOfRange = current.content.length === 0 && current.totalElements > 0;
      if (outOfRange) {
        this.setResponseStatus(404);
      }
      const page = this.currentPage();
      const category = this.categoryFilter();
      const tag = this.tagFilter();
      this.seo.set({
        title: [
          this.labels().list,
          category && `catégorie ${this.categoryName() ?? category}`,
          tag && `tag ${this.tagName() ?? tag}`,
          page > 1 && `page ${page}`,
        ]
          .filter(Boolean)
          .join(', '),
        description: this.labels().description,
        path: listPath(publicationListPath(this.type()), { category, tag, page }),
        // Vues filtrées et pages hors de la liste : pas d'indexation
        noindex: category !== null || tag !== null || outOfRange,
      });
    });
  }

  protected countLabel(total: number): string {
    // Zéro et un au singulier, en français
    return `${total} ${total <= 1 ? this.labels().one : this.labels().many}`;
  }

  protected outOfRangeMessage(totalPages: number): string {
    return `Cette page n’existe pas\u202f: la liste compte ${totalPages} ${totalPages === 1 ? 'page' : 'pages'}.`;
  }
}

/** Adresse canonique de la vue : la page 1 n'a pas de paramètre. */
function listPath(
  base: string,
  { category, tag, page }: { category: string | null; tag: string | null; page: number },
): string {
  const params = new URLSearchParams();
  if (category) {
    params.set('category', category);
  }
  if (tag) {
    params.set('tag', tag);
  }
  if (page > 1) {
    params.set('page', String(page));
  }
  const query = params.toString();
  return query ? `${base}?${query}` : base;
}
