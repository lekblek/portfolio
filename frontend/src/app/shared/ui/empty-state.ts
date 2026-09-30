import { Component, input } from '@angular/core';

/**
 * État vide d'une liste ou d'une page : une phrase qui explique, puis la suite proposée
 * (lien projeté, « Voir tous les articles »). Docs : 02-design-system §19.
 */
@Component({
  selector: 'app-empty-state',
  template: `
    <div class="border-t border-rule pt-block">
      <p class="max-w-prose font-text text-lg leading-prose">{{ message() }}</p>
      <div class="mt-4 cluster-4">
        <ng-content />
      </div>
    </div>
  `,
})
export class EmptyState {
  readonly message = input.required<string>();
}
