import { Routes } from '@angular/router';

import { unsavedChangesGuard } from '../../../shared/forms/unsaved-changes.guard';

/**
 * Profil (`/admin/profile`) : une seule page, un seul enregistrement. Quitter avec une saisie non
 * enregistrée demande confirmation.
 */
export const profileRoutes: Routes = [
  {
    path: '',
    title: 'Profil',
    data: { noindex: true },
    canDeactivate: [unsavedChangesGuard],
    loadComponent: () => import('./pages/profile-page').then((m) => m.ProfilePage),
  },
];
