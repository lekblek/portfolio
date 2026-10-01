import { Routes } from '@angular/router';

import { PublicationType } from '../../core/api/api-types';

/**
 * Routes d'un type de publication, montées sous `/articles` (ARTICLE) et `/news` (NEWS) : mêmes
 * pages, type transmis en entrée par la donnée de route.
 */
export function publicationRoutes(type: PublicationType): Routes {
  return [
    {
      path: '',
      title: type === 'ARTICLE' ? 'Articles' : 'Actualités',
      data: { type },
      loadComponent: () => import('./pages/publication-list').then((m) => m.PublicationList),
    },
    {
      // Titre remplacé par celui de la publication dès sa lecture
      path: ':slug',
      title: type === 'ARTICLE' ? 'Article' : 'Actualité',
      data: { type },
      loadComponent: () => import('./pages/publication-detail').then((m) => m.PublicationDetail),
    },
  ];
}
