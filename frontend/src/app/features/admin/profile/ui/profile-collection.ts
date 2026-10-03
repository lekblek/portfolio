import { LiveAnnouncer } from '@angular/cdk/a11y';
import { NgTemplateOutlet } from '@angular/common';
import {
  afterNextRender,
  Component,
  computed,
  contentChild,
  Directive,
  ElementRef,
  inject,
  Injector,
  input,
  TemplateRef,
} from '@angular/core';
import { FieldTree } from '@angular/forms/signals';

import { Button } from '../../../../shared/ui/button';
import { Icon } from '../../../../shared/ui/icon';
import {
  moveItem,
  SortableItem,
  SortableList,
  SortableMove,
} from '../../../../shared/ui/sortable-list';

/** Contexte du gabarit d'un élément : son arbre de champs et sa position. */
export interface CollectionItemContext<T extends object> {
  $implicit: FieldTree<T>;
  index: number;
}

/**
 * Gabarit des champs d'un élément de collection : `<ng-template [appCollectionItem]="form.links"
 * let-link let-i="index">`. L'entrée ne sert qu'à typer le contexte.
 */
@Directive({ selector: 'ng-template[appCollectionItem]' })
export class CollectionItem<T extends object> {
  readonly appCollectionItem = input.required<FieldTree<T[]>>();
  readonly template = inject<TemplateRef<CollectionItemContext<T>>>(TemplateRef);

  static ngTemplateContextGuard<T extends object>(
    _directive: CollectionItem<T>,
    // Paramètre imposé par la signature du garde de contexte, utilisé seulement dans le type
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    context: unknown,
  ): context is CollectionItemContext<T> {
    return true;
  }
}

let nextId = 0;

/**
 * Collection du profil (liens, compétences, expériences, formations, certifications) : titre,
 * explication, liste ordonnable des éléments (l'ordre saisi est l'ordre d'affichage du site, D-CY),
 * ajout et retrait. Le modèle est modifié par l'arbre de champs (`items`). Un élément ajouté reçoit
 * le focus sur son premier champ ; après un retrait, le focus passe au suivant, au précédent, ou au
 * bouton d'ajout ; chaque changement est annoncé. Une erreur portant sur toute la liste s'affiche sous
 * son titre et reçoit le focus après un envoi refusé.
 */
@Component({
  selector: 'app-profile-collection',
  imports: [Button, Icon, NgTemplateOutlet, SortableItem, SortableList],
  template: `
    <section class="profile-section" [attr.aria-labelledby]="headingId">
      <h2 class="text-xl tracking-heading" [id]="headingId">{{ title() }}</h2>
      @if (description()) {
        <p class="mt-1 max-w-prose text-sm text-ink-muted">{{ description() }}</p>
      }
      @if (error(); as message) {
        <p class="collection-error" tabindex="-1" data-invalid="true">
          <app-icon name="circle-alert" class="mt-0.5" />
          <span>{{ message }}</span>
        </p>
      }
      @if (count() === 0) {
        <p class="mt-4 text-ink-muted">{{ emptyText() }}</p>
      } @else {
        <app-sortable-list class="mt-4" [label]="title()" (moved)="move($event)">
          @for (item of items(); track item; let i = $index) {
            <app-sortable-item
              [index]="i"
              [count]="count()"
              [name]="itemName()(item().value(), i)"
              (moveTo)="move($event)"
              (remove)="remove(i)"
            >
              @if (itemTemplate(); as template) {
                <ng-container *ngTemplateOutlet="template.template; context: contextOf(i)" />
              }
            </app-sortable-item>
          }
        </app-sortable-list>
      }
      <button appButton type="button" variant="secondary" class="mt-4" (click)="append()">
        {{ addLabel() }}
      </button>
    </section>
  `,
  styles: `
    .collection-error {
      display: flex;
      gap: calc(var(--spacing) * 2);
      margin-block-start: calc(var(--spacing) * 3);
      color: var(--color-danger);
      font-weight: var(--font-weight-medium);
    }
  `,
})
export class ProfileCollection<T extends object> {
  readonly items = input.required<FieldTree<T[]>>();
  readonly title = input.required<string>();
  readonly description = input<string | null>(null);
  readonly addLabel = input.required<string>();
  readonly emptyText = input.required<string>();
  /** Désignation d'un élément pour les boutons et les annonces : « le lien « GitHub » ». */
  readonly itemName = input.required<(value: T, index: number) => string>();
  /** Élément vide ajouté par le bouton. */
  readonly create = input.required<() => T>();

  protected readonly headingId = `profil-collection-${++nextId}`;
  protected readonly itemTemplate = contentChild<CollectionItem<T>>(CollectionItem);
  protected readonly count = computed(() => this.items()().value().length);
  /** Erreur de la liste elle-même, renvoyée par le serveur (`skills` : compétence en double). */
  protected readonly error = computed(() => this.items()().errors()[0]?.message ?? null);

  private readonly host: HTMLElement = inject(ElementRef).nativeElement;
  private readonly announcer = inject(LiveAnnouncer);
  private readonly injector = inject(Injector);

  /** Contexte du gabarit : le typage générique des arbres de champs ne se résout pas ici. */
  protected contextOf(index: number): CollectionItemContext<T> {
    return { $implicit: this.items()[index] as FieldTree<T>, index };
  }

  protected move(move: SortableMove): void {
    this.items()().value.update((list) => moveItem(list, move));
  }

  protected append(): void {
    this.items()().value.update((list) => [...list, this.create()()]);
    this.focusItem(this.count() - 1);
  }

  protected remove(index: number): void {
    const name = this.itemName()(this.items()().value()[index], index);
    this.items()().value.update((list) => list.filter((_, position) => position !== index));
    void this.announcer.announce(
      `${name} retiré de la liste. Enregistrez le profil pour le retirer du site.`,
    );
    this.focusItem(Math.min(index, this.count() - 1));
  }

  /** Premier champ de l'élément `index` après le rendu ; sans élément, le bouton d'ajout. */
  private focusItem(index: number): void {
    afterNextRender(
      () => {
        const items = this.host.querySelectorAll('app-sortable-item');
        const target =
          index >= 0 ? items[index]?.querySelector<HTMLElement>('input, textarea, select') : null;
        (target ?? this.host.querySelector<HTMLElement>('section > button'))?.focus();
      },
      { injector: this.injector },
    );
  }
}
