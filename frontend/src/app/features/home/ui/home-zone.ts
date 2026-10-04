import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Zone de l'accueil, sous un filet. Par défaut, repère (titre `<h2>`) dans la colonne de gauche
 * dès `lg` et contenu dans les neuf autres colonnes (02-design-system §4.3). `wide` (DS09) : titre
 * et lien « Tous les … » sur une ligne, contenu sur toute la largeur, pour les compositions
 * illustrées (projets, articles, séries). Un seul `<ng-content>` : un contenu projeté ne l'est
 * qu'une fois, même dans deux branches.
 */
@Component({
  selector: 'app-home-zone',
  imports: [RouterLink],
  host: { class: 'block' },
  template: `
    <section
      class="home-zone"
      [class.home-zone-split]="!wide()"
      [attr.aria-labelledby]="headingId()"
    >
      <div class="home-zone-head">
        <h2 [id]="headingId()" class="text-2xl tracking-heading">
          {{ heading() }}
        </h2>
        @if (wide() && moreLink(); as link) {
          <a [routerLink]="link">{{ moreLabel() }}</a>
        }
      </div>
      <div class="@container min-w-0">
        <ng-content />
        @if (!wide() && moreLink(); as link) {
          <p class="mt-block">
            <a [routerLink]="link">{{ moreLabel() }}</a>
          </p>
        }
      </div>
    </section>
  `,
  styles: `
    .home-zone {
      display: grid;
      gap: calc(var(--spacing) * 4);
      padding-block: var(--spacing-block);
      border-top: var(--border-rule) solid var(--color-rule);
    }

    .home-zone-head {
      display: flex;
      flex-wrap: wrap;
      align-items: baseline;
      justify-content: space-between;
      column-gap: calc(var(--spacing) * 8);
      row-gap: calc(var(--spacing) * 2);
    }

    .home-zone:not(.home-zone-split) {
      gap: var(--spacing-block);
    }

    @media (min-width: 64rem) {
      .home-zone-split {
        grid-template-columns: repeat(12, minmax(0, 1fr));
        column-gap: calc(var(--spacing) * 8);
      }

      .home-zone-split > .home-zone-head {
        grid-column: 1 / span 3;
        align-self: start;
      }

      .home-zone-split h2 {
        font-size: var(--text-lg);
      }

      .home-zone-split > :last-child {
        grid-column: 4 / span 9;
      }
    }
  `,
})
export class HomeZone {
  readonly heading = input.required<string>();
  /** Identifiant du titre, préfixé `zone-`. */
  readonly headingId = input.required<string>();
  readonly wide = input(false);
  /** Liste complète (« Tous les projets (16) »), si elle est connue. */
  readonly moreLink = input<string | null>(null);
  readonly moreLabel = input('');
}
