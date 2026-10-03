import { LiveAnnouncer } from '@angular/cdk/a11y';
import { CdkDrag, CdkDragDrop, CdkDragHandle, CdkDropList } from '@angular/cdk/drag-drop';
import {
  afterNextRender,
  Component,
  computed,
  ElementRef,
  inject,
  Injector,
  input,
  output,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { Button } from './button';

/** Déplacement demandé : de la position `from` à la position `to` (base 0). */
export interface SortableMove {
  from: number;
  to: number;
}

/** Renvoie une copie de `items` où l'élément `from` est passé en position `to`. */
export function moveItem<T>(items: readonly T[], { from, to }: SortableMove): T[] {
  const result = [...items];
  const [moved] = result.splice(from, 1);
  result.splice(to, 0, moved);
  return result;
}

/**
 * Liste ordonnable (02-design-system §18) : chaque `app-sortable-item` projeté peut être déplacé par
 * ses boutons « Monter » et « Descendre » (clavier, lecteur d'écran, tactile) ou, à la souris, en le
 * glissant par sa poignée (CDK). La page applique le déplacement émis (`moved`) à son modèle ; chaque
 * déplacement est annoncé. Rôle `list` nommé, éléments `listitem`.
 */
@Component({
  selector: 'app-sortable-list',
  hostDirectives: [CdkDropList],
  host: { class: 'sortable-list', role: 'list', '[attr.aria-label]': 'label()' },
  template: '<ng-content />',
  styles: `
    :host {
      display: grid;
      gap: calc(var(--spacing) * 3);
    }
  `,
})
export class SortableList {
  /** Nom de la liste pour les technologies d'assistance (« Liens professionnels »). */
  readonly label = input.required<string>();
  readonly moved = output<SortableMove>();

  constructor() {
    inject(CdkDropList)
      .dropped.pipe(takeUntilDestroyed())
      .subscribe((event: CdkDragDrop<unknown>) => {
        if (event.previousIndex !== event.currentIndex) {
          this.moved.emit({ from: event.previousIndex, to: event.currentIndex });
        }
      });
  }
}

/**
 * Élément d'une liste ordonnable : en-tête (poignée de glisser-déposer pour la souris, position,
 * « Monter », « Descendre », « Retirer ») puis les champs projetés. Les boutons portent le nom de
 * l'élément (`name`, pour les lecteurs d'écran) ; après un déplacement au clavier, le focus reste sur
 * le bouton utilisé, ou passe à l'autre quand l'élément atteint une extrémité.
 */
@Component({
  selector: 'app-sortable-item',
  imports: [Button, CdkDragHandle],
  hostDirectives: [CdkDrag],
  host: { class: 'sortable-item', role: 'listitem' },
  template: `
    <div class="sortable-item-bar">
      <span class="sortable-handle" cdkDragHandle aria-hidden="true" title="Glisser pour déplacer">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
          <circle cx="9" cy="6" r="1.5" />
          <circle cx="15" cy="6" r="1.5" />
          <circle cx="9" cy="12" r="1.5" />
          <circle cx="15" cy="12" r="1.5" />
          <circle cx="9" cy="18" r="1.5" />
          <circle cx="15" cy="18" r="1.5" />
        </svg>
      </span>
      <span class="text-sm text-ink-muted tabular-nums">{{ index() + 1 }} / {{ count() }}</span>
      <span class="sortable-actions">
        <button
          #up
          appButton
          type="button"
          variant="quiet"
          size="sm"
          [disabled]="index() === 0"
          (click)="move(-1)"
        >
          Monter<span class="sr-only"> {{ name() }}</span>
        </button>
        <button
          #down
          appButton
          type="button"
          variant="quiet"
          size="sm"
          [disabled]="index() === count() - 1"
          (click)="move(1)"
        >
          Descendre<span class="sr-only"> {{ name() }}</span>
        </button>
        <button appButton type="button" variant="quiet" size="sm" (click)="remove.emit()">
          Retirer<span class="sr-only"> {{ name() }}</span>
        </button>
      </span>
    </div>
    <div class="sortable-item-body">
      <ng-content />
    </div>
  `,
  styles: `
    :host {
      display: block;
      border: var(--border-rule) solid var(--color-rule);
      border-radius: var(--radius-control);
      background: var(--color-paper);
    }

    .sortable-item-bar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: calc(var(--spacing) * 2);
      padding: calc(var(--spacing) * 1) calc(var(--spacing) * 2);
      border-bottom: var(--border-rule) solid var(--color-rule);
      background: var(--color-paper-sunken);
    }

    .sortable-handle {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: calc(var(--spacing) * 8);
      min-height: calc(var(--spacing) * 8);
      color: var(--color-ink-muted);
      cursor: grab;
      touch-action: none;
    }

    .sortable-actions {
      display: flex;
      flex-wrap: wrap;
      gap: calc(var(--spacing) * 1);
      margin-inline-start: auto;
    }

    .sortable-item-body {
      display: grid;
      gap: calc(var(--spacing) * 5);
      padding: calc(var(--spacing) * 4);
    }

    /* Élément saisi à la souris : ombre de calque ; sa place reste marquée en pointillé */
    :host(.cdk-drag-preview) {
      box-shadow: var(--shadow-overlay);
    }

    :host(.cdk-drag-placeholder) {
      border-style: dashed;
      opacity: 0.6;
    }

    /* Mouvement du glisser-déposer (F26) : les voisins s'écartent et l'élément lâché rejoint sa
       place en 200 ms ; rien sous mouvement réduit (règle globale). Les déplacements au clavier,
       eux, sont immédiats. */
    :host-context(.cdk-drop-list-dragging),
    :host(.cdk-drag-animating) {
      transition: transform var(--duration-base) var(--ease-out);
    }

    :host(.cdk-drag-placeholder) {
      transition: none;
    }
  `,
})
export class SortableItem {
  /** Position de l'élément (base 0) et taille de la liste. */
  readonly index = input.required<number>();
  readonly count = input.required<number>();
  /** Désignation de l'élément, ajoutée au nom des boutons : « le lien « GitHub » ». */
  readonly name = input.required<string>();
  readonly moveTo = output<SortableMove>();
  readonly remove = output<void>();

  private readonly up = viewChild.required<ElementRef<HTMLButtonElement>>('up');
  private readonly down = viewChild.required<ElementRef<HTMLButtonElement>>('down');
  private readonly announcer = inject(LiveAnnouncer);
  private readonly injector = inject(Injector);
  private readonly last = computed(() => this.count() - 1);

  protected move(delta: -1 | 1): void {
    const from = this.index();
    const to = from + delta;
    this.moveTo.emit({ from, to });
    void this.announcer.announce(
      `${this.name()} déplacé en position ${to + 1} sur ${this.count()}.`,
    );
    afterNextRender(
      () => {
        // Le bouton utilisé reste focalisé, sauf s'il vient de se désactiver (extrémité atteinte)
        const towardsTop = delta < 0;
        const atEnd = towardsTop ? this.index() === 0 : this.index() === this.last();
        const target = towardsTop !== atEnd ? this.up() : this.down();
        target.nativeElement.focus();
      },
      { injector: this.injector },
    );
  }
}
