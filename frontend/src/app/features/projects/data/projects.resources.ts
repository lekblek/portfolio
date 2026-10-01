import { httpResource } from '@angular/common/http';

import { Project, ProjectSummary } from '../../../core/api/api-types';
import { Page, PageRequest, toHttpParams } from '../../../core/api/page';

/**
 * Page de projets publiés (`GET /api/public/projects`), relue à chaque changement de page ou de
 * filtre (`?page=`, `?technology=`). À appeler dans un contexte d'injection.
 */
export function projectListResource(query: () => PageRequest) {
  return httpResource<Page<ProjectSummary>>(() => ({
    url: '/api/public/projects',
    params: toHttpParams(query()),
  }));
}

/** Projet publié par son slug (`GET /api/public/projects/{slug}`) ; 404 s'il n'existe pas. */
export function projectResource(slug: () => string) {
  return httpResource<Project>(() => `/api/public/projects/${encodeURIComponent(slug())}`);
}
