import { HttpClient, httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import {
  AdminCategory,
  AdminTag,
  AdminTechnology,
  SaveCategoryRequest,
  SaveTagRequest,
  SaveTechnologyRequest,
} from '../../../../core/api/api-types';
import { Vocabulary } from '../vocabularies';

/** Terme d'un des trois vocabulaires, champs propres compris quand il les a. */
export type Term = AdminCategory | AdminTag | AdminTechnology;

/** Saisie envoyée : nom, slug facultatif, et les champs propres au vocabulaire. */
export type TermRequest = SaveCategoryRequest | SaveTagRequest | SaveTechnologyRequest;

/**
 * Vocabulaire complet, non paginé (court, D-CS), dans l'ordre de l'API : par nom, ou par ordre
 * d'affichage pour les technologies (D-CW). Aucune requête tant que `vocabulary` rend `undefined`.
 * À appeler dans un contexte d'injection.
 */
export function termsResource(vocabulary: () => Vocabulary | undefined) {
  return httpResource<Term[]>(() => vocabulary()?.api);
}

/** Création (201) : le terme enregistré, slug généré ou suffixé (D-BD). */
export function createTerm(
  http: HttpClient,
  vocabulary: Vocabulary,
  request: TermRequest,
): Promise<Term> {
  return firstValueFrom(http.post<Term>(vocabulary.api, request));
}

/** Modification : saisie remplacée, slug conservé s'il est absent. */
export function updateTerm(
  http: HttpClient,
  vocabulary: Vocabulary,
  id: number,
  request: TermRequest,
): Promise<Term> {
  return firstValueFrom(http.put<Term>(`${vocabulary.api}/${id}`, request));
}

/** Suppression (204) ; 409 `TERM_STILL_USED` si un contenu l'utilise encore. */
export async function deleteTerm(
  http: HttpClient,
  vocabulary: Vocabulary,
  id: number,
): Promise<void> {
  await firstValueFrom(http.delete<void>(`${vocabulary.api}/${id}`));
}

/** Description d'une catégorie ; texte vide pour un autre terme ou une catégorie sans description. */
export function descriptionOf(term: Term): string {
  return isCategory(term) ? (term.description ?? '') : '';
}

/** Ordre d'affichage d'une technologie ; `null` pour un autre terme. */
export function displayOrderOf(term: Term): number | null {
  return isTechnology(term) ? term.displayOrder : null;
}

function isCategory(term: Term): term is AdminCategory {
  return 'description' in term;
}

function isTechnology(term: Term): term is AdminTechnology {
  return 'displayOrder' in term;
}
