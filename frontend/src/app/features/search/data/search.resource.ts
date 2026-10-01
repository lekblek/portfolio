import { httpResource } from '@angular/common/http';

import { SearchResult } from '../../../core/api/api-types';
import { Page, toHttpParams } from '../../../core/api/page';

export interface SearchQuery {
  q: string;
  /** Page de l'URL, en base 1. */
  page: number;
}

/**
 * Résultats de la recherche publique (`GET /api/public/search`), relus à chaque changement de
 * `?q=` ou de `?page=`. Aucune requête sans texte à chercher (`null`). Contexte d'injection.
 */
export function searchResource(query: () => SearchQuery | null) {
  return httpResource<Page<SearchResult>>(() => {
    const current = query();
    return current
      ? { url: '/api/public/search', params: toHttpParams({ page: current.page, q: current.q }) }
      : undefined;
  });
}
