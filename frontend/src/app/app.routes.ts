import { isDevMode } from '@angular/core';
import { Routes } from '@angular/router';

import { PublicShell } from './layout/public-shell/public-shell';

export const routes: Routes = [
  {
    // Spécimen du rendu Markdown : développement seulement
    path: '_ui/prose',
    title: 'Rendu Markdown',
    canMatch: [() => isDevMode()],
    loadComponent: () => import('./dev/prose-specimen').then((m) => m.ProseSpecimen),
  },
  {
    // Catalogue du système de design : développement seulement (en production, page introuvable)
    path: '_ui',
    title: 'Catalogue de l’interface',
    canMatch: [() => isDevMode()],
    loadComponent: () => import('./dev/ui-catalogue').then((m) => m.UiCatalogue),
  },
  {
    // Cadre commun des pages publiques ; chaque page publique s'ajoute à ses enfants
    path: '',
    component: PublicShell,
    children: [
      {
        path: '**',
        title: 'Page introuvable',
        data: { noindex: true },
        loadComponent: () => import('./features/not-found/not-found').then((m) => m.NotFound),
      },
    ],
  },
];
