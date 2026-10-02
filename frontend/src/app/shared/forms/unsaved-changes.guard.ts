import { CanDeactivateFn } from '@angular/router';

/**
 * Page de formulaire qui peut retenir la navigation : elle sait si sa saisie est enregistrée et
 * demande confirmation sinon (dialogue de la page, 02-design-system §18).
 */
export interface UnsavedChanges {
  /** Vrai si l'on peut quitter la page : saisie enregistrée, inchangée, ou abandon confirmé. */
  canLeave(): boolean | Promise<boolean>;
}

/**
 * Garde des formulaires d'administration (01-architecture §10) : une navigation dans l'application
 * attend la réponse de la page. Fermer l'onglet ou recharger relève de l'avertissement natif du
 * navigateur, que la page déclenche elle-même (`beforeunload`).
 */
export const unsavedChangesGuard: CanDeactivateFn<UnsavedChanges> = (page) => page.canLeave();
