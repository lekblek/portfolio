import { httpResource } from '@angular/common/http';

import { Publication, PublicationSummary } from '../../../core/api/api-types';
import { Page, PageRequest, toHttpParams } from '../../../core/api/page';

/**
 * Page de publications visibles d'un type (`GET /api/public/publications?type=`), relue à chaque
 * changement de page ou de filtre (`?page=`, `?category=`, `?tag=`). Contexte d'injection.
 */
export function publicationListResource(query: () => PageRequest) {
  return httpResource<Page<PublicationSummary>>(() => ({
    url: '/api/public/publications',
    params: toHttpParams(query()),
  }));
}

/** Publication visible par son slug (`GET /api/public/publications/{slug}`) ; 404 sinon. */
export function publicationResource(slug: () => string) {
  return httpResource<Publication>(() => `/api/public/publications/${encodeURIComponent(slug())}`);
}
