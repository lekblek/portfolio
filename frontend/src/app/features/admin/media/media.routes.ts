import { Routes } from '@angular/router';

import { unsavedChangesGuard } from '../../../shared/forms/unsaved-changes.guard';

/**
 * Médiathèque (`/admin/media`) : liste paginée avec la zone d'envoi, et page de chaque média
 * (texte alternatif). Quitter pendant un envoi ou avec une saisie non enregistrée demande
 * confirmation.
 */
export const mediaRoutes: Routes = [
  {
    path: '',
    title: 'Médias',
    data: { noindex: true },
    canDeactivate: [unsavedChangesGuard],
    loadComponent: () => import('./pages/media-list-page').then((m) => m.MediaListPage),
  },
  {
    path: ':id',
    title: 'Média',
    data: { noindex: true },
    canDeactivate: [unsavedChangesGuard],
    loadComponent: () => import('./pages/media-edit-page').then((m) => m.MediaEditPage),
  },
];
