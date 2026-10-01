import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Terme d'un vocabulaire du contrat (technologie, catégorie, tag) : nom affiché et slug. */
export interface Term {
  name: string;
  slug: string;
}

/**
 * Termes d'un contenu en liens typographiques, jamais en pastilles (02-design-system §18) : chaque
 * lien mène à la liste filtrée par ce terme (`/projects?technology=spring-boot`). Le terme du
 * filtre en cours est marqué (`aria-current`). Noms techniques protégés de la traduction.
 */
@Component({
  selector: 'app-term-links',
  imports: [RouterLink],
  template: `
    <ul class="term-list" [attr.aria-label]="label()" translate="no">
      @for (term of terms(); track term.slug) {
        <li>
          <a
            class="term-link"
            [routerLink]="path()"
            [queryParams]="queryFor(term)"
            [attr.aria-current]="term.slug === current() ? 'true' : null"
            >{{ term.name }}</a
          >
        </li>
      }
    </ul>
  `,
  styles: `
    .term-list {
      display: flex;
      flex-wrap: wrap;
      column-gap: calc(var(--spacing) * 2);
      font-size: var(--text-sm);
    }

    /* Séparateur visible, sans texte de remplacement : la liste énonce déjà chaque terme */
    .term-list > li {
      display: flex;
      align-items: center;
    }

    .term-list > li:not(:last-child)::after {
      content: '·' / '';
      margin-inline-start: calc(var(--spacing) * 2);
      color: var(--color-ink-muted);
    }

    /* Cible d'au moins 24 px (WCAG 2.5.8), même quand la liste passe sur plusieurs lignes */
    .term-link {
      display: inline-flex;
      align-items: center;
      min-height: calc(var(--spacing) * 6);
    }

    .term-link[aria-current] {
      color: var(--color-ink);
      font-weight: var(--font-weight-semibold);
    }
  `,
})
export class TermLinks {
  readonly terms = input.required<readonly Term[]>();
  /** Nom accessible de la liste (« Technologies de … »). */
  readonly label = input.required<string>();
  /** Page de la liste filtrée (`/projects`). */
  readonly path = input.required<string>();
  /** Paramètre de filtre de cette liste (`technology`). */
  readonly param = input.required<string>();
  /** Slug du filtre en cours, s'il y en a un. */
  readonly current = input<string | null>(null);

  /** Liste filtrée par ce terme seul, depuis sa première page. */
  protected queryFor(term: Term): Record<string, string> {
    return { [this.param()]: term.slug };
  }
}
