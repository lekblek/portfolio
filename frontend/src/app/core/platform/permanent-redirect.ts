import { inject, RESPONSE_INIT } from '@angular/core';
import { Router } from '@angular/router';

/**
 * Retourne une fonction qui mène à l'adresse canonique d'un contenu ouvert sous une autre adresse.
 * Rendu serveur : statut 301 posé avant la navigation ; `@angular/ssr` constate que l'adresse
 * finale diffère de l'adresse demandée et répond par une redirection vers elle, avec ce statut.
 * Navigateur : l'adresse est remplacée dans l'historique. À appeler dans un contexte d'injection.
 */
export function injectPermanentRedirect(): (url: string) => void {
  const responseInit = inject(RESPONSE_INIT);
  const router = inject(Router);
  return (url) => {
    if (responseInit !== null) {
      responseInit.status = 301;
    }
    void router.navigateByUrl(url, { replaceUrl: true });
  };
}
