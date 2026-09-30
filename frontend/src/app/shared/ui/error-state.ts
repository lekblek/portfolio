import { Component, input, output } from '@angular/core';

import { Button } from './button';

/**
 * Erreur de chargement d'une page ou d'un bloc : ce qui s'est passé (texte de l'API si
 * pertinent, D-DJ) et l'action « Réessayer ». Annoncée dès son apparition (`role="alert"`).
 */
@Component({
  selector: 'app-error-state',
  imports: [Button],
  template: `
    <div role="alert" class="error-state pt-block">
      <p class="font-semibold">{{ title() }}</p>
      @if (detail()) {
        <p class="mt-2 max-w-prose font-text text-lg leading-prose">{{ detail() }}</p>
      }
      <button appButton type="button" variant="secondary" class="mt-4" (click)="retry.emit()">
        Réessayer
      </button>
    </div>
  `,
  styles: `
    .error-state {
      border-top: var(--border-strong) solid var(--color-danger);
    }
  `,
})
export class ErrorState {
  readonly title = input('Le contenu n’a pas pu être chargé.');
  readonly detail = input<string | null>(null);
  readonly retry = output<void>();
}
