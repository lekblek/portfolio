import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SeriesNavigation as Navigation } from '../../../core/api/api-types';

/**
 * Place d'un article dans sa série (02-design-system §4.4). `context` : une ligne sous le résumé
 * (« Chapitre 2 sur 5 de la série … »). `chapters` : bloc de fin d'article encadré de deux traits
 * forts, chapitres précédent et suivant (`rel="prev"` / `rel="next"`), absents aux extrémités.
 */
@Component({
  selector: 'app-series-navigation',
  imports: [RouterLink],
  template: `
    @if (variant() === 'context') {
      <p class="text-sm">
        <span class="text-ink-muted tabular-nums"
          >Chapitre {{ navigation().position }} sur {{ navigation().chapterCount }} de la série </span
        ><a [routerLink]="['/series', navigation().series.slug]">{{ navigation().series.title }}</a>
      </p>
    } @else {
      <nav
        class="series-chapters"
        [attr.aria-label]="'Série «&#160;' + navigation().series.title + '&#160;»'"
      >
        <p class="series-chapters-title">
          <span class="text-sm text-ink-muted tabular-nums"
            >Chapitre {{ navigation().position }} sur {{ navigation().chapterCount }}</span
          >
          <a class="font-semibold" [routerLink]="['/series', navigation().series.slug]">{{
            navigation().series.title
          }}</a>
        </p>
        @if (navigation().previous || navigation().next) {
          <ul class="series-chapters-links">
            @if (navigation().previous; as previous) {
              <li class="series-chapters-previous">
                <span class="text-sm text-ink-muted">Chapitre précédent</span>
                <a rel="prev" [routerLink]="['/articles', previous.slug]">{{ previous.title }}</a>
              </li>
            }
            @if (navigation().next; as next) {
              <li class="series-chapters-next">
                <span class="text-sm text-ink-muted">Chapitre suivant</span>
                <a rel="next" [routerLink]="['/articles', next.slug]">{{ next.title }}</a>
              </li>
            }
          </ul>
        }
      </nav>
    }
  `,
  styles: `
    .series-chapters {
      padding-block: calc(var(--spacing) * 5);
      border-block: var(--border-strong) solid var(--color-ink);
      font-family: var(--font-display);
    }

    .series-chapters-title {
      display: flex;
      flex-direction: column;
      gap: calc(var(--spacing) * 1);
    }

    .series-chapters-links {
      display: grid;
      gap: calc(var(--spacing) * 4) calc(var(--spacing) * 8);
      margin-block-start: calc(var(--spacing) * 5);
      padding-block-start: calc(var(--spacing) * 4);
      border-top: var(--border-rule) solid var(--color-rule);
    }

    .series-chapters-links li {
      display: flex;
      flex-direction: column;
      gap: calc(var(--spacing) * 1);
    }

    @media (min-width: 40rem) {
      .series-chapters-links {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }

      .series-chapters-next {
        grid-column: 2;
        text-align: end;
      }
    }
  `,
})
export class SeriesNavigation {
  readonly navigation = input.required<Navigation>();
  readonly variant = input<'context' | 'chapters'>('chapters');
}
