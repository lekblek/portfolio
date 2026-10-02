import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { VOCABULARIES, VOCABULARY_KEYS } from '../vocabularies';

/**
 * Sous-sections de la taxonomie (Catégories, Tags, Technologies) : liens, la sous-section courante
 * repérée par un filet `accent` sous son nom (`aria-current="page"`), comme la navigation du site.
 */
@Component({
  selector: 'app-taxonomy-nav',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav aria-label="Sections de la taxonomie">
      <ul class="taxonomy-nav">
        @for (vocabulary of vocabularies; track vocabulary.key) {
          <li>
            <a
              class="taxonomy-nav-link"
              [routerLink]="['/admin/taxonomy', vocabulary.key]"
              routerLinkActive="taxonomy-nav-link-current"
              [routerLinkActiveOptions]="{ exact: true }"
              ariaCurrentWhenActive="page"
              >{{ vocabulary.plural }}</a
            >
          </li>
        }
      </ul>
    </nav>
  `,
  styles: `
    .taxonomy-nav {
      display: flex;
      flex-wrap: wrap;
      column-gap: calc(var(--spacing) * 6);
      border-bottom: var(--border-rule) solid var(--color-rule);
    }

    .taxonomy-nav-link {
      display: inline-flex;
      align-items: center;
      min-height: calc(var(--spacing) * 11);
      color: var(--color-ink);
      font-weight: var(--font-weight-medium);
      text-decoration-line: none;
    }

    .taxonomy-nav-link-current {
      box-shadow: inset 0 -2px 0 var(--color-accent);
    }

    @media (hover: hover) and (pointer: fine) {
      .taxonomy-nav-link:hover {
        color: var(--color-accent-strong);
      }
    }
  `,
})
export class TaxonomyNav {
  protected readonly vocabularies = VOCABULARY_KEYS.map((key) => VOCABULARIES[key]);
}
