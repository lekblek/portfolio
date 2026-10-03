import { HttpClient, httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import {
  AdminCategory,
  AdminPublication,
  AdminPublicationSummary,
  AdminTag,
  CreatePublicationRequest,
  PublicationStatus,
  PublicationType,
  UpdatePublicationRequest,
} from '../../../../core/api/api-types';
import { Page, toHttpParams } from '../../../../core/api/page';

const API = '/api/admin/publications';

/** Filtres de la liste, lus dans l'URL : type et statut observable (absents : tous). */
export interface PublicationListRequest {
  page: number;
  type?: PublicationType;
  status?: PublicationStatus;
}

export const TYPE_FILTERS: readonly { value: PublicationType; label: string }[] = [
  { value: 'ARTICLE', label: 'Articles' },
  { value: 'NEWS', label: 'Actualités' },
];

/** Statuts dans l'ordre du cycle éditorial (D-AV), avec leur libellé de filtre. */
export const STATUS_FILTERS: readonly { value: PublicationStatus; label: string }[] = [
  { value: 'DRAFT', label: 'Brouillons' },
  { value: 'IN_REVIEW', label: 'En relecture' },
  { value: 'SCHEDULED', label: 'Programmées' },
  { value: 'PUBLISHED', label: 'Publiées' },
  { value: 'ARCHIVED', label: 'Archivées' },
];

export const TYPE_LABELS: Record<PublicationType, string> = {
  ARTICLE: 'Article',
  NEWS: 'Actualité',
};

/** Adresse publique d'une publication : `/articles/…` ou `/news/…`. */
export function publicPath(type: PublicationType, slug: string): string {
  return `${type === 'ARTICLE' ? '/articles' : '/news'}/${slug}`;
}

/** Page des publications (20 par page), les dernières modifiées d'abord (D-CU). Contexte d'injection. */
export function publicationPageResource(request: () => PublicationListRequest) {
  return httpResource<Page<AdminPublicationSummary>>(() => ({
    url: API,
    params: toHttpParams({ ...request() }),
  }));
}

/** Une publication à modifier ; 404 si elle n'existe pas. Contexte d'injection. */
export function publicationResource(id: () => string | undefined) {
  return httpResource<AdminPublication>(() => {
    const current = id();
    return current === undefined ? undefined : `${API}/${current}`;
  });
}

/** Catégories et tags, vocabulaires complets (D-CS). Contexte d'injection. */
export function categoriesResource() {
  return httpResource<AdminCategory[]>(() => '/api/admin/categories');
}

export function tagsResource() {
  return httpResource<AdminTag[]>(() => '/api/admin/tags');
}

/** Création (201) : toujours un brouillon ; slug généré depuis le titre ou suffixé (D-BD). */
export function createPublication(
  http: HttpClient,
  request: CreatePublicationRequest,
): Promise<AdminPublication> {
  return firstValueFrom(http.post<AdminPublication>(API, request));
}

/** Remplacement de la saisie (statut exclu) ; 409 `SLUG_LOCKED` après la première publication. */
export function updatePublication(
  http: HttpClient,
  id: number,
  request: UpdatePublicationRequest,
): Promise<AdminPublication> {
  return firstValueFrom(http.put<AdminPublication>(`${API}/${id}`, request));
}

/** Changement de statut (§17) ; 409 `INVALID_PUBLICATION_TRANSITION` si la transition est refusée. */
export function changePublicationStatus(
  http: HttpClient,
  id: number,
  status: PublicationStatus,
  publishedAt?: string,
): Promise<AdminPublication> {
  return firstValueFrom(
    http.post<AdminPublication>(`${API}/${id}/status`, { status, publishedAt }),
  );
}
