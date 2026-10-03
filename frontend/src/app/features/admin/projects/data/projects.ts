import { HttpClient, httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import {
  AdminProject,
  AdminProjectSummary,
  AdminTechnology,
  ProjectVisibility,
  SaveProjectRequest,
} from '../../../../core/api/api-types';
import { Page, toHttpParams } from '../../../../core/api/page';

const API = '/api/admin/projects';

/** Filtres de la liste, lus dans l'URL : visibilité et slug d'une technologie (absents : tous). */
export interface ProjectListRequest {
  page: number;
  visibility?: ProjectVisibility;
  technology?: string;
}

/** Visibilités dans l'ordre du cycle de vie, avec leur libellé de filtre (pluriel). */
export const VISIBILITY_FILTERS: readonly { value: ProjectVisibility; label: string }[] = [
  { value: 'DRAFT', label: 'Brouillons' },
  { value: 'PUBLISHED', label: 'Publiés' },
  { value: 'ARCHIVED', label: 'Archivés' },
];

/** Page des projets (20 par page, D-CX), toutes visibilités, dans l'ordre du site. Contexte d'injection. */
export function projectPageResource(request: () => ProjectListRequest) {
  return httpResource<Page<AdminProjectSummary>>(() => ({
    url: API,
    params: toHttpParams({ ...request() }),
  }));
}

/** Un projet à modifier ; 404 s'il n'existe pas. Aucune requête sans identifiant. Contexte d'injection. */
export function projectResource(id: () => string | undefined) {
  return httpResource<AdminProject>(() => {
    const current = id();
    return current === undefined ? undefined : `${API}/${current}`;
  });
}

/** Toutes les technologies (vocabulaire court, D-CW), dans leur ordre d'affichage. Contexte d'injection. */
export function technologiesResource() {
  return httpResource<AdminTechnology[]>(() => '/api/admin/technologies');
}

/** Création (201) : slug généré depuis le titre ou suffixé s'il est pris (D-BD). */
export function createProject(
  http: HttpClient,
  request: SaveProjectRequest,
): Promise<AdminProject> {
  return firstValueFrom(http.post<AdminProject>(API, request));
}

/** Remplacement de toute la saisie ; 409 `SLUG_LOCKED` si le slug d'un projet déjà publié change (D11). */
export function updateProject(
  http: HttpClient,
  id: number,
  request: SaveProjectRequest,
): Promise<AdminProject> {
  return firstValueFrom(http.put<AdminProject>(`${API}/${id}`, request));
}
