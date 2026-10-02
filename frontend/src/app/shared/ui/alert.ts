import { Component, computed, ElementRef, inject, input } from '@angular/core';

import { Icon } from './icon';
import { IconName } from './icons';

export type AlertTone = 'success' | 'danger';

const ICON: Record<AlertTone, IconName> = {
  success: 'circle-check',
  danger: 'circle-alert',
};

/**
 * Message d'issue d'une action (02-design-system §18) : envoi réussi, refus, échec. Titre et texte
 * projeté ; la couleur n'est jamais seule (icône et titre). Un échec est annoncé dès son apparition
 * (`role="alert"`) ; un succès reçoit le focus (`focus()`), ce qui l'annonce et place le lecteur
 * au bon endroit quand le formulaire disparaît. Apparition : fondu et montée de 4 px en 200 ms
 * (`@starting-style`), absente sous mouvement réduit ; un message ne fait jamais partie du rendu
 * serveur, sans quoi il entrerait au chargement de la page (02-design-system §11).
 */
@Component({
  selector: 'app-alert',
  imports: [Icon],
  host: {
    class: 'alert',
    tabindex: '-1',
    '[class.alert-success]': "tone() === 'success'",
    '[class.alert-danger]': "tone() === 'danger'",
    '[attr.role]': "tone() === 'danger' ? 'alert' : null",
  },
  template: `
    <app-icon [name]="icon()" class="alert-icon" />
    <div class="min-w-0">
      <p class="font-semibold">{{ title() }}</p>
      <div class="mt-1 max-w-prose font-text leading-prose">
        <ng-content />
      </div>
    </div>
  `,
  styles: `
    :host {
      display: flex;
      gap: calc(var(--spacing) * 3);
      padding: calc(var(--spacing) * 4);
      border-inline-start: var(--border-strong) solid currentColor;
      background: var(--color-paper-sunken);
    }

    :host(.alert-success) {
      border-inline-start-color: var(--color-success);
    }

    :host(.alert-danger) {
      border-inline-start-color: var(--color-danger);
    }

    .alert-icon {
      margin-block-start: 0.125rem;
    }

    :host(.alert-success) .alert-icon {
      color: var(--color-success);
    }

    :host(.alert-danger) .alert-icon {
      color: var(--color-danger);
    }

    /* Le message monte de 4 px en apparaissant, 200 ms (F20) */
    @media (prefers-reduced-motion: no-preference) {
      :host {
        transition:
          opacity var(--duration-base) var(--ease-out),
          transform var(--duration-base) var(--ease-out);

        @starting-style {
          opacity: 0;
          transform: translateY(calc(var(--spacing) * 1));
        }
      }
    }
  `,
})
export class Alert {
  readonly tone = input.required<AlertTone>();
  readonly title = input.required<string>();

  protected readonly icon = computed(() => ICON[this.tone()]);

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  focus(): void {
    this.host.nativeElement.focus();
  }
}
