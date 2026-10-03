import { Routes } from '@angular/router';

import { unsavedChangesGuard } from '../../../shared/forms/unsaved-changes.guard';

/**
 * Projets (`/admin/projects`) : liste filtrable et paginée, création, modification. Quitter une
 * saisie non enregistrée demande confirmation.
 */
export const projectRoutes: Routes = [
  {
    path: '',
    title: 'Projets',
    data: { noindex: true },
    loadComponent: () => import('./pages/project-list-page').then((m) => m.ProjectListPage),
  },
  {
    path: 'new',
    title: 'Nouveau projet',
    data: { noindex: true },
    canDeactivate: [unsavedChangesGuard],
    loadComponent: () => import('./pages/project-form-page').then((m) => m.ProjectFormPage),
  },
  {
    path: ':id',
    title: 'Modifier le projet',
    data: { noindex: true },
    canDeactivate: [unsavedChangesGuard],
    loadComponent: () => import('./pages/project-form-page').then((m) => m.ProjectFormPage),
  },
];
