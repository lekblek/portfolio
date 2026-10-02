import {
  afterNextRender,
  Component,
  DestroyRef,
  ElementRef,
  inject,
  input,
  signal,
  ViewEncapsulation,
} from '@angular/core';

/**
 * Cadre d'un tableau de données (02-design-system §18) : `<table class="data-table">` projeté, en
 * `<th scope>` et `<caption>`. Sur un écran trop étroit, le tableau défile horizontalement dans ce
 * cadre (la page jamais) ; le cadre devient alors une région nommée atteignable au clavier, pour
 * que le défilement soit possible sans souris. Styles globaux (le tableau est projeté), chargés avec
 * le premier tableau : jamais par le site public.
 */
@Component({
  selector: 'app-data-table',
  host: {
    class: 'data-table-frame',
    role: 'region',
    '[attr.aria-label]': 'label()',
    '[attr.tabindex]': 'scrollable() ? 0 : null',
  },
  template: '<ng-content />',
  encapsulation: ViewEncapsulation.None,
  styles: `
    /* Positionné : les textes masqués (sr-only, en position absolue) restent dans le cadre défilant */
    .data-table-frame {
      position: relative;
      display: block;
      overflow-x: auto;
    }

    .data-table {
      width: 100%;
      border-collapse: collapse;
    }

    .data-table caption {
      text-align: start;
    }

    .data-table thead th {
      padding: calc(var(--spacing) * 2) calc(var(--spacing) * 3);
      border-bottom: var(--border-strong) solid var(--color-ink);
      color: var(--color-ink-muted);
      font-size: var(--text-sm);
      font-weight: var(--font-weight-semibold);
      text-align: start;
      white-space: nowrap;
    }

    /* En-tête de ligne (th scope="row") : une cellule comme les autres, en graisse moyenne */
    .data-table td,
    .data-table tbody th {
      padding: calc(var(--spacing) * 2) calc(var(--spacing) * 3);
      border-bottom: var(--border-rule) solid var(--color-rule);
      vertical-align: middle;
      text-align: start;
    }

    .data-table tbody th {
      font-weight: var(--font-weight-medium);
    }

    .data-table th:first-child,
    .data-table td:first-child {
      padding-inline-start: 0;
    }

    .data-table th:last-child,
    .data-table td:last-child {
      padding-inline-end: 0;
    }

    /* Colonne de nombres : chiffres à chasse fixe (pas tout le tableau : la ponctuation s'élargirait
       aussi), alignés à droite */
    .data-table .data-table-number {
      font-variant-numeric: tabular-nums;
      text-align: end;
    }

    /* Identifiants techniques (slug) : chasse fixe, jamais coupés */
    .data-table .data-table-code {
      font-family: var(--font-code);
      font-size: var(--text-sm);
      white-space: nowrap;
    }

    /* Actions de la ligne, alignées à droite, sans retour à la ligne */
    .data-table .data-table-actions {
      text-align: end;
      white-space: nowrap;
    }
  `,
})
export class DataTable {
  /** Nom de la région : celui du tableau (« Catégories »). */
  readonly label = input.required<string>();

  protected readonly scrollable = signal(false);

  constructor() {
    const host: HTMLElement = inject(ElementRef).nativeElement;
    const destroyRef = inject(DestroyRef);
    afterNextRender(() => {
      const measure = () => this.scrollable.set(host.scrollWidth > host.clientWidth);
      const observer = new ResizeObserver(measure);
      observer.observe(host);
      const table = host.querySelector('table');
      if (table) {
        observer.observe(table);
      }
      destroyRef.onDestroy(() => observer.disconnect());
    });
  }
}
