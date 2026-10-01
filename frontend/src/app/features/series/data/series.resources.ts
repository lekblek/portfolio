import { httpResource } from '@angular/common/http';

import { Series, SeriesSummary } from '../../../core/api/api-types';
import { Page, PageRequest, toHttpParams } from '../../../core/api/page';

/** Page de séries publiques (`GET /api/public/series`), relue à chaque changement de `?page=`. */
export function seriesListResource(query: () => PageRequest) {
  return httpResource<Page<SeriesSummary>>(() => ({
    url: '/api/public/series',
    params: toHttpParams(query()),
  }));
}

/** Série publique et ses chapitres visibles (`GET /api/public/series/{slug}`) ; 404 sinon. */
export function seriesResource(slug: () => string) {
  return httpResource<Series>(() => `/api/public/series/${encodeURIComponent(slug())}`);
}
