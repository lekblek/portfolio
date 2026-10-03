import { Routes } from '@angular/router';

import { unsavedChangesGuard } from '../../../shared/forms/unsaved-changes.guard';

const form = () => import('./pages/series-form-page').then((m) => m.SeriesFormPage);

/**
 * Séries (`/admin/series`) : liste, création, modification et chapitres. Quitter une saisie non
 * enregistrée (série ou chapitres) demande confirmation.
 */
export const seriesRoutes: Routes = [
  {
    path: '',
    title: 'Séries',
    data: { noindex: true },
    loadComponent: () => import('./pages/series-list-page').then((m) => m.SeriesListPage),
  },
  {
    path: 'new',
    title: 'Nouvelle série',
    data: { noindex: true },
    canDeactivate: [unsavedChangesGuard],
    loadComponent: form,
  },
  {
    path: ':id',
    title: 'Modifier la série',
    data: { noindex: true },
    canDeactivate: [unsavedChangesGuard],
    loadComponent: form,
  },
];
