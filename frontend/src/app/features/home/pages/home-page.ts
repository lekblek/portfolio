import { Component, computed, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { toApiError } from '../../../core/api/api-error';
import { injectResponseStatus } from '../../../core/platform/response-status';
import { Seo } from '../../../core/seo/seo';
import { SITE_NAME, SITE_SIGNATURE } from '../../../core/seo/site-config';
import { ProjectEntry } from '../../../shared/content/project-entry';
import { PublicationEntry } from '../../../shared/content/publication-entry';
import { SeriesEntry } from '../../../shared/content/series-entry';
import { formatFileSize } from '../../../shared/format/file-size';
import { Button } from '../../../shared/ui/button';
import { EmptyState } from '../../../shared/ui/empty-state';
import { ErrorState } from '../../../shared/ui/error-state';
import {
  featuredProjectsResource,
  firstProjectsResource,
  firstSeriesResource,
  homeProfileResource,
  latestPublicationsResource,
} from '../data/home.resources';
import { HomeZone } from '../ui/home-zone';
import { TitleBlock } from '../ui/title-block';

/** Entrées par bloc : un aperçu, la liste complète est à un lien. */
const PROJECTS = 3;
const ARTICLES = 3;
const SERIES = 2;
const NEWS = 3;
/** Technologies retenues pour la ligne « Pile » du cartouche. */
const STACK = 6;

/**
 * Accueil : énoncé typographique (nom, titre, présentation courte, CV et liens professionnels)
 * et cartouche de faits réels, puis une zone par contenu — projets mis en avant (à défaut, les
 * premiers projets), derniers articles, séries, dernières actualités, invitation au contact. Toutes les requêtes partent
 * en même temps ; le rendu serveur les attend. Une zone sans contenu n'est pas affichée, sauf
 * celle des projets ; une requête en échec donne l'état d'erreur de sa zone et la réponse 503
 * au rendu serveur, pour qu'une page incomplète ne soit ni indexée ni mise en cache.
 */
@Component({
  selector: 'app-home-page',
  imports: [
    Button,
    EmptyState,
    ErrorState,
    HomeZone,
    ProjectEntry,
    PublicationEntry,
    RouterLink,
    SeriesEntry,
    TitleBlock,
  ],
  host: { class: 'block page-container wrap-break-word' },
  template: `
    <div class="grid gap-block pt-section pb-block lg:grid-cols-12 lg:gap-8">
      <div class="min-w-0 lg:col-span-8">
        <h1 class="text-4xl leading-tight tracking-title">{{ name() }}</h1>
        @if (profile.hasValue()) {
          @let current = profile.value();
          <p class="mt-4 max-w-prose text-xl leading-snug tracking-heading">
            {{ current.professionalTitle }}
          </p>
          <p class="mt-flow max-w-prose font-text text-lg leading-prose">{{ current.shortBio }}</p>
          @if (current.cv || current.links.length > 0) {
            <div class="mt-block flex flex-wrap items-center gap-x-8 gap-y-2">
              @if (current.cv; as cv) {
                <a appButton variant="secondary" [href]="cv.url" [attr.download]="cvFileName()">
                  Télécharger le CV
                  <span class="font-normal text-ink-muted">(PDF, {{ size(cv.sizeBytes) }})</span>
                </a>
              }
              @if (current.links.length > 0) {
                <ul class="flex flex-wrap gap-x-6" aria-label="Liens professionnels">
                  @for (link of current.links; track $index) {
                    <li>
                      <a class="external inline-flex min-h-11 items-center" [href]="link.url"
                        >{{ link.label }}<span class="sr-only"> (site externe)</span></a
                      >
                    </li>
                  }
                </ul>
              }
            </div>
          }
        } @else if (profileError(); as failure) {
          <p class="mt-4 max-w-prose text-xl leading-snug tracking-heading">{{ signature }}</p>
          @if (failure.status !== 404) {
            <div class="mt-block max-w-prose">
              <app-error-state
                title="La présentation n’a pas pu être chargée."
                [detail]="failure.detail"
                (retry)="profile.reload()"
              />
            </div>
          }
        } @else {
          <!-- Navigation dans le navigateur seulement : le rendu serveur attend la réponse -->
          @defer (on timer(300ms)) {
            <p role="status" class="mt-4 text-ink-muted">Chargement…</p>
          }
        }
      </div>
      <app-title-block
        class="max-w-md lg:col-span-4 lg:max-w-none lg:self-end"
        [role]="profile.hasValue() ? profile.value().professionalTitle : null"
        [location]="profile.hasValue() ? profile.value().publicLocation : null"
        [stack]="stack()"
        [projectCount]="projectCount()"
        [publicationCount]="publicationCount()"
        [lastPublished]="lastPublished()"
      />
    </div>

    <app-home-zone [heading]="projectsHeading()" headingId="zone-projets">
      @if (projectsError(); as failure) {
        <app-error-state
          title="Les projets n’ont pas pu être chargés."
          [detail]="failure.detail"
          (retry)="featured.reload(); firstProjects.reload()"
        />
      } @else if (projectCount() === 0) {
        <app-empty-state message="Aucun projet n’est encore publié." />
      } @else if (projectCount() !== null) {
        <ul class="divide-y divide-rule">
          @for (project of shownProjects(); track project.slug) {
            <li class="py-block first:pt-0 last:pb-0">
              <app-project-entry [project]="project" [headingLevel]="3" />
            </li>
          }
        </ul>
        <p class="mt-block">
          <a routerLink="/projects">Tous les projets ({{ projectCount() }})</a>
        </p>
      }
    </app-home-zone>

    @if (articlesError(); as failure) {
      <app-home-zone heading="Derniers articles" headingId="zone-articles">
        <app-error-state
          title="Les articles n’ont pas pu être chargés."
          [detail]="failure.detail"
          (retry)="articles.reload()"
        />
      </app-home-zone>
    } @else if (articles.hasValue() && articles.value().totalElements > 0) {
      <app-home-zone heading="Derniers articles" headingId="zone-articles">
        <ul class="divide-y divide-rule">
          @for (article of articles.value().content; track article.slug) {
            <li class="py-block first:pt-0 last:pb-0">
              <app-publication-entry [publication]="article" [headingLevel]="3" />
            </li>
          }
        </ul>
        <p class="mt-block">
          <a routerLink="/articles">Tous les articles ({{ articles.value().totalElements }})</a>
        </p>
      </app-home-zone>
    }

    @if (seriesError(); as failure) {
      <app-home-zone heading="Séries" headingId="zone-series">
        <app-error-state
          title="Les séries n’ont pas pu être chargées."
          [detail]="failure.detail"
          (retry)="series.reload()"
        />
      </app-home-zone>
    } @else if (series.hasValue() && series.value().totalElements > 0) {
      <app-home-zone heading="Séries" headingId="zone-series">
        <ul class="divide-y divide-rule">
          @for (entry of series.value().content; track entry.slug) {
            <li class="py-block first:pt-0 last:pb-0">
              <app-series-entry [series]="entry" [headingLevel]="3" />
            </li>
          }
        </ul>
        <p class="mt-block">
          <a routerLink="/series">Toutes les séries ({{ series.value().totalElements }})</a>
        </p>
      </app-home-zone>
    }

    @if (newsError(); as failure) {
      <app-home-zone heading="Dernières actualités" headingId="zone-actualites">
        <app-error-state
          title="Les actualités n’ont pas pu être chargées."
          [detail]="failure.detail"
          (retry)="news.reload()"
        />
      </app-home-zone>
    } @else if (news.hasValue() && news.value().totalElements > 0) {
      <app-home-zone heading="Dernières actualités" headingId="zone-actualites">
        <ul class="divide-y divide-rule">
          @for (item of news.value().content; track item.slug) {
            <li class="py-block first:pt-0 last:pb-0">
              <app-publication-entry [publication]="item" [headingLevel]="3" />
            </li>
          }
        </ul>
        <p class="mt-block">
          <a routerLink="/news">Toutes les actualités ({{ news.value().totalElements }})</a>
        </p>
      </app-home-zone>
    }

    <app-home-zone heading="Contact" headingId="zone-contact">
      <p class="max-w-prose font-text text-lg leading-prose">
        Une question sur un projet, un article ou une collaboration&#8239;:
        <a routerLink="/contact">écrire un message</a>.
      </p>
    </app-home-zone>
  `,
})
export class HomePage {
  protected readonly signature = SITE_SIGNATURE;

  protected readonly profile = homeProfileResource();
  protected readonly featured = featuredProjectsResource(PROJECTS);
  protected readonly firstProjects = firstProjectsResource(PROJECTS);
  protected readonly articles = latestPublicationsResource('ARTICLE', ARTICLES);
  protected readonly series = firstSeriesResource(SERIES);
  protected readonly news = latestPublicationsResource('NEWS', NEWS);

  protected readonly profileError = computed(() =>
    this.profile.error() ? toApiError(this.profile.error()) : null,
  );
  protected readonly projectsError = computed(() => {
    const error = this.featured.error() ?? this.firstProjects.error();
    return error ? toApiError(error) : null;
  });
  protected readonly articlesError = computed(() =>
    this.articles.error() ? toApiError(this.articles.error()) : null,
  );
  protected readonly seriesError = computed(() =>
    this.series.error() ? toApiError(this.series.error()) : null,
  );
  protected readonly newsError = computed(() =>
    this.news.error() ? toApiError(this.news.error()) : null,
  );

  /** Nom du profil ; sans profil publié, celui du site. */
  protected readonly name = computed(() =>
    this.profile.hasValue() ? this.profile.value().displayName : SITE_NAME,
  );

  /** Projets mis en avant ; s'il n'y en a aucun, les premiers projets publiés. */
  protected readonly shownProjects = computed(() => {
    const featured = this.featured.hasValue() ? this.featured.value().content : [];
    if (featured.length > 0) {
      return featured;
    }
    return this.firstProjects.hasValue() ? this.firstProjects.value().content : [];
  });

  protected readonly projectsHeading = computed(() =>
    this.featured.hasValue() && this.featured.value().totalElements > 0
      ? 'Projets mis en avant'
      : 'Projets',
  );

  /** Nombre de projets publiés, connu une fois les deux requêtes de projets reçues. */
  protected readonly projectCount = computed(() =>
    this.featured.hasValue() && this.firstProjects.hasValue()
      ? this.firstProjects.value().totalElements
      : null,
  );

  protected readonly publicationCount = computed(() =>
    this.articles.hasValue() && this.news.hasValue()
      ? this.articles.value().totalElements + this.news.value().totalElements
      : null,
  );

  /** Technologies des projets présentés, dans leur ordre d'apparition, sans doublon. */
  protected readonly stack = computed(() => {
    const names = this.shownProjects().flatMap((project) =>
      project.technologies.map((technology) => technology.name),
    );
    return [...new Set(names)].slice(0, STACK);
  });

  /** Instant de la publication visible la plus récente, article ou actualité. */
  protected readonly lastPublished = computed(() => {
    const latest = [this.articles, this.news]
      .map((resource) => (resource.hasValue() ? resource.value().content[0]?.publishedAt : null))
      .filter((instant): instant is string => !!instant)
      .sort();
    return latest.at(-1) ?? null;
  });

  /** Nom proposé à l'enregistrement : la clé de stockage du fichier ne dit rien au visiteur. */
  protected readonly cvFileName = computed(() => `CV ${this.name()}.pdf`);

  protected size(bytes: number): string {
    return formatFileSize(bytes);
  }

  private readonly seo = inject(Seo);
  private readonly setResponseStatus = injectResponseStatus();

  constructor() {
    effect(() => {
      const failed =
        (this.profileError() !== null && this.profileError()?.status !== 404) ||
        this.projectsError() !== null ||
        this.articlesError() !== null ||
        this.seriesError() !== null ||
        this.newsError() !== null;
      if (failed) {
        this.setResponseStatus(503);
      }
      const profile = this.profile.hasValue() ? this.profile.value() : null;
      this.seo.set({
        path: '/',
        description: profile?.shortBio ?? null,
        image: profile?.avatar?.url ?? null,
      });
    });
  }
}
