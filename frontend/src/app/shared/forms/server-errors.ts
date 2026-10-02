import { ValidationError } from '@angular/forms/signals';

import { toApiError } from '../../core/api/api-error';

type FieldRef = ValidationError.WithFieldTree['fieldTree'];

/** Conflit (409) propre à un champ : le code stable de l'API et le message à afficher sous ce champ. */
export interface FieldConflict<K extends string> {
  field: K;
  message: string;
}

/**
 * Erreurs d'un envoi refusé par l'API, rattachées aux champs du formulaire (01-architecture §10) :
 * erreurs de champ d'une 400 `VALIDATION_FAILED` dont le champ est connu, et conflits (409) dont le
 * code désigne un champ (`NAME_ALREADY_USED` → le nom). À retourner par l'action de soumission des
 * Signal Forms. Liste vide : l'erreur ne concerne aucun champ, la page l'affiche en tête du
 * formulaire.
 */
export function serverFieldErrors<K extends string>(
  error: unknown,
  fields: Record<K, FieldRef>,
  conflicts: Partial<Record<string, FieldConflict<NoInfer<K>>>> = {},
): ValidationError.WithFieldTree[] {
  const failure = toApiError(error);
  const known = (field: string): field is K => Object.hasOwn(fields, field);
  if (failure.code === 'VALIDATION_FAILED') {
    return failure.fieldErrors.flatMap(({ field, message }) =>
      known(field) ? [{ fieldTree: fields[field], kind: 'server', message }] : [],
    );
  }
  const conflict = failure.code === null ? undefined : conflicts[failure.code];
  return conflict
    ? [{ fieldTree: fields[conflict.field], kind: 'server', message: conflict.message }]
    : [];
}
