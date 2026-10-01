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
        path: '',
        pathMatch: 'full',
        loadComponent: () => import('./features/home/pages/home-page').then((m) => m.HomePage),
      },
      {
        path: 'projects',
        loadChildren: () =>
          import('./features/projects/projects.routes').then((m) => m.projectsRoutes),
      },
      {
        path: 'articles',
        loadChildren: () =>
          import('./features/publications/publications.routes').then((m) =>
            m.publicationRoutes('ARTICLE'),
          ),
      },
      {
        path: 'series',
        loadChildren: () => import('./features/series/series.routes').then((m) => m.seriesRoutes),
      },
      {
        path: 'news',
        loadChildren: () =>
          import('./features/publications/publications.routes').then((m) =>
            m.publicationRoutes('NEWS'),
          ),
      },
      {
        path: 'about',
        loadChildren: () => import('./features/about/about.routes').then((m) => m.aboutRoutes),
      },
      {
        path: 'contact',
        loadChildren: () =>
          import('./features/contact/contact.routes').then((m) => m.contactRoutes),
      },
      {
        path: 'search',
        loadChildren: () => import('./features/search/search.routes').then((m) => m.searchRoutes),
      },
      {
        path: '**',
        title: 'Page introuvable',
        data: { noindex: true },
        loadComponent: () => import('./features/not-found/not-found').then((m) => m.NotFound),
      },
    ],
  },
];
