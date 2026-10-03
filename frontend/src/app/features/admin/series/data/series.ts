import { HttpClient, httpResource } from '@angular/common/http';
import { maxLength, pattern, required, SchemaPathTree } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';

import {
  AdminPublicationSummary,
  AdminSeries,
  AdminSeriesSummary,
  SaveSeriesRequest,
} from '../../../../core/api/api-types';
import { Page, PageRequest, toHttpParams } from '../../../../core/api/page';

const API = '/api/admin/series';

/** Bornes du contrat (`Series`, D-CV). */
export const TITLE_MAX = 160;
export const DESCRIPTION_MAX = 10_000;

const HAS_LETTER_OR_DIGIT = /[\p{L}\p{N}]/u;
const SLUG_FORMAT = /^[a-z0-9]+(-[a-z0-9]+)*$/;

/** Page des séries (20 par page), par titre (D-CV). Contexte d'injection. */
export function seriesPageResource(request: () => PageRequest) {
  return httpResource<Page<AdminSeriesSummary>>(() => ({
    url: API,
    params: toHttpParams(request()),
  }));
}

/** Une série à modifier, chapitres compris ; 404 si elle n'existe pas. Contexte d'injection. */
export function seriesResource(id: () => string | undefined) {
  return httpResource<AdminSeries>(() => {
    const current = id();
    return current === undefined ? undefined : `${API}/${current}`;
  });
}

/**
 * Articles qui peuvent devenir des chapitres (une actualité ne le peut pas, D-CV) : jusqu'à 100
 * (taille de page maximale), les derniers modifiés d'abord. Contexte d'injection.
 */
export function articlesResource() {
  return httpResource<Page<AdminPublicationSummary>>(() => ({
    url: '/api/admin/publications',
    params: toHttpParams({ type: 'ARTICLE', size: 100 }),
  }));
}

export function createSeries(http: HttpClient, request: SaveSeriesRequest): Promise<AdminSeries> {
  return firstValueFrom(http.post<AdminSeries>(API, request));
}

/** Titre, slug, description, couverture ; les chapitres ne changent pas. */
export function updateSeries(
  http: HttpClient,
  id: number,
  request: SaveSeriesRequest,
): Promise<AdminSeries> {
  return firstValueFrom(http.put<AdminSeries>(`${API}/${id}`, request));
}

/**
 * Chapitres remplacés d'un bloc, dans l'ordre de lecture ; 409 `ARTICLE_ALREADY_IN_SERIES` si un
 * article appartient à une autre série.
 */
export function replaceChapters(
  http: HttpClient,
  id: number,
  publicationIds: readonly number[],
): Promise<AdminSeries> {
  return firstValueFrom(http.put<AdminSeries>(`${API}/${id}/chapters`, { publicationIds }));
}

/** Saisie d'une série (hors chapitres). */
export interface SeriesModel {
  title: string;
  slug: string;
  descriptionMarkdown: string;
  coverMediaId: number | null;
}

export const EMPTY_SERIES: SeriesModel = {
  title: '',
  slug: '',
  descriptionMarkdown: '',
  coverMediaId: null,
};

export function toModel(series: AdminSeries): SeriesModel {
  return {
    title: series.title,
    slug: series.slug,
    descriptionMarkdown: series.descriptionMarkdown,
    coverMediaId: series.coverMediaId,
  };
}

/** Saisie → requête : slug envoyé seulement s'il a changé ; couverture absente : aucune. */
export function toRequest(model: SeriesModel, initialSlug: string): SaveSeriesRequest {
  const slug = model.slug.trim();
  return {
    title: model.title.trim(),
    slug: slug !== '' && slug !== initialSlug ? slug : undefined,
    descriptionMarkdown: model.descriptionMarkdown,
    coverMediaId: model.coverMediaId ?? undefined,
  };
}

/** Règles de la saisie, alignées sur `SaveSeriesRequest` (D-CV). */
export function seriesSchema(path: SchemaPathTree<SeriesModel>): void {
  required(path.title, { message: 'Indiquez le titre.' });
  pattern(path.title, HAS_LETTER_OR_DIGIT, {
    message: 'Le titre doit contenir au moins une lettre ou un chiffre.',
  });
  maxLength(path.title, TITLE_MAX, { message: `${TITLE_MAX} caractères au plus.` });
  pattern(path.slug, SLUG_FORMAT, {
    message:
      'Lettres minuscules sans accent, chiffres et tirets seulement, par exemple « ma-serie ».',
  });
  maxLength(path.slug, TITLE_MAX, { message: `${TITLE_MAX} caractères au plus.` });
  maxLength(path.descriptionMarkdown, DESCRIPTION_MAX, {
    message: '10 000 caractères au plus.',
  });
}
