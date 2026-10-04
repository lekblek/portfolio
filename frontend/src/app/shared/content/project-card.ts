import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ProjectSummary } from '../../core/api/api-types';
import { formatPeriod } from '../format/date';
import { TermLinks } from '../ui/term-link';
import { CARD_SIZES, ContentPlate, LEAD_SIZES } from './content-plate';
import { projectStageLabel } from './content-labels';

/** Technologies montrées sur une carte ; les autres sont comptées (« + 2 »). */
const SHOWN_TECHNOLOGIES = 4;

/**
 * Carte de projet (DS09, `styles/plates.css`) : planche (couverture ou planche vide), état et
 * période, titre lié au détail, description courte, quatre technologies au plus (liens vers la
 * liste filtrée). `lead` : carte de tête, planche à gauche et texte à droite quand la place le
 * permet. Le titre est le seul lien de la carte ; sa cible couvre la carte entière.
 */
@Component({
  selector: 'app-project-card',
  imports: [ContentPlate, RouterLink, TermLinks],
  host: { class: '@container block' },
  template: `
    <article class="card" [class.card-lead]="lead()">
      @if (headingLevel() === 2) {
        <h2 class="card-title">
          <a class="card-link" [routerLink]="link()">{{ project().title }}</a>
        </h2>
      } @else {
        <h3 class="card-title">
          <a class="card-link" [routerLink]="link()">{{ project().title }}</a>
        </h3>
      }
      <p class="card-meta">
        <strong>{{ stage() }}</strong
        ><span class="sr-only">, </span>
        <span class="tabular-nums">{{ period() }}</span>
      </p>
      <p class="card-summary">{{ project().shortDescription }}</p>
      @if (project().technologies.length > 0) {
        <div class="card-terms">
          <app-term-links
            [terms]="shownTechnologies()"
            [label]="'Technologies de ' + project().title"
            path="/projects"
            param="technology"
            [current]="currentTechnology()"
          />
          @if (hiddenCount() > 0) {
            <span class="text-ink-muted"
              >+&nbsp;{{ hiddenCount()
              }}<span class="sr-only">
                {{ hiddenCount() === 1 ? 'autre technologie' : 'autres technologies' }}</span
              ></span
            >
          }
        </div>
      }
      <!-- Une carte qui passe en tête change de planche : sizes n'est lu qu'à la création (NG02953) -->
      @if (lead()) {
        <app-content-plate
          class="card-plate"
          [image]="project().cover"
          [title]="project().title"
          [priority]="coverPriority()"
          [sizes]="leadSizes"
        />
      } @else {
        <app-content-plate
          class="card-plate"
          [image]="project().cover"
          [title]="project().title"
          [priority]="coverPriority()"
          [sizes]="cardSizes"
        />
      }
    </article>
  `,
})
export class ProjectCard {
  readonly project = input.required<ProjectSummary>();
  /** `<h2>` dans la liste des projets, `<h3>` sous une section (accueil). */
  readonly headingLevel = input<2 | 3>(2);
  readonly lead = input(false);
  /** Technologie du filtre en cours, marquée parmi les liens. */
  readonly currentTechnology = input<string | null>(null);
  /** Couverture du haut de page (LCP) : chargée en priorité. */
  readonly coverPriority = input(false);

  protected readonly link = computed(() => ['/projects', this.project().slug]);
  protected readonly stage = computed(() => projectStageLabel(this.project().stage));
  protected readonly period = computed(() =>
    formatPeriod(this.project().startDate, this.project().endDate),
  );
  /** La technologie du filtre en cours reste visible, même au-delà des quatre premières. */
  protected readonly shownTechnologies = computed(() => {
    const all = this.project().technologies;
    const shown = all.slice(0, SHOWN_TECHNOLOGIES);
    const current = all.find((technology) => technology.slug === this.currentTechnology());
    return current && !shown.includes(current)
      ? [...shown.slice(0, SHOWN_TECHNOLOGIES - 1), current]
      : shown;
  });
  protected readonly hiddenCount = computed(
    () => this.project().technologies.length - this.shownTechnologies().length,
  );
  protected readonly cardSizes = CARD_SIZES;
  protected readonly leadSizes = LEAD_SIZES;
}
