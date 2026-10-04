import { Component, input } from '@angular/core';

import { formatDay, isoDay } from '../../../shared/format/date';

/**
 * Cartouche de l'accueil (rappel de DS01, revu en DS09) : le bloc-titre d'un plan, sous le
 * portrait, qui réunit des faits réels ; les nombres en grand, la pile et la dernière publication
 * sur toute la largeur. Une cellule sans donnée n'est pas affichée.
 */
@Component({
  selector: 'app-title-block',
  host: { class: 'block' },
  template: `
    <h2 class="sr-only">En bref</h2>
    <dl class="title-block">
      @if (projectCount() !== null) {
        <div class="fact">
          <dt>{{ projectCount() === 1 ? 'Projet publié' : 'Projets publiés' }}</dt>
          <dd class="fact-number tabular-nums">{{ projectCount() }}</dd>
        </div>
      }
      @if (publicationCount() !== null) {
        <div class="fact">
          <dt>{{ publicationCount() === 1 ? 'Publication' : 'Publications' }}</dt>
          <dd class="fact-number tabular-nums">{{ publicationCount() }}</dd>
        </div>
      }
      @if (stack().length > 0) {
        <div class="fact fact-wide">
          <dt>Pile des projets présentés</dt>
          <dd translate="no">{{ stack().join(', ') }}</dd>
        </div>
      }
      @if (lastPublished(); as instant) {
        <div class="fact fact-wide">
          <dt>Dernière publication</dt>
          <dd>
            <time class="tabular-nums" [attr.datetime]="day(instant)">{{ date(instant) }}</time>
          </dd>
        </div>
      }
    </dl>
  `,
  styles: `
    .title-block {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      border: var(--border-strong) solid var(--color-ink);
      font-size: var(--text-xs);
    }

    .fact {
      display: flex;
      flex-direction: column-reverse;
      justify-content: flex-end;
      gap: calc(var(--spacing) * 1);
      padding: calc(var(--spacing) * 3) calc(var(--spacing) * 4);
      border-block-start: var(--border-rule) solid var(--color-rule);
    }

    .fact:nth-child(-n + 2) {
      border-block-start: 0;
    }

    .fact:nth-child(2) {
      border-inline-start: var(--border-rule) solid var(--color-rule);
    }

    .fact-wide {
      grid-column: 1 / -1;
      flex-direction: column;
    }

    dt {
      color: var(--color-ink-muted);
      font-weight: var(--font-weight-medium);
    }

    .fact-wide dd {
      font-size: var(--text-sm);
    }

    .fact-number {
      font-family: var(--font-display);
      font-size: var(--text-3xl);
      font-weight: var(--font-weight-semibold);
      letter-spacing: var(--tracking-title);
      line-height: var(--leading-tight);
    }
  `,
})
export class TitleBlock {
  /** Technologies des projets présentés, sans doublon. */
  readonly stack = input<string[]>([]);
  /** `null` : nombre inconnu (requête en cours ou en échec), cellule absente. */
  readonly projectCount = input<number | null>(null);
  readonly publicationCount = input<number | null>(null);
  /** Instant de la publication la plus récente. */
  readonly lastPublished = input<string | null>(null);

  protected date(instant: string): string {
    return formatDay(instant);
  }

  protected day(instant: string): string {
    return isoDay(instant);
  }
}
