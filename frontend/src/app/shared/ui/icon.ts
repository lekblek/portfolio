import { Component, computed, input } from '@angular/core';

import { ICONS, IconName } from './icons';

/**
 * Icône décorative en SVG, à la taille et à la couleur du texte voisin. Toujours accompagnée
 * d'un texte visible ou d'un nom accessible porté par le contrôle qui la contient.
 */
@Component({
  selector: 'app-icon',
  host: { 'aria-hidden': 'true' },
  template: `
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      focusable="false"
    >
      @for (path of paths(); track $index) {
        <path [attr.d]="path" />
      }
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      flex-shrink: 0;
      width: 1.25em;
      height: 1.25em;
    }

    svg {
      width: 100%;
      height: 100%;
    }
  `,
})
export class Icon {
  readonly name = input.required<IconName>();

  protected readonly paths = computed(() => ICONS[this.name()]);
}
