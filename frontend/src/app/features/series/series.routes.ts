import { Routes } from '@angular/router';

export const seriesRoutes: Routes = [
  {
    path: '',
    title: 'Séries',
    loadComponent: () => import('./pages/series-list').then((m) => m.SeriesList),
  },
  {
    // Titre remplacé par celui de la série dès sa lecture
    path: ':slug',
    title: 'Série',
    loadComponent: () => import('./pages/series-detail').then((m) => m.SeriesDetail),
  },
];
