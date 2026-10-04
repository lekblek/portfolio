import { NgOptimizedImage } from '@angular/common';
import { Component, computed, effect, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { toApiError } from '../../../core/api/api-error';
import { injectResponseStatus } from '../../../core/platform/response-status';
import { Seo } from '../../../core/seo/seo';
import { projectStageLabel } from '../../../shared/content/content-labels';
import { formatPeriod } from '../../../shared/format/date';
import { MarkdownView } from '../../../shared/markdown/markdown-view';
import { Button } from '../../../shared/ui/button';
import { ErrorState } from '../../../shared/ui/error-state';
import { TermLinks } from '../../../shared/ui/term-link';
import { projectResource } from '../data/projects.resources';
import { projectJsonLd } from './project-json-ld';

/**
 * Détail d'un projet publié, en étude de cas (DS09) : dans la marge gauche, le retour à la liste
 * (en tête sur petit écran) et le cartouche des métadonnées (état, période, technologies), qui
 * accompagne la lecture sur grand écran ;
 * dans la colonne principale, titre, résumé, liens vers le code et la démonstration s'ils
 * existent, couverture, description, puis les captures en figures numérotées, chacune entière
 * dans un cadre 3:2 commun (des rapports différents ne laissent aucun trou dans la galerie). Slug inconnu ou projet non public : page
 * introuvable, 404 au rendu serveur.
 */
@Component({
  selector: 'app-project-detail',
  imports: [Button, ErrorState, MarkdownView, NgOptimizedImage, RouterLink, TermLinks],
  host: { class: 'block page-container wrap-break-word' },
  template: `
    @if (project.hasValue()) {
      @let current = project.value();
      <article class="grid gap-x-8 pt-section pb-block lg:grid-cols-12">
        <p class="mb-flow text-sm lg:col-span-3 lg:col-start-1 lg:row-start-1 lg:mb-0 lg:pt-3">
          <a routerLink="/projects">Tous les projets</a>
        </p>
        <header class="min-w-0 lg:col-span-9 lg:col-start-4 lg:row-span-2 lg:row-start-1">
          <h1 class="text-3xl leading-tight tracking-title">{{ current.title }}</h1>
          <p class="mt-flow max-w-prose font-text text-xl leading-snug">
            {{ current.shortDescription }}
          </p>
          @if (current.repositoryUrl || current.demoUrl) {
            <p class="mt-block flex flex-wrap gap-3">
              @if (current.repositoryUrl; as repository) {
                <a appButton variant="secondary" class="external" [href]="repository"
                  >Voir le code source<span class="sr-only"> (site externe)</span></a
                >
              }
              @if (current.demoUrl; as demo) {
                <a appButton variant="secondary" class="external" [href]="demo"
                  >Voir la démonstration<span class="sr-only"> (site externe)</span></a
                >
              }
            </p>
          }
        </header>

        <aside
          class="project-aside mt-block min-w-0 lg:col-span-3 lg:col-start-1 lg:row-span-3 lg:row-start-2 lg:mt-flow"
          aria-label="Fiche du projet"
        >
          <dl class="project-sheet">
            <div>
              <dt>État</dt>
              <dd>{{ stage() }}</dd>
            </div>
            <div>
              <dt>Période</dt>
              <dd class="tabular-nums">{{ period() }}</dd>
            </div>
            @if (current.technologies.length > 0) {
              <div>
                <dt>Technologies</dt>
                <dd>
                  <app-term-links
                    [terms]="current.technologies"
                    [label]="'Technologies de ' + current.title"
                    path="/projects"
                    param="technology"
                  />
                </dd>
              </div>
            }
          </dl>
        </aside>

        @if (current.cover; as cover) {
          <figure class="mt-block min-w-0 lg:col-span-9 lg:col-start-4 lg:row-start-3">
            <!-- Sous le titre et le résumé : au-dessus de la ligne de flottaison, prioritaire -->
            <div class="plate plate-wide">
              <img
                [ngSrc]="cover.url"
                fill
                sizes="(min-width: 80rem) 62rem, (min-width: 64rem) 75vw, 100vw"
                [alt]="cover.altText ?? ''"
                priority
              />
            </div>
          </figure>
        }

        <div class="mt-block min-w-0 lg:col-span-9 lg:col-start-4 lg:row-start-4">
          <app-markdown-view [source]="current.descriptionMarkdown" />

          @if (current.screenshots.length > 0) {
            <section class="mt-section border-t border-rule pt-block" aria-labelledby="captures">
              <h2 id="captures" class="text-2xl tracking-heading">Captures</h2>
              <ol class="gallery mt-flow">
                @for (screenshot of current.screenshots; track $index) {
                  <li [class.gallery-lead]="$first && current.screenshots.length !== 2">
                    <figure>
                      <!-- Cadre 3:2 commun : la capture y est entière (jamais recadrée) -->
                      <div class="gallery-frame">
                        <img
                          [ngSrc]="screenshot.image.url"
                          fill
                          [alt]="screenshot.image.altText ?? ''"
                        />
                      </div>
                      <figcaption class="mt-2 text-sm text-ink-muted">
                        <span class="font-semibold text-ink">Figure {{ $index + 1 }}</span>
                        @if (screenshot.caption) {
                          <span>&nbsp;— {{ screenshot.caption }}</span>
                        }
                      </figcaption>
                    </figure>
                  </li>
                }
              </ol>
            </section>
          }
        </div>
      </article>
    } @else if (error(); as failure) {
      <div class="grid gap-3 py-section lg:grid-cols-12 lg:gap-8">
        <p class="text-sm font-semibold text-ink-muted lg:col-span-3 lg:pt-3">
          {{ failure.status === 404 ? 'Erreur 404' : 'Projets' }}
        </p>
        <div class="lg:col-span-9">
          @if (failure.status === 404) {
            <h1 class="text-3xl leading-tight tracking-title">Projet introuvable</h1>
            <p class="mt-flow max-w-prose font-text text-lg leading-prose">
              Aucun projet publié ne correspond à cette adresse.
            </p>
            <p class="mt-4"><a routerLink="/projects">Voir tous les projets</a></p>
          } @else {
            <h1 class="text-3xl leading-tight tracking-title">Projet indisponible</h1>
            <div class="mt-block">
              <app-error-state
                title="Le projet n’a pas pu être chargé."
                [detail]="failure.detail"
                (retry)="project.reload()"
              />
            </div>
          }
        </div>
      </div>
    } @else {
      <!-- Navigation dans le navigateur seulement : le rendu serveur attend la réponse -->
      @defer (on timer(300ms)) {
        <p role="status" class="py-section text-ink-muted">Chargement du projet…</p>
      }
    }
  `,
  styles: `
    .project-sheet {
      margin-block-start: var(--spacing-flow);
      border-top: var(--border-strong) solid var(--color-ink);
      font-size: var(--text-sm);
    }

    .project-sheet > div {
      padding-block: calc(var(--spacing) * 3);
      border-bottom: var(--border-rule) solid var(--color-rule);
    }

    .project-sheet dt {
      color: var(--color-ink-muted);
    }

    .project-sheet dd {
      margin-block-start: calc(var(--spacing) * 1);
      font-weight: var(--font-weight-medium);
    }

    /* La fiche accompagne la lecture de l'étude de cas sur grand écran */
    @media (min-width: 64rem) {
      .project-aside {
        position: sticky;
        top: var(--spacing-block);
        align-self: start;
      }
    }

    .gallery-frame {
      position: relative;
      aspect-ratio: var(--aspect-cover);
      overflow: hidden;
      border: var(--border-rule) solid var(--color-rule);
      border-radius: var(--radius-media);
      background: var(--color-paper-sunken);
    }

    .gallery-frame img {
      object-fit: contain;
    }

    .gallery {
      display: grid;
      gap: var(--spacing-block) calc(var(--spacing) * 8);
    }

    @media (min-width: 48rem) {
      .gallery {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }

      /* Première capture en grand quand la galerie en compte une, trois ou plus */
      .gallery-lead {
        grid-column: 1 / -1;
      }
    }
  `,
})
export class ProjectDetail {
  /** Paramètre `:slug` de la route. */
  readonly slug = input.required<string>();

  protected readonly project = projectResource(() => this.slug());
  protected readonly error = computed(() =>
    this.project.error() ? toApiError(this.project.error()) : null,
  );
  protected readonly stage = computed(() =>
    this.project.hasValue() ? projectStageLabel(this.project.value().stage) : '',
  );
  protected readonly period = computed(() =>
    this.project.hasValue()
      ? formatPeriod(this.project.value().startDate, this.project.value().endDate)
      : '',
  );

  private readonly seo = inject(Seo);
  private readonly setResponseStatus = injectResponseStatus();

  constructor() {
    effect(() => {
      const path = `/projects/${this.slug()}`;
      if (this.project.hasValue()) {
        const project = this.project.value();
        this.seo.set({
          title: project.title,
          description: project.shortDescription,
          path,
          type: 'article',
          image: project.cover?.url ?? null,
          jsonLd: projectJsonLd(project, (target) => this.seo.absolute(target)),
        });
        return;
      }
      const failure = this.error();
      if (failure?.status === 404) {
        this.setResponseStatus(404);
        this.seo.set({ title: 'Projet introuvable', path, noindex: true });
      } else if (failure) {
        this.setResponseStatus(503);
      }
    });
  }
}
