import { Routes } from '@angular/router';

export const aboutRoutes: Routes = [
  {
    path: '',
    title: 'À propos',
    loadComponent: () => import('./pages/about-page').then((m) => m.AboutPage),
  },
];
