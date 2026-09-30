import { inject, RESPONSE_INIT } from '@angular/core';

/**
 * Retourne une fonction qui pose le statut HTTP de la réponse du rendu serveur (404 d'une page
 * introuvable). Dans le navigateur, `RESPONSE_INIT` vaut `null` : l'appel est sans effet.
 * À appeler dans un contexte d'injection ; la fonction retournée peut l'être plus tard.
 */
export function injectResponseStatus(): (status: number) => void {
  const responseInit = inject(RESPONSE_INIT);
  return (status) => {
    if (responseInit !== null) {
      responseInit.status = status;
    }
  };
}
