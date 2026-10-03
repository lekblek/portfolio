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
    <article class="register-entry" [class.register-with-cover]="project().cover">
      @if (headingLevel() === 2) {
        <h2 class="register-title">
          <a [routerLink]="link()">{{ project().title }}</a>
        </h2>
      } @else {
        <h3 class="register-title">
          <a [routerLink]="link()">{{ project().title }}</a>
        </h3>
      }
      <p class="register-meta">
        <span class="font-medium">{{ stage() }}<span class="sr-only">, </span></span>
        <span class="text-ink-muted tabular-nums">{{ period() }}</span>
      </p>
      <p class="register-summary max-w-prose font-text leading-prose">
        {{ project().shortDescription }}
      </p>
      @if (project().technologies.length > 0) {
        <app-term-links
          class="register-terms"
          [terms]="project().technologies"
          [label]="'Technologies de ' + project().title"
          path="/projects"
          param="technology"
          [current]="currentTechnology()"
        />
      }
      @if (project().cover; as cover) {
        <div class="register-cover">
          <!-- priority n'est lu qu'à la création de l'image : deux branches, recréées quand il change -->
          @if (coverPriority()) {
            <img [ngSrc]="cover.url" fill [alt]="cover.altText ?? ''" priority />
          } @else {
            <img [ngSrc]="cover.url" fill [alt]="cover.altText ?? ''" />
          }
        </div>
      }
    </article>
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
