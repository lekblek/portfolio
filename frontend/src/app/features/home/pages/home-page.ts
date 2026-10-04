import { NgOptimizedImage } from '@angular/common';
import { Component, computed, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { toApiError } from '../../../core/api/api-error';
import { injectResponseStatus } from '../../../core/platform/response-status';
import { Seo } from '../../../core/seo/seo';
import { SITE_NAME, SITE_SIGNATURE } from '../../../core/seo/site-config';
import { NewsItem } from '../../../shared/content/news-item';
import { ProjectCard } from '../../../shared/content/project-card';
import { PublicationCard } from '../../../shared/content/publication-card';
import { readingTimeLabel } from '../../../shared/content/content-labels';
import { SeriesCard } from '../../../shared/content/series-card';
import { formatDay, isoDay } from '../../../shared/format/date';
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
/** Derniers articles demandés : l'article illustré le plus récent en tête, puis la liste. */
const ARTICLES = 6;
/** Titres de la liste « Derniers articles », à côté de l'article de tête. */
const LATEST_ARTICLES = 4;
const SERIES = 2;
const NEWS = 3;
/** Technologies retenues pour la ligne « Pile » du cartouche. */
const STACK = 6;

/**
 * Accueil (DS09) : énoncé (nom, titre, présentation courte, actions, liens professionnels), portrait
 * en planche et cartouche de faits réels, domaines (groupes de compétences du profil), puis une
 * zone par contenu — projets mis en avant (à défaut, les premiers projets ; le premier illustré en
 * tête), articles (le plus récent illustré en tête, puis les derniers titres), séries, dernières
 * actualités en dépêches, invitation au contact. Toutes les requêtes partent
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
    NewsItem,
    NgOptimizedImage,
    ProjectCard,
    PublicationCard,
    RouterLink,
    SeriesCard,
    TitleBlock,
  ],
  host: { class: 'block page-container wrap-break-word' },
  template: `
    <div class="home-intro pt-section pb-block">
      <div class="home-statement min-w-0">
        <h1 class="text-4xl leading-tight tracking-display">{{ name() }}</h1>
        @if (profile.hasValue()) {
          @let current = profile.value();
          <p class="mt-4 max-w-prose text-xl leading-snug tracking-heading">
            {{ current.professionalTitle }}
          </p>
          <p class="mt-flow max-w-prose font-text text-lg leading-prose">{{ current.shortBio }}</p>
          <div class="mt-block flex flex-wrap items-center gap-3">
            <a appButton routerLink="/projects">Voir les projets</a>
            @if (current.cv; as cv) {
              <a appButton variant="secondary" [href]="cv.url" [attr.download]="cvFileName()">
                Télécharger le CV
                <span class="font-normal text-ink-muted">(PDF, {{ size(cv.sizeBytes) }})</span>
              </a>
            }
            <a appButton variant="quiet" routerLink="/contact">Écrire un message</a>
          </div>
          @if (current.links.length > 0) {
            <ul class="mt-flow flex flex-wrap gap-x-6" aria-label="Liens professionnels">
              @for (link of current.links; track $index) {
                <li>
                  <a class="external inline-flex min-h-11 items-center" [href]="link.url"
                    >{{ link.label }}<span class="sr-only"> (site externe)</span></a
                  >
                </li>
              }
            </ul>
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
      <div class="home-portrait min-w-0">
        @if (avatar(); as image) {
          <figure class="home-portrait-figure">
            <div class="plate home-portrait-plate">
              <img
                [ngSrc]="image.url"
                fill
                sizes="(min-width: 64rem) 22rem, 14rem"
                [alt]="image.altText ?? ''"
                priority
              />
            </div>
            <figcaption class="plate-caption">
              {{ name() }}
              @if (location(); as place) {
                <span aria-hidden="true">·</span> {{ place }}
              }
            </figcaption>
          </figure>
        }
        <app-title-block
          [stack]="stack()"
          [projectCount]="projectCount()"
          [publicationCount]="publicationCount()"
          [lastPublished]="lastPublished()"
        />
      </div>
    </div>

    @if (skillGroups().length > 0) {
      <section class="home-domains" aria-labelledby="zone-domaines">
        <h2 id="zone-domaines" class="text-lg tracking-heading">Domaines</h2>
        <ul class="home-domain-list">
          @for (group of skillGroups(); track group.category) {
            <li>
              <h3 class="font-semibold">{{ group.category }}</h3>
              <p class="mt-2 text-sm text-ink-muted" translate="no">
                {{ skillNames(group.skills) }}
              </p>
            </li>
          }
        </ul>
      </section>
    }

    <app-home-zone
      [heading]="projectsHeading()"
      headingId="zone-projets"
      [wide]="true"
      [moreLink]="projectCount() ? '/projects' : null"
      [moreLabel]="'Tous les projets (' + projectCount() + ')'"
    >
      @if (projectsError(); as failure) {
        <app-error-state
          title="Les projets n’ont pas pu être chargés."
          [detail]="failure.detail"
          (retry)="featured.reload(); firstProjects.reload()"
        />
      } @else if (projectCount() === 0) {
        <app-empty-state message="Aucun projet n’est encore publié." />
      } @else if (projectCount() !== null) {
        @if (leadProject(); as lead) {
          <app-project-card [project]="lead" [headingLevel]="3" [lead]="true" />
        }
        @if (otherProjects().length > 0) {
          <ul class="card-grid" [class.mt-section]="leadProject()">
            @for (project of otherProjects(); track project.slug) {
              <li><app-project-card [project]="project" [headingLevel]="3" /></li>
            }
          </ul>
        }
      }
    </app-home-zone>

    @if (articlesError(); as failure) {
      <app-home-zone heading="Articles" headingId="zone-articles">
        <app-error-state
          title="Les articles n’ont pas pu être chargés."
          [detail]="failure.detail"
          (retry)="articles.reload()"
        />
      </app-home-zone>
    } @else if (articles.hasValue() && articles.value().totalElements > 0) {
      <app-home-zone
        heading="Articles"
        headingId="zone-articles"
        [wide]="true"
        moreLink="/articles"
        [moreLabel]="'Tous les articles (' + articles.value().totalElements + ')'"
      >
        <div class="home-articles" [class.home-articles-with-lead]="leadArticle()">
          @if (leadArticle(); as lead) {
            <app-publication-card [publication]="lead" [headingLevel]="3" />
          }
          <div class="min-w-0">
            <h3 class="text-sm font-semibold text-ink-muted">Derniers articles</h3>
            <ul class="mt-2 divide-y divide-rule border-t border-rule">
              @for (article of latestArticles(); track article.slug) {
                <li class="home-latest">
                  <a class="home-latest-link" [routerLink]="['/articles', article.slug]">{{
                    article.title
                  }}</a>
                  <p class="mt-1 text-sm text-ink-muted">
                    <time class="tabular-nums" [attr.datetime]="iso(article.publishedAt)">{{
                      day(article.publishedAt)
                    }}</time
                    ><span class="sr-only">, </span><span aria-hidden="true"> · </span
                    >{{ reading(article.readingTimeMinutes) }}
                  </p>
                </li>
              }
            </ul>
          </div>
        </div>
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
      <app-home-zone
        heading="Séries"
        headingId="zone-series"
        [wide]="true"
        moreLink="/series"
        [moreLabel]="'Toutes les séries (' + series.value().totalElements + ')'"
      >
        <ul class="flex flex-col gap-block">
          @for (entry of series.value().content; track entry.slug) {
            <li><app-series-card [series]="entry" [headingLevel]="3" /></li>
          }
        </ul>
      </app-home-zone>
    }

    @if (newsError(); as failure) {
      <app-home-zone heading="Actualités" headingId="zone-actualites">
        <app-error-state
          title="Les actualités n’ont pas pu être chargées."
          [detail]="failure.detail"
          (retry)="news.reload()"
        />
      </app-home-zone>
    } @else if (news.hasValue() && news.value().totalElements > 0) {
      <app-home-zone
        heading="Actualités"
        headingId="zone-actualites"
        moreLink="/news"
        [moreLabel]="'Toutes les actualités (' + news.value().totalElements + ')'"
      >
        <ul class="divide-y divide-rule">
          @for (item of news.value().content; track item.slug) {
            <li class="py-block first:pt-0 last:pb-0">
              <app-news-item [item]="item" />
            </li>
          }
        </ul>
      </app-home-zone>
    }

    <section class="home-contact" aria-labelledby="zone-contact">
      <div class="min-w-0">
        <h2 id="zone-contact" class="text-2xl tracking-heading">Contact</h2>
        <p class="mt-2 max-w-prose font-text text-lg leading-prose">
          Une question sur un projet, un article ou une collaboration&#8239;: la réponse arrive à
          l’adresse que vous indiquez.
        </p>
      </div>
      <p><a appButton routerLink="/contact">Écrire un message</a></p>
    </section>
  `,
  styles: `
    .home-intro {
      display: grid;
      gap: var(--spacing-block);
    }

    .home-portrait {
      display: grid;
      gap: var(--spacing-block);
      max-width: calc(var(--spacing) * 96);
    }

    .home-portrait-plate {
      aspect-ratio: var(--aspect-portrait);
      max-width: calc(var(--spacing) * 56);
    }

    @media (min-width: 40rem) and (max-width: 63.99rem) {
      .home-portrait {
        grid-template-columns: calc(var(--spacing) * 48) minmax(0, 1fr);
        align-items: start;
        max-width: none;
      }
    }

    @media (min-width: 64rem) {
      .home-intro {
        grid-template-columns: repeat(12, minmax(0, 1fr));
        column-gap: calc(var(--spacing) * 8);
        align-items: center;
      }

      .home-statement {
        grid-column: 1 / span 7;
      }

      .home-portrait {
        grid-column: 9 / span 4;
        max-width: none;
      }

      .home-portrait-plate {
        max-width: none;
      }
    }

    .home-domains {
      display: grid;
      gap: calc(var(--spacing) * 4);
      margin-block-end: var(--spacing-block);
      padding: var(--spacing-block) calc(var(--spacing) * 6);
      border-block: var(--border-strong) solid var(--color-ink);
      background: var(--color-paper-sunken);
    }

    .home-domain-list {
      display: grid;
      gap: calc(var(--spacing) * 6) calc(var(--spacing) * 8);
    }

    @media (min-width: 48rem) {
      .home-domain-list {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }

    @media (min-width: 64rem) {
      .home-domains {
        grid-template-columns: repeat(12, minmax(0, 1fr));
        column-gap: calc(var(--spacing) * 8);
      }

      .home-domains > h2 {
        grid-column: 1 / span 3;
      }

      .home-domain-list {
        grid-column: 4 / span 9;
        grid-template-columns: repeat(4, minmax(0, 1fr));
      }
    }

    .home-articles {
      display: grid;
      gap: var(--spacing-block);
    }

    @media (min-width: 64rem) {
      .home-articles-with-lead {
        grid-template-columns: minmax(0, 7fr) minmax(0, 5fr);
        column-gap: calc(var(--spacing) * 8);
      }
    }

    .home-latest {
      padding-block: calc(var(--spacing) * 4);
    }

    .home-latest-link {
      color: var(--color-ink);
      font-size: var(--text-lg);
      font-weight: var(--font-weight-semibold);
      letter-spacing: var(--tracking-heading);
      text-decoration-line: none;
    }

    .home-latest-link:hover,
    .home-latest-link:focus-visible {
      color: var(--color-accent-strong);
      text-decoration-line: underline;
    }

    .home-contact {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: var(--spacing-block);
      margin-block: var(--spacing-block) var(--spacing-section);
      padding: var(--spacing-block) calc(var(--spacing) * 6);
      border: var(--border-strong) solid var(--color-ink);
    }
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

  /** Projet de tête : le premier projet présenté qui a une couverture. */
  protected readonly leadProject = computed(
    () => this.shownProjects().find((project) => project.cover !== null) ?? null,
  );
  protected readonly otherProjects = computed(() =>
    this.shownProjects().filter((project) => project !== this.leadProject()),
  );

  /** Article de tête : le plus récent des derniers articles qui a une couverture. */
  protected readonly leadArticle = computed(() =>
    this.articles.hasValue()
      ? (this.articles.value().content.find((article) => article.cover !== null) ?? null)
      : null,
  );
  /** Derniers articles, hors article de tête. */
  protected readonly latestArticles = computed(() =>
    this.articles.hasValue()
      ? this.articles
          .value()
          .content.filter((article) => article !== this.leadArticle())
          .slice(0, LATEST_ARTICLES)
      : [],
  );

  protected readonly avatar = computed(() =>
    this.profile.hasValue() ? this.profile.value().avatar : null,
  );
  protected readonly location = computed(() =>
    this.profile.hasValue() ? this.profile.value().publicLocation : null,
  );
  protected readonly skillGroups = computed(() =>
    this.profile.hasValue() ? this.profile.value().skillGroups : [],
  );

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

  protected skillNames(skills: readonly { name: string }[]): string {
    return skills.map((skill) => skill.name).join(' · ');
  }

  protected day(instant: string): string {
    return formatDay(instant);
  }

  protected iso(instant: string): string {
    return isoDay(instant);
  }

  protected reading(minutes: number): string {
    return readingTimeLabel(minutes);
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
