import { Component, input } from '@angular/core';

/**
 * Entrée d'un registre de parcours (expérience, formation, certification), placée dans un
 * `<li>` : titre `<h3>`, précisions, date ou période, description. Dès que la liste (conteneur
 * de requête) dispose de 36 rem, la date passe dans une colonne fixe à gauche, alignée sur le
 * titre ; l'ordre du document (titre d'abord) reste l'ordre de lecture. Contenu projeté : ce
 * qui suit la description (validité, lien).
 */
@Component({
  selector: 'app-career-entry',
  template: `
    <h3 class="entry-heading text-lg">{{ heading() }}</h3>
    @if (details().length > 0) {
      <p class="entry-details text-ink-muted">
        @for (detail of details(); track $index) {
          @if (!$first) {
            <span aria-hidden="true">&nbsp;· </span>
          }
          <span>{{ detail }}</span>
        }
      </p>
    }
    <p class="entry-when text-sm text-ink-muted tabular-nums">{{ when() }}</p>
    @if (description()) {
      <p class="entry-description max-w-prose font-text leading-prose whitespace-pre-line">
        {{ description() }}
      </p>
    }
    <div class="entry-more">
      <ng-content />
    </div>
  `,
  styles: `
    :host {
      display: grid;
      grid-template-columns: minmax(0, 1fr);
      grid-template-areas: 'heading' 'details' 'when' 'description' 'more';
      align-items: baseline;
      column-gap: calc(var(--spacing) * 8);
      row-gap: calc(var(--spacing) * 1);
    }

    .entry-heading {
      grid-area: heading;
    }

    .entry-details {
      grid-area: details;
    }

    .entry-when {
      grid-area: when;
    }

    .entry-description {
      grid-area: description;
      margin-block-start: calc(var(--spacing) * 2);
      hyphens: auto;
    }

    .entry-more {
      grid-area: more;
    }

    .entry-more:empty {
      display: none;
    }

    @container (min-width: 36rem) {
      :host {
        grid-template-columns: 12rem minmax(0, 1fr);
        grid-template-areas:
          'when heading'
          'when details'
          'when description'
          'when more';
      }
    }
  `,
})
export class CareerEntry {
  readonly heading = input.required<string>();
  /** Précisions affichées sous le titre, séparées par « · » (organisation, lieu…). */
  readonly details = input<readonly string[]>([]);
  /** Date ou période déjà formatée. */
  readonly when = input.required<string>();
  readonly description = input<string | null>(null);
}
