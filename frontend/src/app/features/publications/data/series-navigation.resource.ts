import { isPlatformServer } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { effect, inject, makeStateKey, PLATFORM_ID, TransferState } from '@angular/core';

import { toApiError } from '../../../core/api/api-error';
import { SeriesNavigation } from '../../../core/api/api-types';

const absent = (slug: string) => makeStateKey<boolean>(`series-navigation-absent:${slug}`);

/**
 * Place d'un article dans sa série (`GET /api/public/publications/{slug}/series`). L'API répond
 * 404 pour un article hors série (D-BL) ; le cache de transfert ne transmet pas les erreurs, donc
 * le rendu serveur note cette absence dans `TransferState` et le navigateur ne repose pas la
 * question au premier affichage. `slug` nul : aucune requête (une actualité n'entre dans aucune
 * série). Contexte d'injection.
 */
export function seriesNavigationResource(slug: () => string | null) {
  const transferState = inject(TransferState);
  const resource = httpResource<SeriesNavigation>(() => {
    const current = slug();
    return current === null || transferState.get(absent(current), false)
      ? undefined
      : `/api/public/publications/${encodeURIComponent(current)}/series`;
  });
  if (isPlatformServer(inject(PLATFORM_ID))) {
    effect(() => {
      const current = slug();
      if (current !== null && resource.error() && toApiError(resource.error()).status === 404) {
        transferState.set(absent(current), true);
      }
    });
  }
  return resource;
}
