import { Routes } from '@angular/router';

import { adminGuard } from './auth/admin-guard';

/**
 * Administration (`/admin/**`) : rendue dans le navigateur seulement (`app.routes.server.ts`),
 * chargée à la demande, jamais par une page publique. Toutes les pages sont en `noindex`.
 */
export const adminRoutes: Routes = [
  {
    path: 'login',
    title: 'Connexion',
    data: { noindex: true },
    loadComponent: () => import('./auth/pages/login-page').then((m) => m.LoginPage),
  },
  {
    path: '',
    canMatch: [adminGuard],
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'Administration',
        data: { noindex: true },
        loadComponent: () => import('./dashboard/dashboard-page').then((m) => m.DashboardPage),
      },
    ],
  },
];
