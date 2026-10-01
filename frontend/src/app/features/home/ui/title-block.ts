import { Component, input } from '@angular/core';

import { formatDay, isoDay } from '../../../shared/format/date';

/**
 * Cartouche de l'accueil (rappel discret de DS01, 02-design-system §4) : le bloc-titre d'un plan,
 * qui réunit des faits réels. Une ligne sans donnée n'est pas affichée.
 */
@Component({
  selector: 'app-title-block',
  host: { class: 'block' },
  template: `
    <table class="title-block">
      <caption>
        En bref
      </caption>
      <tbody>
        @if (role(); as role) {
          <tr>
            <th scope="row">Rôle</th>
            <td>{{ role }}</td>
          </tr>
        }
        @if (location(); as location) {
          <tr>
            <th scope="row">Lieu</th>
            <td>{{ location }}</td>
          </tr>
        }
        @if (stack().length > 0) {
          <tr>
            <th scope="row">Pile</th>
            <td translate="no">{{ stack().join(', ') }}</td>
          </tr>
        }
        @if (projectCount() !== null) {
          <tr>
            <th scope="row">Projets publiés</th>
            <td class="tabular-nums">{{ projectCount() }}</td>
          </tr>
        }
        @if (publicationCount() !== null) {
          <tr>
            <th scope="row">Publications</th>
            <td class="tabular-nums">{{ publicationCount() }}</td>
          </tr>
        }
        @if (lastPublished(); as instant) {
          <tr>
            <th scope="row">Dernière publication</th>
            <td>
              <time class="tabular-nums" [attr.datetime]="day(instant)">{{ date(instant) }}</time>
            </td>
          </tr>
        }
      </tbody>
    </table>
  `,
  styles: `
    .title-block {
      width: 100%;
      border-collapse: collapse;
      border: var(--border-strong) solid var(--color-ink);
      font-size: var(--text-xs);
    }

    caption {
      padding-block-end: calc(var(--spacing) * 2);
      text-align: start;
      font-weight: var(--font-weight-semibold);
    }

    th,
    td {
      border-block-start: var(--border-rule) solid var(--color-rule);
      padding: calc(var(--spacing) * 2) calc(var(--spacing) * 3);
      text-align: start;
      vertical-align: top;
    }

    th {
      width: 42%;
      border-inline-end: var(--border-rule) solid var(--color-rule);
      color: var(--color-ink-muted);
      font-weight: var(--font-weight-medium);
    }

    tr:first-child th,
    tr:first-child td {
      border-block-start: 0;
    }
  `,
})
export class TitleBlock {
  readonly role = input<string | null>(null);
  readonly location = input<string | null>(null);
  /** Technologies des projets présentés, sans doublon. */
  readonly stack = input<string[]>([]);
  /** `null` : nombre inconnu (requête en cours ou en échec), ligne absente. */
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
