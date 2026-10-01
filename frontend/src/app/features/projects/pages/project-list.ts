import { Component, computed, effect, inject, input, linkedSignal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { toApiError } from '../../../core/api/api-error';
import { ProjectSummary } from '../../../core/api/api-types';
import { Page } from '../../../core/api/page';
import { injectResponseStatus } from '../../../core/platform/response-status';
import { Seo } from '../../../core/seo/seo';
import { ProjectEntry } from '../../../shared/content/project-entry';
import { EmptyState } from '../../../shared/ui/empty-state';
import { ErrorState } from '../../../shared/ui/error-state';
import { Pagination } from '../../../shared/ui/pagination';
import { projectListResource } from '../data/projects.resources';

/** Lignes de la liste au-dessus de la ligne de flottaison aux largeurs courantes. */
const PRIORITY_ROWS = 3;

const PATH = '/projects';
const DESCRIPTION =
  'Projets de Blek Ngossanga\u202f: présentation, période, état, technologies utilisées et captures.';

/**
 * Liste des projets publiés : registre paginé, filtré par technologie. L'état vit dans l'URL
 * (`?page=` en base 1, `?technology=` : slug), lu en entrées par la liaison des paramètres du
 * routeur ; une page invalide vaut la première (comme l'API). Pendant un changement de page ou
 * de filtre, la page précédente reste affichée jusqu'à la réponse.
 */
@Component({
  selector: 'app-project-list',
  imports: [EmptyState, ErrorState, Pagination, ProjectEntry, RouterLink],
  host: { class: 'block page-container wrap-break-word' },
  template: `
    <div class="grid gap-3 pt-section pb-block lg:grid-cols-12 lg:gap-8">
      <!-- Repère : le nombre de projets, seulement quand il est connu (sans répéter le titre) -->
      @if (shown(); as current) {
        <p class="text-sm font-semibold text-ink-muted tabular-nums lg:col-span-3 lg:pt-3">
          {{ countLabel(current.totalElements) }}
        </p>
      }
      <div class="min-w-0 lg:col-span-9 lg:col-start-4">
        <h1 class="text-3xl leading-tight tracking-title">Projets</h1>
        @if (filter(); as slug) {
          <p class="mt-flow flex flex-wrap items-baseline gap-x-4 gap-y-1">
            <span
              >Technologie&nbsp;: <strong translate="no">{{ filterName() ?? slug }}</strong></span
            >
            <a [routerLink]="[]" [queryParams]="{ technology: null, page: null }">
              Retirer le filtre
            </a>
          </p>
        }
        @if (projects.isLoading() && shown()) {
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
            title="La liste des projets n’a pas pu être chargée."
            [detail]="failure.detail"
            (retry)="projects.reload()"
          />
        </div>
      </div>
    } @else if (shown(); as current) {
      @if (current.content.length > 0) {
        <ul class="@container divide-y divide-rule border-t border-rule">
          @for (project of current.content; track project.slug; let index = $index) {
            <li class="py-block">
              <app-project-entry
                [project]="project"
                [currentTechnology]="filter()"
                [coverPriority]="index === priorityCover()"
              />
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
                <a [routerLink]="[]" [queryParams]="{ page: null }" queryParamsHandling="merge">
                  Revenir à la première page
                </a>
              </app-empty-state>
            } @else if (filter()) {
              <app-empty-state message="Aucun projet publié n’utilise cette technologie.">
                <a [routerLink]="[]" [queryParams]="{ technology: null, page: null }">
                  Voir tous les projets
                </a>
              </app-empty-state>
            } @else {
              <app-empty-state message="Aucun projet n’est encore publié.">
                <a routerLink="/about">Voir la page À propos</a>
              </app-empty-state>
            }
          </div>
        </div>
      }
    } @else {
      <!-- Premier affichage dans le navigateur seulement : le rendu serveur attend la réponse -->
      @defer (on timer(300ms)) {
        <p role="status" class="border-t border-rule pt-block text-ink-muted">
          Chargement des projets…
        </p>
      }
    }
  `,
})
export class ProjectList {
  /** `?page=` de l'URL (base 1). */
  readonly page = input<string>();
  /** `?technology=` de l'URL : slug d'une technologie. */
  readonly technology = input<string>();

  protected readonly currentPage = computed(() => {
    const page = Number(this.page());
    return Number.isInteger(page) && page >= 1 ? page : 1;
  });
  protected readonly filter = computed(() => this.technology()?.trim() || null);

  protected readonly projects = projectListResource(() => ({
    page: this.currentPage(),
    technology: this.filter(),
  }));

  /** Dernière page reçue, gardée pendant le chargement de la suivante. */
  protected readonly shown = linkedSignal<
    Page<ProjectSummary> | undefined,
    Page<ProjectSummary> | undefined
  >({
    source: () => (this.projects.hasValue() ? this.projects.value() : undefined),
    computation: (next, previous) => next ?? previous?.value,
  });

  /**
   * Seule couverture chargée en priorité : la première de la page, si elle figure dans les trois
   * premières lignes (au-dessus de la ligne de flottaison) ; -1 sinon.
   */
  protected readonly priorityCover = computed(() => {
    const index = this.shown()?.content.findIndex((entry) => entry.cover !== null) ?? -1;
    return index < PRIORITY_ROWS ? index : -1;
  });

  protected readonly error = computed(() =>
    this.projects.error() ? toApiError(this.projects.error()) : null,
  );

  /** Nom de la technologie filtrée, lu dans les projets affichés ; le slug à défaut. */
  protected readonly filterName = computed(() => {
    const slug = this.filter();
    return (
      this.shown()
        ?.content.flatMap((project) => project.technologies)
        .find((technology) => technology.slug === slug)?.name ?? null
    );
  });

  private readonly seo = inject(Seo);
  private readonly setResponseStatus = injectResponseStatus();

  constructor() {
    effect(() => {
      const current = this.projects.hasValue() ? this.projects.value() : undefined;
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
      const slug = this.filter();
      const technology = slug ? (this.filterName() ?? slug) : null;
      this.seo.set({
        title: ['Projets', technology && `technologie ${technology}`, page > 1 && `page ${page}`]
          .filter(Boolean)
          .join(', '),
        description: DESCRIPTION,
        path: listPath(slug, page),
        // Vues filtrées et pages hors de la liste : pas d'indexation (contenu dupliqué ou absent)
        noindex: slug !== null || outOfRange,
      });
    });
  }

  protected countLabel(total: number): string {
    // Zéro et un au singulier, en français
    return `${total} ${total <= 1 ? 'projet' : 'projets'}`;
  }

  protected outOfRangeMessage(totalPages: number): string {
    return `Cette page n’existe pas\u202f: la liste compte ${totalPages} ${totalPages === 1 ? 'page' : 'pages'}.`;
  }
}

/** Adresse canonique de la vue : la page 1 n'a pas de paramètre. */
function listPath(technology: string | null, page: number): string {
  const params = new URLSearchParams();
  if (technology) {
    params.set('technology', technology);
  }
  if (page > 1) {
    params.set('page', String(page));
  }
  const query = params.toString();
  return query ? `${PATH}?${query}` : PATH;
}
