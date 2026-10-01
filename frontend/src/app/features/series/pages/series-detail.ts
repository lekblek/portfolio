import { NgOptimizedImage } from '@angular/common';
import { Component, computed, effect, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { toApiError } from '../../../core/api/api-error';
import { injectResponseStatus } from '../../../core/platform/response-status';
import { Seo } from '../../../core/seo/seo';
import { readingTimeLabel } from '../../../shared/content/publication-entry';
import { chapterCountLabel } from '../../../shared/content/series-entry';
import { markdownExcerpt } from '../../../shared/format/excerpt';
import { MarkdownView } from '../../../shared/markdown/markdown-view';
import { ErrorState } from '../../../shared/ui/error-state';
import { seriesResource } from '../data/series.resources';
import { ChapterList } from '../ui/chapter-list';
import { seriesJsonLd } from './series-json-ld';

/**
 * Page d'une série : retour à la liste et fiche (chapitres, lecture totale) dans la marge,
 * titre, description, couverture, puis la table des chapitres dans l'ordre de lecture. Seuls les
 * chapitres visibles sont listés, numérotés par leur rang public. Slug inconnu ou série sans
 * chapitre visible : page introuvable, 404 au rendu serveur.
 */
@Component({
  selector: 'app-series-detail',
  imports: [ChapterList, ErrorState, MarkdownView, NgOptimizedImage, RouterLink],
  host: { class: 'block page-container wrap-break-word' },
  template: `
    @if (series.hasValue()) {
      @let current = series.value();
      <article class="grid gap-x-8 pt-section pb-block lg:grid-cols-12">
        <p class="mb-flow text-sm lg:col-span-3 lg:col-start-1 lg:row-start-1 lg:mb-0 lg:pt-3">
          <a routerLink="/series">Toutes les séries</a>
        </p>
        <header class="min-w-0 lg:col-span-9 lg:col-start-4 lg:row-span-2 lg:row-start-1">
          <h1 class="text-3xl leading-tight tracking-title">{{ current.title }}</h1>
          <app-markdown-view class="mt-flow block" [source]="current.descriptionMarkdown" />
        </header>
        <aside
          class="mt-block min-w-0 lg:col-span-3 lg:col-start-1 lg:row-start-2 lg:mt-flow"
          aria-label="Fiche de la série"
        >
          <dl class="series-sheet">
            <div>
              <dt>Chapitres</dt>
              <dd class="tabular-nums">{{ chapters() }}</dd>
            </div>
            <div>
              <dt>Lecture totale</dt>
              <dd>{{ totalReading() }}</dd>
            </div>
          </dl>
        </aside>
        @if (current.cover; as cover) {
          <figure class="mt-block min-w-0 lg:col-span-9 lg:col-start-4 lg:row-start-3">
            <img
              class="series-cover"
              [ngSrc]="cover.url"
              [width]="cover.width"
              [height]="cover.height"
              [alt]="cover.altText ?? ''"
              priority
            />
          </figure>
        }
        <section
          class="mt-section min-w-0 border-t border-rule pt-block lg:col-span-12 lg:row-start-4"
          aria-labelledby="chapitres"
        >
          <h2 id="chapitres" class="text-2xl tracking-heading lg:sr-only">Chapitres</h2>
          <app-chapter-list class="mt-flow block lg:mt-0" [chapters]="current.chapters" />
        </section>
      </article>
    } @else if (error(); as failure) {
      <div class="grid gap-3 py-section lg:grid-cols-12 lg:gap-8">
        <p class="text-sm font-semibold text-ink-muted lg:col-span-3 lg:pt-3">
          {{ failure.status === 404 ? 'Erreur 404' : 'Séries' }}
        </p>
        <div class="lg:col-span-9">
          @if (failure.status === 404) {
            <h1 class="text-3xl leading-tight tracking-title">Série introuvable</h1>
            <p class="mt-flow max-w-prose font-text text-lg leading-prose">
              Aucune série publiée ne correspond à cette adresse.
            </p>
            <p class="mt-4"><a routerLink="/series">Voir toutes les séries</a></p>
          } @else {
            <h1 class="text-3xl leading-tight tracking-title">Série indisponible</h1>
            <div class="mt-block">
              <app-error-state
                title="La série n’a pas pu être chargée."
                [detail]="failure.detail"
                (retry)="series.reload()"
              />
            </div>
          }
        </div>
      </div>
    } @else {
      <!-- Navigation dans le navigateur seulement : le rendu serveur attend la réponse -->
      @defer (on timer(300ms)) {
        <p role="status" class="py-section text-ink-muted">Chargement de la série…</p>
      }
    }
  `,
  styles: `
    .series-sheet {
      border-top: var(--border-strong) solid var(--color-ink);
      font-size: var(--text-sm);
    }

    .series-sheet > div {
      padding-block: calc(var(--spacing) * 3);
      border-bottom: var(--border-rule) solid var(--color-rule);
    }

    .series-sheet dt {
      color: var(--color-ink-muted);
    }

    .series-sheet dd {
      margin-block-start: calc(var(--spacing) * 1);
      font-weight: var(--font-weight-medium);
    }

    .series-cover {
      width: 100%;
      height: auto;
      border: var(--border-rule) solid var(--color-rule);
      border-radius: var(--radius-media);
      background: var(--color-paper-sunken);
    }
  `,
})
export class SeriesDetail {
  /** Paramètre `:slug` de la route. */
  readonly slug = input.required<string>();

  protected readonly series = seriesResource(() => this.slug());
  protected readonly error = computed(() =>
    this.series.error() ? toApiError(this.series.error()) : null,
  );
  protected readonly chapters = computed(() =>
    this.series.hasValue() ? chapterCountLabel(this.series.value().chapters.length) : '',
  );
  protected readonly totalReading = computed(() =>
    this.series.hasValue()
      ? readingTimeLabel(
          this.series
            .value()
            .chapters.reduce((sum, chapter) => sum + chapter.readingTimeMinutes, 0),
        )
      : '',
  );

  private readonly seo = inject(Seo);
  private readonly setResponseStatus = injectResponseStatus();

  constructor() {
    effect(() => {
      const path = `/series/${this.slug()}`;
      if (this.series.hasValue()) {
        const series = this.series.value();
        this.seo.set({
          title: series.title,
          description: markdownExcerpt(series.descriptionMarkdown, 300) || null,
          path,
          image: series.cover?.url ?? null,
          jsonLd: seriesJsonLd(series, (target) => this.seo.absolute(target)),
        });
        return;
      }
      const failure = this.error();
      if (failure?.status === 404) {
        this.setResponseStatus(404);
        this.seo.set({ title: 'Série introuvable', path, noindex: true });
      } else if (failure) {
        this.setResponseStatus(503);
      }
    });
  }
}
