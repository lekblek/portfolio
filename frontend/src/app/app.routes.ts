import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '**',
    title: 'Page introuvable',
    loadComponent: () => import('./features/not-found/not-found').then((m) => m.NotFound),
  },
];
