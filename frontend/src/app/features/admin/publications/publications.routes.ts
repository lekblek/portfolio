import { Routes } from '@angular/router';

import { unsavedChangesGuard } from '../../../shared/forms/unsaved-changes.guard';

const form = () => import('./pages/publication-form-page').then((m) => m.PublicationFormPage);

/**
 * Publications (`/admin/publications`) : liste filtrée et paginée, création par type (le type ne
 * change plus ensuite), modification et statut. Quitter une saisie non enregistrée demande
 * confirmation.
 */
export const publicationRoutes: Routes = [
  {
    path: '',
    title: 'Publications',
    data: { noindex: true },
    loadComponent: () => import('./pages/publication-list-page').then((m) => m.PublicationListPage),
  },
  {
    path: 'new/article',
    title: 'Nouvel article',
    data: { noindex: true, createType: 'ARTICLE' },
    canDeactivate: [unsavedChangesGuard],
    loadComponent: form,
  },
  {
    path: 'new/news',
    title: 'Nouvelle actualité',
    data: { noindex: true, createType: 'NEWS' },
    canDeactivate: [unsavedChangesGuard],
    loadComponent: form,
  },
  {
    path: ':id',
    title: 'Modifier la publication',
    data: { noindex: true },
    canDeactivate: [unsavedChangesGuard],
    loadComponent: form,
  },
];
