import { Component } from '@angular/core';

import { injectResponseStatus } from '../../core/platform/response-status';

/**
 * Page introuvable, avec le statut HTTP 404 côté serveur. Les liens de sortie viendront avec
 * les pages publiques (accueil, articles) : aucun lien vers une page qui n'existe pas encore.
 */
@Component({
  selector: 'app-not-found',
  template: `
    <div class="page-container grid gap-3 py-section lg:grid-cols-12 lg:gap-8">
      <p class="text-sm font-semibold text-ink-muted lg:col-span-3 lg:pt-3">Erreur 404</p>
      <div class="lg:col-span-9">
        <h1 class="text-3xl leading-tight tracking-title">Page introuvable</h1>
        <p class="mt-flow max-w-prose font-text text-lg leading-prose">
          L’adresse demandée ne correspond à aucune page du site. Vérifiez qu’elle est correctement
          saisie.
        </p>
      </div>
    </div>
  `,
})
export class NotFound {
  constructor() {
    injectResponseStatus()(404);
  }
}
