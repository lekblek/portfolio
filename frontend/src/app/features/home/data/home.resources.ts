import { httpResource } from '@angular/common/http';

import {
  Profile,
  ProjectSummary,
  PublicationSummary,
  PublicationType,
  SeriesSummary,
} from '../../../core/api/api-types';
import { Page } from '../../../core/api/page';

/**
 * Données de l'accueil : chaque bloc a sa propre requête, toutes lancées en même temps, lues
 * pendant le rendu serveur puis reprises par le navigateur depuis le cache de transfert.
 * À appeler dans un contexte d'injection.
 */

/** Profil public : nom, titre, présentation courte, CV et liens professionnels. */
export function homeProfileResource() {
  return httpResource<Profile>(() => '/api/public/profile');
}

/** Projets mis en avant (D-EH), dans l'ordre choisi par le propriétaire. */
export function featuredProjectsResource(size: number) {
  return httpResource<Page<ProjectSummary>>(() => ({
    url: '/api/public/projects',
    params: { featured: true, size },
  }));
}

/** Premiers projets publiés, tous confondus : leur nombre, et le repli sans projet mis en avant. */
export function firstProjectsResource(size: number) {
  return httpResource<Page<ProjectSummary>>(() => ({
    url: '/api/public/projects',
    params: { size },
  }));
}

/** Dernières publications visibles d'un type, des plus récentes aux plus anciennes. */
export function latestPublicationsResource(type: PublicationType, size: number) {
  return httpResource<Page<PublicationSummary>>(() => ({
    url: '/api/public/publications',
    params: { type, size },
  }));
}

/** Premières séries publiques. */
export function firstSeriesResource(size: number) {
  return httpResource<Page<SeriesSummary>>(() => ({
    url: '/api/public/series',
    params: { size },
  }));
}
