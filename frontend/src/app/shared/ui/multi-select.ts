import { Combobox, ComboboxPopup, ComboboxWidget } from '@angular/aria/combobox';
import { Listbox, Option } from '@angular/aria/listbox';
import {
  afterNextRender,
  afterRenderEffect,
  Component,
  computed,
  ElementRef,
  inject,
  Injector,
  input,
  model,
  signal,
  viewChild,
} from '@angular/core';
import { FormValueControl } from '@angular/forms/signals';

import { Button } from './button';
import { FieldControl } from './field';

/** Choix proposé : valeur enregistrée et libellé affiché. */
export interface MultiSelectOption<V> {
  value: V;
  label: string;
}

/** Minuscules sans accents, pour filtrer « Securite » comme « Sécurité ». */
function folded(text: string): string {
  return text.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
}

/**
 * Choix multiple (02-design-system §18, motif combobox de l'APG) : champ de saisie qui filtre la
 * liste (combobox Angular Aria), liste à choix multiples (`aria-multiselectable`, coche visible),
 * puis les choix retenus en éléments retirables. Clavier : la saisie ou `Alt + ↓` ouvre la liste,
 * `↑` `↓` parcourent, `Entrée` coche ou décoche, `Échap` ferme ; chaque changement est annoncé.
 * Placé dans un `app-field` (libellé, aide, erreur) ; contrôle de Signal Forms (`[formField]`).
 */
@Component({
  selector: 'app-multi-select',
  imports: [Button, Combobox, ComboboxPopup, ComboboxWidget, FieldControl, Listbox, Option],
  template: `
    <div class="multi-select">
      <input
        #combobox="ngCombobox"
        appFieldControl
        ngCombobox
        type="text"
        autocomplete="off"
        spellcheck="false"
        [placeholder]="placeholder()"
        [(value)]="query"
        [(expanded)]="expanded"
        (click)="expanded.set(true)"
        (blur)="touched.set(true)"
      />
      <ng-template ngComboboxPopup [combobox]="combobox">
        <div class="multi-select-popup">
          @if (filtered().length === 0) {
            <p class="multi-select-empty">
              Aucun choix ne correspond à «&#160;{{ query() }}&#160;».
            </p>
          }
          <div
            #listbox="ngListbox"
            ngListbox
            ngComboboxWidget
            multi
            class="multi-select-list"
            focusMode="activedescendant"
            selectionMode="explicit"
            [attr.aria-label]="listLabel()"
            [tabindex]="-1"
            [activeDescendant]="listbox.activeDescendant()"
            [value]="visibleValue()"
            (valueChange)="select($event)"
          >
            @for (option of filtered(); track option.value) {
              <div
                ngOption
                class="multi-select-option"
                [value]="option.value"
                [label]="option.label"
              >
                <span class="multi-select-check" aria-hidden="true"></span>
                <span>{{ option.label }}</span>
              </div>
            }
          </div>
        </div>
      </ng-template>
    </div>
    <p class="sr-only" aria-live="polite">{{ announcement() }}</p>
    @if (chosen().length > 0) {
      <ul class="multi-select-chosen" [attr.aria-label]="chosenLabel()">
        @for (option of chosen(); track option.value; let i = $index) {
          <li class="multi-select-chip">
            <span>{{ option.label }}</span>
            <button
              appButton
              type="button"
              variant="quiet"
              size="sm"
              [attr.data-chip]="i"
              (click)="remove(option.value, i)"
            >
              Retirer<span class="sr-only"> {{ option.label }}</span>
            </button>
          </li>
        }
      </ul>
    }
  `,
  styles: `
    .multi-select {
      position: relative;
    }

    .multi-select-popup {
      position: absolute;
      z-index: var(--z-dropdown);
      inset-inline: 0;
      max-height: calc(var(--spacing) * 72);
      margin-block-start: calc(var(--spacing) * 1);
      overflow-y: auto;
      overscroll-behavior: contain;
      border: var(--border-strong) solid var(--color-ink);
      border-radius: var(--radius-control);
      background: var(--color-paper);
      box-shadow: var(--shadow-overlay);
    }

    .multi-select-empty {
      padding: calc(var(--spacing) * 3);
      color: var(--color-ink-muted);
    }

    .multi-select-option {
      display: flex;
      align-items: center;
      gap: calc(var(--spacing) * 3);
      min-height: calc(var(--spacing) * 11);
      padding-inline: calc(var(--spacing) * 3);
      cursor: pointer;
    }

    .multi-select-option[data-active='true'] {
      outline: var(--border-strong) solid var(--color-focus);
      outline-offset: calc(var(--border-strong) * -1);
      background: var(--color-paper-sunken);
    }

    .multi-select-check {
      flex: none;
      width: calc(var(--spacing) * 4);
      height: calc(var(--spacing) * 4);
      border: var(--border-strong) solid var(--color-ink);
      border-radius: var(--radius-control);
    }

    .multi-select-option[aria-selected='true'] .multi-select-check {
      border-color: var(--color-accent);
      background: var(--color-accent);
      box-shadow: inset 0 0 0 calc(var(--spacing) * 0.75) var(--color-paper);
    }

    .multi-select-chosen {
      display: flex;
      flex-wrap: wrap;
      gap: calc(var(--spacing) * 2);
      margin-block-start: calc(var(--spacing) * 3);
    }

    .multi-select-chip {
      display: inline-flex;
      align-items: center;
      gap: calc(var(--spacing) * 1);
      padding-inline-start: calc(var(--spacing) * 3);
      border: var(--border-rule) solid var(--color-rule);
      border-radius: var(--radius-control);
      background: var(--color-paper-sunken);
    }
  `,
})
export class MultiSelect<V> implements FormValueControl<V[]> {
  /** Valeurs choisies, dans l'ordre des choix proposés. */
  readonly value = model<V[]>([]);
  readonly touched = model(false);
  readonly options = input.required<readonly MultiSelectOption<V>[]>();
  /** Nom de la liste des propositions et de la liste des choix : « Technologies ». */
  readonly listLabel = input.required<string>();
  readonly placeholder = input('Rechercher…');

  protected readonly query = signal('');
  protected readonly expanded = signal(false);
  protected readonly announcement = signal('');
  protected readonly filtered = computed(() => {
    const query = folded(this.query().trim());
    return this.options().filter((option) => folded(option.label).includes(query));
  });
  /** Choix parmi les propositions affichées : la liste filtrée ne connaît que celles-là. */
  protected readonly visibleValue = computed(() => {
    const visible = new Set(this.filtered().map((option) => option.value));
    return this.value().filter((value) => visible.has(value));
  });
  protected readonly chosen = computed(() => {
    const chosen = new Set(this.value());
    return this.options().filter((option) => chosen.has(option.value));
  });
  protected readonly chosenLabel = computed(() => `${this.listLabel()} choisies`);

  private readonly listbox = viewChild(Listbox);
  private readonly combobox = viewChild.required(Combobox);
  private readonly host: HTMLElement = inject(ElementRef).nativeElement;
  private readonly injector = inject(Injector);

  constructor() {
    afterRenderEffect(() => {
      if (this.expanded()) {
        this.listbox()?.scrollActiveItemIntoView();
      }
    });
  }

  /**
   * Nouveau choix dans la liste filtrée : les choix masqués par le filtre sont conservés, l'ordre
   * suit celui des propositions.
   */
  protected select(visibleValues: V[]): void {
    const visible = new Set(this.filtered().map((option) => option.value));
    const chosen = new Set([
      ...this.value().filter((value) => !visible.has(value)),
      ...visibleValues,
    ]);
    const next = this.options()
      .map((option) => option.value)
      .filter((value) => chosen.has(value));
    this.value.set(next);
    this.touched.set(true);
    const count = next.length;
    this.announcement.set(`${count} ${count > 1 ? 'choix retenus' : 'choix retenu'}.`);
  }

  protected remove(value: V, index: number): void {
    const label = this.options().find((option) => option.value === value)?.label ?? '';
    this.value.update((values) => values.filter((current) => current !== value));
    this.touched.set(true);
    this.announcement.set(`${label} retiré.`);
    afterNextRender(
      () => {
        const chips = this.host.querySelectorAll<HTMLElement>('[data-chip]');
        (chips[Math.min(index, chips.length - 1)] ?? this.combobox().element).focus();
      },
      { injector: this.injector },
    );
  }
}
