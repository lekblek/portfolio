import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { toApiError } from '../../../../core/api/api-error';
import { ProjectVisibility } from '../../../../core/api/api-types';
import { Button } from '../../../../shared/ui/button';
import { DataTable } from '../../../../shared/ui/data-table';
import { EmptyState } from '../../../../shared/ui/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state';
import { Pagination } from '../../../../shared/ui/pagination';
import { StatusBadge } from '../../../../shared/ui/status-badge';
import { projectPageResource, technologiesResource, VISIBILITY_FILTERS } from '../data/projects';

const VISIBILITIES = new Set<string>(VISIBILITY_FILTERS.map((filter) => filter.value));

/**
 * Projets (`/admin/projects`) : tous les projets, quelle que soit leur visibilité, dans l'ordre du
 * site, 20 par page. Filtres dans l'URL (`?visibility=`, `?technology=`, conservés par la
 * pagination ; les changer ramène à la première page), appliqués par « Filtrer ». Chaque ligne
 * nomme le projet, son statut (pastille), son avancement et sa mise en avant ; un projet publié se
 * voit sur le site. Pas de suppression : l'archivage retire un projet du site (D-CX).
 */
@Component({
  selector: 'app-project-list-page',
  imports: [Button, DataTable, EmptyState, ErrorState, Pagination, RouterLink, StatusBadge],
  template: `
    <div class="flex flex-wrap items-baseline justify-between gap-4">
      <h1 class="text-2xl tracking-heading">Projets</h1>
      <a appButton routerLink="/admin/projects/new">Nouveau projet</a>
    </div>

    <form class="project-filters" aria-label="Filtrer les projets" (submit)="filter($event)">
      <div>
        <label class="block font-semibold" for="filtre-visibilite">Visibilité</label>
        <select
          id="filtre-visibilite"
          class="field-control mt-2"
          [value]="visibilityChoice()"
          (change)="visibilityChoice.set($any($event.target).value)"
        >
          <option value="">Toutes</option>
          @for (filter of visibilities; track filter.value) {
            <option [value]="filter.value" [selected]="filter.value === visibilityChoice()">
              {{ filter.label }}
            </option>
          }
        </select>
      </div>
      <div>
        <label class="block font-semibold" for="filtre-technologie">Technologie</label>
        <select
          id="filtre-technologie"
          class="field-control mt-2"
          [value]="technologyChoice()"
          (change)="technologyChoice.set($any($event.target).value)"
        >
          <option value="">Toutes</option>
          @for (technology of technologies.value() ?? []; track technology.id) {
            <option [value]="technology.slug" [selected]="technology.slug === technologyChoice()">
              {{ technology.name }}
            </option>
          }
        </select>
      </div>
      <div class="flex flex-wrap items-center gap-3">
        <button appButton type="submit" variant="secondary">Filtrer</button>
        @if (filtered()) {
          <a routerLink="/admin/projects">Effacer les filtres</a>
        }
      </div>
    </form>

    @if (projects.error(); as error) {
      <app-error-state
        class="mt-block block"
        [title]="'Les projets n’ont pas pu être chargés.'"
        [detail]="detailOf(error)"
        (retry)="projects.reload()"
      />
    } @else if (projects.hasValue()) {
      @let current = projects.value();
      @if (current.content.length === 0) {
        @if (currentPage() > 1) {
          <app-empty-state class="mt-block block" message="Cette page de la liste est vide.">
            <a routerLink="/admin/projects" [queryParams]="filterParams()">Première page</a>
          </app-empty-state>
        } @else if (filtered()) {
          <app-empty-state
            class="mt-block block"
            message="Aucun projet ne correspond à ces filtres."
          >
            <a routerLink="/admin/projects">Effacer les filtres</a>
          </app-empty-state>
        } @else {
          <app-empty-state
            class="mt-block block"
            message="Aucun projet pour le moment&#8239;: créez le premier."
          />
        }
      } @else {
        <p class="mt-block text-sm text-ink-muted" role="status">{{ countText() }}</p>
        <app-data-table class="mt-3" label="Projets">
          <table class="data-table">
            <caption class="sr-only">
              Projets, dans l’ordre du site
            </caption>
            <thead>
              <tr>
                <th scope="col">Projet</th>
                <th scope="col">Statut</th>
                <th scope="col">Avancement</th>
                <th scope="col">Mise en avant</th>
                <th scope="col" class="data-table-number">Ordre</th>
                <th scope="col" class="data-table-actions"><span class="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              @for (project of current.content; track project.id) {
                <tr>
                  <th scope="row" class="project-name">
                    {{ project.title }}
                    <span class="block text-sm font-normal text-ink-muted" translate="no">{{
                      project.slug
                    }}</span>
                  </th>
                  <td><app-status-badge [status]="project.visibility" /></td>
                  <td class="text-sm whitespace-nowrap">
                    {{ project.stage === 'IN_PROGRESS' ? 'En cours' : 'Terminé' }}
                  </td>
                  <td class="text-sm">
                    @if (project.featured) {
                      Mis en avant
                    } @else {
                      <span class="text-ink-muted">—</span>
                    }
                  </td>
                  <td class="data-table-number">{{ project.displayOrder }}</td>
                  <td class="data-table-actions">
                    <a
                      appButton
                      variant="quiet"
                      size="sm"
                      [routerLink]="['/admin/projects', project.id]"
                      >Modifier<span class="sr-only">
                        le projet {{ quoted(project.title) }}</span
                      ></a
                    >
                    @if (project.visibility === 'PUBLISHED') {
                      <a
                        appButton
                        variant="quiet"
                        size="sm"
                        [routerLink]="['/projects', project.slug]"
                        >Voir<span class="sr-only">
                          le projet {{ quoted(project.title) }} sur le site</span
                        ></a
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
          label="Pages des projets"
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
    .project-filters {
      display: grid;
      gap: calc(var(--spacing) * 4);
      align-items: end;
      margin-block-start: var(--spacing-block);
    }

    @media (width >= 48rem) {
      .project-filters {
        grid-template-columns: repeat(2, minmax(0, calc(var(--spacing) * 64))) auto;
      }
    }

    .project-name {
      min-width: calc(var(--spacing) * 56);
      max-width: calc(var(--spacing) * 96);
    }
  `,
})
export class ProjectListPage {
  /** `?page=` (base 1), `?visibility=`, `?technology=` de l'URL. */
  readonly page = input<string>();
  readonly visibility = input<string>();
  readonly technology = input<string>();

  protected readonly visibilities = VISIBILITY_FILTERS;
  protected readonly currentPage = computed(() => {
    const page = Number(this.page());
    return Number.isInteger(page) && page >= 1 ? page : 1;
  });
  private readonly visibilityFilter = computed(() => {
    const visibility = this.visibility();
    return visibility !== undefined && VISIBILITIES.has(visibility)
      ? (visibility as ProjectVisibility)
      : undefined;
  });
  private readonly technologyFilter = computed(() => this.technology()?.trim() || undefined);
  protected readonly filtered = computed(
    () => this.visibilityFilter() !== undefined || this.technologyFilter() !== undefined,
  );
  protected readonly filterParams = computed(() => ({
    visibility: this.visibilityFilter() ?? null,
    technology: this.technologyFilter() ?? null,
  }));

  protected readonly projects = projectPageResource(() => ({
    page: this.currentPage(),
    visibility: this.visibilityFilter(),
    technology: this.technologyFilter(),
  }));
  protected readonly technologies = technologiesResource();

  /** Choix des listes, appliqués par « Filtrer » ; ils suivent l'URL (retour, lien « Effacer »). */
  protected readonly visibilityChoice = signal('');
  protected readonly technologyChoice = signal('');

  protected readonly countText = computed(() => {
    const total = this.projects.hasValue() ? this.projects.value().totalElements : 0;
    const noun = total === 1 ? 'projet' : 'projets';
    return this.filtered() ? `${total} ${noun} correspondant aux filtres` : `${total} ${noun}`;
  });

  private readonly router = inject(Router);

  constructor() {
    effect(() => {
      this.visibilityChoice.set(this.visibilityFilter() ?? '');
      this.technologyChoice.set(this.technologyFilter() ?? '');
    });
  }

  protected filter(event: Event): void {
    event.preventDefault();
    void this.router.navigate([], {
      queryParams: {
        visibility: this.visibilityChoice() || null,
        technology: this.technologyChoice() || null,
        page: null,
      },
    });
  }

  protected quoted(title: string): string {
    return `«\u00a0${title}\u00a0»`;
  }

  protected detailOf(error: unknown): string | null {
    return toApiError(error).detail;
  }
}
