import { NgOptimizedImage } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ProjectStage, ProjectSummary } from '../../core/api/api-types';
import { formatPeriod } from '../format/date';
import { TermLinks } from '../ui/term-link';

const STAGE_LABELS: Record<ProjectStage, string> = {
  IN_PROGRESS: 'En cours',
  COMPLETED: 'Terminé',
};

/** État d'un projet, en toutes lettres. */
export function projectStageLabel(stage: ProjectStage): string {
  return STAGE_LABELS[stage];
}

/**
 * Ligne du registre des projets (liste, accueil) : titre lié au détail, résumé, technologies
 * (liens vers la liste filtrée), état et période, couverture s'il y en a une. L'ordre du
 * document (titre d'abord) reste l'ordre de lecture.
 *
 * La couverture occupe un emplacement de rapport fixe (3:2), recadré sans déformation
 * (02-design-system §15) : vignette à côté du titre sur petit écran, 3 colonnes sur 12 dès que la
 * liste (conteneur de requête) dispose de 60 rem, où la ligne suit la grille des pages (état et
 * période sur 3 colonnes, texte sur 6). Toutes les couvertures ont ainsi la même taille : aucune,
 * plus loin dans la liste, ne dépasse la première, seule chargée en priorité.
 */
@Component({
  selector: 'app-project-entry',
  imports: [NgOptimizedImage, RouterLink, TermLinks],
  template: `
    <article class="entry" [class.entry-with-cover]="project().cover">
      @if (headingLevel() === 2) {
        <h2 class="entry-title">
          <a [routerLink]="link()">{{ project().title }}</a>
        </h2>
      } @else {
        <h3 class="entry-title">
          <a [routerLink]="link()">{{ project().title }}</a>
        </h3>
      }
      <p class="entry-meta">
        <span class="font-medium">{{ stage() }}<span class="sr-only">, </span></span>
        <span class="text-ink-muted tabular-nums">{{ period() }}</span>
      </p>
      <p class="entry-summary max-w-prose font-text leading-prose">
        {{ project().shortDescription }}
      </p>
      @if (project().technologies.length > 0) {
        <app-term-links
          class="entry-terms"
          [terms]="project().technologies"
          [label]="'Technologies de ' + project().title"
          path="/projects"
          param="technology"
          [current]="currentTechnology()"
        />
      }
      @if (project().cover; as cover) {
        <div class="entry-cover">
          <img [ngSrc]="cover.url" fill [alt]="cover.altText ?? ''" [priority]="coverPriority()" />
        </div>
      }
    </article>
  `,
  styles: `
    .entry {
      display: grid;
      grid-template-columns: minmax(0, 1fr);
      grid-template-areas: 'meta' 'title' 'summary' 'terms';
      column-gap: calc(var(--spacing) * 4);
      row-gap: calc(var(--spacing) * 2);
    }

    .entry-with-cover {
      grid-template-columns: minmax(0, 1fr) calc(var(--spacing) * 24);
      grid-template-areas: 'meta cover' 'title cover' 'summary summary' 'terms terms';
    }

    .entry-title {
      grid-area: title;
      font-size: var(--text-xl);
      letter-spacing: var(--tracking-heading);
    }

    /* Titre lié : la ligne entière est un registre de liens, le soulignement vient au survol */
    .entry-title a {
      color: var(--color-ink);
      text-decoration-line: none;
    }

    .entry-title a:hover,
    .entry-title a:focus-visible {
      color: var(--color-accent-strong);
      text-decoration-line: underline;
    }

    .entry-meta {
      grid-area: meta;
      display: flex;
      flex-wrap: wrap;
      column-gap: calc(var(--spacing) * 2);
      font-size: var(--text-sm);
    }

    .entry-summary {
      grid-area: summary;
    }

    .entry-terms {
      grid-area: terms;
      margin-block-start: calc(var(--spacing) * 1);
    }

    .entry-cover {
      grid-area: cover;
      align-self: start;
      position: relative;
      aspect-ratio: 3 / 2;
      overflow: hidden;
      border: var(--border-rule) solid var(--color-rule);
      border-radius: var(--radius-media);
      background: var(--color-paper-sunken);
    }

    .entry-cover img {
      object-fit: cover;
    }

    @container (min-width: 60rem) {
      .entry,
      .entry-with-cover {
        grid-template-columns: repeat(12, minmax(0, 1fr));
        /* La hauteur de la couverture s'ajoute à la dernière ligne, pas entre titre et résumé */
        grid-template-rows: auto auto 1fr;
        grid-template-areas: none;
        column-gap: calc(var(--spacing) * 8);
        align-items: start;
      }

      .entry-meta {
        grid-column: 1 / span 3;
        grid-row: 1 / span 3;
        flex-direction: column;
        padding-block-start: calc(var(--spacing) * 1);
      }

      .entry-title,
      .entry-summary,
      .entry-terms {
        grid-column: 4 / span 6;
      }

      .entry-title {
        grid-row: 1;
      }

      .entry-summary {
        grid-row: 2;
      }

      .entry-terms {
        grid-row: 3;
      }

      .entry-cover {
        grid-column: 10 / span 3;
        grid-row: 1 / span 3;
      }
    }
  `,
})
export class ProjectEntry {
  readonly project = input.required<ProjectSummary>();
  /** `<h2>` dans la liste des projets, `<h3>` sous une section (accueil). */
  readonly headingLevel = input<2 | 3>(2);
  /** Technologie du filtre en cours, marquée parmi les liens. */
  readonly currentTechnology = input<string | null>(null);
  /** Couverture de la première ligne, au-dessus de la ligne de flottaison : chargée en priorité. */
  readonly coverPriority = input(false);

  protected readonly link = computed(() => ['/projects', this.project().slug]);
  protected readonly stage = computed(() => projectStageLabel(this.project().stage));
  protected readonly period = computed(() =>
    formatPeriod(this.project().startDate, this.project().endDate),
  );
}
