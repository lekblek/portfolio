import { HttpParams } from '@angular/common/http';

import { Schemas } from './api-types';

type PageMetadata = Omit<Schemas['PageResponseProjectSummaryResponse'], 'content'>;

/**
 * Page de résultats (`PageResponse`, 05-conventions-api §14). Métadonnées reprises du contrat, pour
 * suivre sa dérive. `page` est en base 0, comme l'API.
 */
export type Page<T> = PageMetadata & { content: T[] };

/**
 * Page demandée par l'URL (`?page=`, base 1) et filtres de la liste.
 * Un filtre absent ou vide n'est pas transmis.
 */
export type PageRequest = { page?: number } & Record<string, string | number | null | undefined>;

/** Paramètres de l'API : page de l'URL (base 1) → page de l'API (base 0) ; page invalide → première. */
export function toHttpParams({ page, ...filters }: PageRequest): HttpParams {
  const apiPage = page !== undefined && Number.isInteger(page) && page >= 1 ? page - 1 : 0;
  let params = new HttpParams().set('page', apiPage);
  for (const [name, value] of Object.entries(filters)) {
    if (value !== null && value !== undefined && value !== '') {
      params = params.set(name, value);
    }
  }
  return params;
}
