import { Component, computed, effect, inject, input, linkedSignal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { toApiError } from '../../../core/api/api-error';
import { SearchResult } from '../../../core/api/api-types';
import { Page } from '../../../core/api/page';
import { injectResponseStatus } from '../../../core/platform/response-status';
import { Seo } from '../../../core/seo/seo';
import { Button } from '../../../shared/ui/button';
import { EmptyState } from '../../../shared/ui/empty-state';
import { ErrorState } from '../../../shared/ui/error-state';
import { Pagination } from '../../../shared/ui/pagination';
import { searchResource } from '../data/search.resource';
import { SearchResultEntry } from '../ui/search-result-entry';

/** Longueur maximale d'une recherche, celle de l'API (D-CF). */
export const MAX_QUERY_LENGTH = 200;

const DESCRIPTION =
  'Rechercher dans les articles, les actualités et les projets de Blek Ngossanga.';

/**
 * Recherche publique : formulaire `GET` natif (`/search?q=`), qui fonctionne sans JavaScript et
 * dont le serveur rend les résultats ; dans le navigateur, la soumission devient une navigation
 * interne. Résultats mixtes vers la page de leur type, pagination (`?page=` en base 1), nombre
 * de résultats annoncé (`role="status"`). Aucune requête sans texte ; plus de 200 caractères :
 * refusé avant l'API (400). Pages jamais indexées.
 */
@Component({
  selector: 'app-search-page',
  imports: [Button, EmptyState, ErrorState, Pagination, RouterLink, SearchResultEntry],
  host: { class: 'block page-container wrap-break-word' },
  template: `
    <div class="grid gap-3 pt-section pb-block lg:grid-cols-12 lg:gap-8">
      <div class="min-w-0 lg:col-span-9 lg:col-start-4">
        <h1 class="text-3xl leading-tight tracking-title">Recherche</h1>
        <form
          role="search"
          method="get"
          action="/search"
          class="mt-block max-w-prose"
          (submit)="submit($event)"
        >
          <label for="search-q" class="block font-semibold">
            Rechercher dans les articles, les actualités et les projets
          </label>
          <div class="mt-2 flex flex-wrap gap-3">
            <input
              id="search-q"
              class="field-control search-field"
              name="q"
              type="search"
              autocomplete="off"
              enterkeyhint="search"
              [attr.maxlength]="maxLength"
              [value]="query()"
              [attr.aria-invalid]="tooLong() ? 'true' : null"
              [attr.aria-describedby]="tooLong() ? 'search-q-error search-q-hint' : 'search-q-hint'"
            />
            <button appButton type="submit">Rechercher</button>
          </div>
          @if (tooLong()) {
            <p id="search-q-error" class="mt-2 font-medium text-danger">
              La recherche est limitée à {{ maxLength }} caractères.
            </p>
          }
          <p id="search-q-hint" class="mt-2 text-sm text-ink-muted">
            Des guillemets pour une expression exacte, «&nbsp;or&nbsp;» entre deux mots pour l’un ou
            l’autre, un tiret devant un mot pour l’exclure.
          </p>
        </form>
        <p role="status" class="mt-block min-h-6 font-medium">{{ status() }}</p>
      </div>
    </div>

    @if (!text() && !tooLong()) {
      <section class="search-browse" aria-labelledby="parcourir">
        <h2 id="parcourir" class="text-lg tracking-heading">Parcourir</h2>
        <ul class="search-browse-list">
          @for (entry of browse; track entry.path) {
            <li>
              <a class="search-browse-link" [routerLink]="entry.path"
                ><span class="type-mark" [attr.data-type]="entry.type" aria-hidden="true"></span
                >{{ entry.label }}</a
              >
              <p class="mt-2 text-sm text-ink-muted">{{ entry.text }}</p>
            </li>
          }
        </ul>
      </section>
    }

    @if (error(); as failure) {
      <div class="lg:grid lg:grid-cols-12 lg:gap-8">
        <div class="lg:col-span-9 lg:col-start-4">
          <app-error-state
            title="La recherche n’a pas pu aboutir."
            [detail]="failure.detail"
            (retry)="results.reload()"
          />
        </div>
      </div>
    } @else if (shown(); as current) {
      @if (current.content.length > 0) {
        <ol class="@container divide-y divide-rule border-t border-rule" [attr.start]="firstRank()">
          @for (result of current.content; track result.type + result.slug) {
            <li class="py-block">
              <app-search-result-entry [result]="result" />
            </li>
          }
        </ol>
        <div class="border-t border-rule pt-block lg:grid lg:grid-cols-12 lg:gap-8">
          <app-pagination
            class="block lg:col-span-9 lg:col-start-4"
            label="Pages de résultats"
            [page]="currentPage()"
            [totalPages]="current.totalPages"
          />
        </div>
      } @else {
        <div class="lg:grid lg:grid-cols-12 lg:gap-8">
          <div class="lg:col-span-9 lg:col-start-4">
            @if (current.totalElements > 0) {
              <app-empty-state message="Cette page de résultats n’existe pas.">
                <a [routerLink]="[]" [queryParams]="{ page: null }" queryParamsHandling="merge"
                  >Revenir à la première page</a
                >
              </app-empty-state>
            } @else {
              <app-empty-state
                message="Aucun article, aucune actualité ni aucun projet ne correspond à cette recherche."
              >
                <a routerLink="/articles">Voir les articles</a>
                <a routerLink="/projects">Voir les projets</a>
              </app-empty-state>
            }
          </div>
        </div>
      }
    }
  `,
  styles: `
    /* Apparence commune des contrôles (styles/controls.css) ; ici, la place à côté du bouton */
    .search-field {
      flex: 1 1 16rem;
      width: auto;
      min-width: 0;
    }

    .search-browse {
      display: grid;
      gap: calc(var(--spacing) * 4);
      padding-block: var(--spacing-block) var(--spacing-section);
      border-top: var(--border-rule) solid var(--color-rule);
    }

    .search-browse-list {
      display: grid;
      gap: var(--spacing-flow) calc(var(--spacing) * 8);
    }

    .search-browse-list > li {
      padding-block-start: calc(var(--spacing) * 3);
      border-top: var(--border-strong) solid var(--color-ink);
    }

    .search-browse-link {
      display: inline-flex;
      align-items: center;
      gap: calc(var(--spacing) * 2);
      min-height: calc(var(--spacing) * 11);
      font-size: var(--text-xl);
      font-weight: var(--font-weight-semibold);
      letter-spacing: var(--tracking-heading);
    }

    @media (min-width: 48rem) {
      .search-browse-list {
        grid-template-columns: repeat(3, minmax(0, 1fr));
      }
    }

    @media (min-width: 64rem) {
      .search-browse {
        grid-template-columns: repeat(12, minmax(0, 1fr));
        column-gap: calc(var(--spacing) * 8);
      }

      .search-browse > h2 {
        grid-column: 1 / span 3;
      }

      .search-browse-list {
        grid-column: 4 / span 9;
      }
    }
  `,
})
export class SearchPage {
  /** `?q=` de l'URL. */
  readonly q = input<string>();
  /** `?page=` de l'URL (base 1). */
  readonly page = input<string>();

  protected readonly maxLength = MAX_QUERY_LENGTH;

  /** État initial : les listes cherchées, avec leur repère de type. */
  protected readonly browse = [
    {
      type: 'PROJECT',
      path: '/projects',
      label: 'Projets',
      text: 'Réalisations, de l’objectif aux résultats, avec leurs captures.',
    },
    {
      type: 'ARTICLE',
      path: '/articles',
      label: 'Articles',
      text: 'Textes techniques longs : architecture, vision par ordinateur, exploitation.',
    },
    {
      type: 'NEWS',
      path: '/news',
      label: 'Actualités',
      text: 'Nouvelles courtes : lancements, versions, interventions.',
    },
  ] as const;

  protected readonly query = computed(() => this.q() ?? '');
  protected readonly tooLong = computed(() => this.query().length > MAX_QUERY_LENGTH);
  protected readonly text = computed(() => (this.tooLong() ? '' : this.query().trim()));

  protected readonly currentPage = computed(() => {
    const page = Number(this.page());
    return Number.isInteger(page) && page >= 1 ? page : 1;
  });

  protected readonly results = searchResource(() =>
    this.text() ? { q: this.text(), page: this.currentPage() } : null,
  );

  /** Dernière page reçue, gardée pendant le chargement de la suivante ; rien sans texte. */
  protected readonly shown = linkedSignal<
    Page<SearchResult> | undefined,
    Page<SearchResult> | undefined
  >({
    source: () => (this.results.hasValue() ? this.results.value() : undefined),
    computation: (next, previous) => (this.text() ? (next ?? previous?.value) : undefined),
  });

  protected readonly error = computed(() =>
    this.results.error() ? toApiError(this.results.error()) : null,
  );

  /** Rang du premier résultat de la page, pour la numérotation de la liste. */
  protected readonly firstRank = computed(() => {
    const current = this.shown();
    return current ? current.page * current.size + 1 : 1;
  });

  protected readonly status = computed(() => {
    if (!this.text()) {
      return '';
    }
    if (this.results.isLoading()) {
      return 'Recherche en cours…';
    }
    const current = this.results.hasValue() ? this.results.value() : undefined;
    return current
      ? `${this.countLabel(current.totalElements)} pour «\u00a0${this.text()}\u00a0»`
      : '';
  });

  private countLabel(total: number): string {
    if (total === 0) {
      return 'Aucun résultat';
    }
    return `${total} ${total === 1 ? 'résultat' : 'résultats'}`;
  }

  private readonly router = inject(Router);

  /** Soumission dans le navigateur : navigation interne vers la même adresse que le formulaire. */
  protected submit(event: SubmitEvent): void {
    event.preventDefault();
    const q = String(new FormData(event.target as HTMLFormElement).get('q') ?? '').trim();
    void this.router.navigate(['/search'], { queryParams: { q: q || null } });
  }

  private readonly seo = inject(Seo);
  private readonly setResponseStatus = injectResponseStatus();

  constructor() {
    effect(() => {
      if (this.tooLong()) {
        this.setResponseStatus(400);
      } else if (this.error()) {
        this.setResponseStatus(503);
      } else {
        const current = this.results.hasValue() ? this.results.value() : undefined;
        if (current && current.content.length === 0 && current.totalElements > 0) {
          this.setResponseStatus(404);
        }
      }
      const text = this.text();
      this.seo.set({
        title: text ? `Recherche\u00a0: ${text}` : 'Recherche',
        description: DESCRIPTION,
        path: '/search',
        noindex: true,
      });
    });
  }
}
