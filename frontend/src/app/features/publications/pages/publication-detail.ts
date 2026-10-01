import { NgOptimizedImage, NgTemplateOutlet } from '@angular/common';
import { Component, computed, effect, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { toApiError } from '../../../core/api/api-error';
import { PublicationType } from '../../../core/api/api-types';
import { injectPermanentRedirect } from '../../../core/platform/permanent-redirect';
import { injectResponseStatus } from '../../../core/platform/response-status';
import { Seo } from '../../../core/seo/seo';
import {
  publicationListPath,
  publicationPath,
  readingTimeLabel,
} from '../../../shared/content/publication-entry';
import { formatDay, isoDay } from '../../../shared/format/date';
import { CodeCopy } from '../../../shared/markdown/code-copy';
import { MarkdownMath } from '../../../shared/markdown/markdown-math';
import { renderMarkdown } from '../../../shared/markdown/markdown-renderer';
import { MarkdownView } from '../../../shared/markdown/markdown-view';
import { TableOfContents } from '../../../shared/markdown/table-of-contents';
import { ErrorState } from '../../../shared/ui/error-state';
import { TermLinks } from '../../../shared/ui/term-link';
import { publicationResource } from '../data/publications.resources';
import { seriesNavigationResource } from '../data/series-navigation.resource';
import { SeriesNavigation } from '../ui/series-navigation';
import { PUBLICATION_LABELS } from './publication-labels';
import { publicationJsonLd } from './publication-json-ld';

/**
 * Page d'une publication, mise en page comme une planche (02-design-system §4.4) : dès 80 rem,
 * sommaire collant dans la marge gauche, colonne de lecture, fiche (date, lecture, catégorie,
 * tags) dans la marge droite puis les notes ; en dessous, une colonne : fiche compacte, sommaire
 * repliable, contenu, notes après leur paragraphe. Fiche et sommaire existent sous leurs deux
 * formes, une seule affichée à chaque largeur. Une publication ouverte sous le mauvais type
 * (`/articles/…` pour une actualité) est redirigée (301) vers son adresse canonique.
 */
@Component({
  selector: 'app-publication-detail',
  imports: [
    CodeCopy,
    ErrorState,
    MarkdownView,
    NgOptimizedImage,
    NgTemplateOutlet,
    RouterLink,
    SeriesNavigation,
    TableOfContents,
    TermLinks,
  ],
  host: { class: 'block page-container wrap-break-word' },
  template: `
    @if (publication.hasValue()) {
      @let current = publication.value();
      <ng-template #sheet>
        <dl class="article-sheet">
          <div>
            <dt>Publié le</dt>
            <dd>
              <time class="tabular-nums" [attr.datetime]="datetime()">{{ date() }}</time>
            </dd>
          </div>
          <div>
            <dt>Lecture</dt>
            <dd>{{ readingTime() }}</dd>
          </div>
          @if (current.category; as category) {
            <div>
              <dt>Catégorie</dt>
              <dd>
                <a [routerLink]="listPath()" [queryParams]="{ category: category.slug }">{{
                  category.name
                }}</a>
              </dd>
            </div>
          }
          @if (current.tags.length > 0) {
            <div>
              <dt>Tags</dt>
              <dd>
                <app-term-links
                  [terms]="current.tags"
                  [label]="'Tags de ' + current.title"
                  [path]="listPath()"
                  param="tag"
                />
              </dd>
            </div>
          }
        </dl>
      </ng-template>
      <ng-template #toc>
        <app-table-of-contents
          [entries]="rendered().headings"
          [path]="canonicalPath()"
          [label]="'Sommaire de « ' + current.title + ' »'"
        />
      </ng-template>

      <article class="article pt-section pb-block">
        <header class="article-header">
          <p class="text-sm">
            <a [routerLink]="listPath()">{{ labels().list }}</a>
          </p>
          <h1 class="mt-3 text-3xl leading-tight tracking-title">{{ current.title }}</h1>
          <p class="mt-flow max-w-prose font-text text-xl leading-snug">{{ current.summary }}</p>
          @if (seriesNavigation.hasValue()) {
            <app-series-navigation
              class="mt-flow block"
              variant="context"
              [navigation]="seriesNavigation.value()"
            />
          }
        </header>

        <div class="article-sheet-inline">
          <ng-container [ngTemplateOutlet]="sheet" />
        </div>

        @if (hasToc()) {
          <details class="article-toc-inline">
            <summary>Sommaire</summary>
            <ng-container [ngTemplateOutlet]="toc" />
          </details>
          <div class="article-toc-side">
            <p class="article-toc-title" aria-hidden="true">Sommaire</p>
            <ng-container [ngTemplateOutlet]="toc" />
          </div>
        }

        @if (current.cover; as cover) {
          <figure class="article-cover">
            <!-- Sous le titre et le résumé : au-dessus de la ligne de flottaison, prioritaire -->
            <img
              class="article-cover-image"
              [ngSrc]="cover.url"
              [width]="cover.width"
              [height]="cover.height"
              [alt]="cover.altText ?? ''"
              priority
            />
          </figure>
        }

        <div class="article-body">
          <div class="article-sheet-margin">
            <ng-container [ngTemplateOutlet]="sheet" />
          </div>
          <app-code-copy>
            <app-markdown-view class="prose-margin-notes" [rendered]="rendered()" />
          </app-code-copy>
          @if (seriesNavigation.hasValue()) {
            <app-series-navigation
              class="mt-section block"
              [navigation]="seriesNavigation.value()"
            />
          }
        </div>
      </article>
    } @else if (error(); as failure) {
      <div class="grid gap-3 py-section lg:grid-cols-12 lg:gap-8">
        <p class="text-sm font-semibold text-ink-muted lg:col-span-3 lg:pt-3">
          {{ failure.status === 404 ? 'Erreur 404' : labels().list }}
        </p>
        <div class="lg:col-span-9">
          @if (failure.status === 404) {
            <h1 class="text-3xl leading-tight tracking-title">{{ labels().notFound }}</h1>
            <p class="mt-flow max-w-prose font-text text-lg leading-prose">
              Aucune publication visible ne correspond à cette adresse.
            </p>
            <p class="mt-4">
              <a [routerLink]="listPath()">{{ labels().seeAll }}</a>
            </p>
          } @else {
            <h1 class="text-3xl leading-tight tracking-title">{{ labels().unavailable }}</h1>
            <div class="mt-block">
              <app-error-state
                [title]="labels().loadError"
                [detail]="failure.detail"
                (retry)="publication.reload()"
              />
            </div>
          }
        </div>
      </div>
    } @else {
      <!-- Navigation dans le navigateur seulement : le rendu serveur attend la réponse -->
      @defer (on timer(300ms)) {
        <p role="status" class="py-section text-ink-muted">Chargement…</p>
      }
    }
  `,
  styles: `
    .article {
      display: grid;
      grid-template-columns: minmax(0, 1fr);
      row-gap: var(--spacing-block);
      max-width: var(--container-prose);
    }

    .article-sheet {
      display: flex;
      flex-wrap: wrap;
      gap: calc(var(--spacing) * 2) calc(var(--spacing) * 6);
      font-size: var(--text-sm);
    }

    .article-sheet dt {
      color: var(--color-ink-muted);
    }

    .article-sheet dd {
      font-weight: var(--font-weight-medium);
    }

    .article-sheet-inline {
      padding-block: calc(var(--spacing) * 3);
      border-block: var(--border-rule) solid var(--color-rule);
    }

    .article-toc-inline {
      border: var(--border-rule) solid var(--color-rule);
      border-radius: var(--radius-media);
      padding: calc(var(--spacing) * 3) calc(var(--spacing) * 4);
    }

    .article-toc-inline summary {
      min-height: calc(var(--spacing) * 6);
      font-weight: var(--font-weight-semibold);
      cursor: pointer;
    }

    .article-toc-inline[open] summary {
      margin-block-end: calc(var(--spacing) * 2);
    }

    .article-toc-side,
    .article-sheet-margin {
      display: none;
    }

    .article-cover-image {
      width: 100%;
      height: auto;
      border: var(--border-rule) solid var(--color-rule);
      border-radius: var(--radius-media);
      background: var(--color-paper-sunken);
    }

    /* Planche : sommaire 13 rem · lecture 43 rem · marge 15 rem, 3,5 rem entre les colonnes */
    @media (min-width: 80rem) {
      .article {
        grid-template-columns: 13rem minmax(0, var(--container-prose)) 15rem;
        column-gap: calc(var(--spacing) * 14);
        max-width: none;
      }

      .article-header {
        grid-column: 2 / span 2;
        grid-row: 1;
      }

      .article-sheet-inline,
      .article-toc-inline {
        display: none;
      }

      .article-toc-side {
        display: block;
        grid-column: 1;
        grid-row: 1 / span 3;
        align-self: start;
        position: sticky;
        top: calc(var(--spacing) * 6);
        max-height: calc(100dvh - var(--spacing) * 12);
        overflow-y: auto;
        padding-block-start: calc(var(--spacing) * 3);
      }

      .article-toc-title {
        margin-block-end: calc(var(--spacing) * 2);
        color: var(--color-ink-muted);
        font-size: var(--text-sm);
        font-weight: var(--font-weight-semibold);
      }

      .article-cover {
        grid-column: 2 / span 2;
      }

      .article-body {
        grid-column: 2;
      }

      /* Fiche flottante dans la marge droite, en tête du contenu : les notes se rangent dessous */
      .article-sheet-margin {
        display: block;
        float: right;
        clear: right;
        width: 15rem;
        margin-inline-end: calc(-15rem - var(--spacing) * 14);
        margin-block-end: var(--spacing-block);
      }

      .article-sheet-margin .article-sheet {
        flex-direction: column;
        gap: 0;
        border-top: var(--border-strong) solid var(--color-ink);
      }

      .article-sheet-margin .article-sheet > div {
        padding-block: calc(var(--spacing) * 3);
        border-bottom: var(--border-rule) solid var(--color-rule);
      }
    }
  `,
})
export class PublicationDetail {
  /** Type attendu à cette adresse (donnée de la route). */
  readonly type = input.required<PublicationType>();
  /** Paramètre `:slug` de la route. */
  readonly slug = input.required<string>();

  protected readonly publication = publicationResource(() => this.slug());
  /** Place dans la série, pour un article seulement (une actualité n'entre dans aucune série). */
  protected readonly seriesNavigation = seriesNavigationResource(() =>
    this.type() === 'ARTICLE' ? this.slug() : null,
  );
  protected readonly labels = computed(() => PUBLICATION_LABELS[this.type()]);
  protected readonly listPath = computed(() => publicationListPath(this.type()));
  protected readonly canonicalPath = computed(() => publicationPath(this.type(), this.slug()));
  protected readonly error = computed(() =>
    this.publication.error() ? toApiError(this.publication.error()) : null,
  );

  private readonly math = inject(MarkdownMath);

  /** Un seul rendu du Markdown, pour le texte et pour le sommaire. */
  protected readonly rendered = computed(() =>
    renderMarkdown(this.publication.hasValue() ? this.publication.value().contentMarkdown : '', {
      path: this.canonicalPath(),
      codeToolbar: true,
      math: this.math.renderer(),
    }),
  );
  /** Sommaire seulement pour au moins deux titres de premier niveau. */
  protected readonly hasToc = computed(() => {
    const headings = this.rendered().headings;
    const top = Math.min(...headings.map((heading) => heading.level));
    return headings.filter((heading) => heading.level === top).length >= 2;
  });

  protected readonly date = computed(() =>
    this.publication.hasValue() ? formatDay(this.publication.value().publishedAt) : '',
  );
  protected readonly datetime = computed(() =>
    this.publication.hasValue() ? isoDay(this.publication.value().publishedAt) : '',
  );
  protected readonly readingTime = computed(() =>
    this.publication.hasValue()
      ? readingTimeLabel(this.publication.value().readingTimeMinutes)
      : '',
  );

  private readonly seo = inject(Seo);
  private readonly setResponseStatus = injectResponseStatus();
  private readonly redirect = injectPermanentRedirect();

  constructor() {
    effect(() => {
      if (this.publication.hasValue()) {
        const publication = this.publication.value();
        if (publication.type !== this.type()) {
          this.redirect(publicationPath(publication.type, publication.slug));
          return;
        }
        this.seo.set({
          title: publication.seoTitle ?? publication.title,
          description: publication.seoDescription ?? publication.summary,
          path: this.canonicalPath(),
          type: 'article',
          image: publication.cover?.url ?? null,
          jsonLd: publicationJsonLd(publication, (path) => this.seo.absolute(path)),
        });
        return;
      }
      const failure = this.error();
      if (failure?.status === 404) {
        this.setResponseStatus(404);
        this.seo.set({ title: this.labels().notFound, path: this.canonicalPath(), noindex: true });
      } else if (failure) {
        this.setResponseStatus(503);
      }
    });
  }
}
