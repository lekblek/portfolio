import { isDevMode } from '@angular/core';
import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    // Catalogue du système de design : développement seulement (en production, page introuvable)
    path: '_ui',
    title: 'Catalogue de l’interface',
    canMatch: [() => isDevMode()],
    loadComponent: () => import('./dev/ui-catalogue').then((m) => m.UiCatalogue),
  },
  {
    path: '**',
    title: 'Page introuvable',
    loadComponent: () => import('./features/not-found/not-found').then((m) => m.NotFound),
  },
];
