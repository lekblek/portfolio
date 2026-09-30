import { Component } from '@angular/core';

import { injectResponseStatus } from '../../core/platform/response-status';

/** Page introuvable minimale : statut 404 côté serveur ; mise en forme avec le shell public. */
@Component({
  selector: 'app-not-found',
  template: `
    <main>
      <h1>Page introuvable</h1>
      <p>L’adresse demandée ne correspond à aucune page du site.</p>
    </main>
  `,
})
export class NotFound {
  constructor() {
    injectResponseStatus()(404);
  }
}
