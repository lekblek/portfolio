import { FieldTree } from '@angular/forms/signals';

/**
 * Message à afficher sous un champ (`app-field`, entrée `error`) : sa première erreur, une fois le
 * champ quitté ou le formulaire soumis ; `null` avant, ou si le champ est valide.
 */
export function visibleError(field: FieldTree<unknown>): string | null {
  const state = field();
  return state.touched() ? (state.errors()[0]?.message ?? null) : null;
}
