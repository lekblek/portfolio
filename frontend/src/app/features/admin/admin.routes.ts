import {
  provideHttpClient,
  withInterceptors,
  withRequestsMadeViaParent,
} from '@angular/common/http';
import { Routes } from '@angular/router';

import { AdminFrame } from './admin-frame';
import { adminGuard } from './auth/admin-guard';
import { adminUnauthorizedInterceptor } from './auth/admin-unauthorized.interceptor';

/**
 * Administration (`/admin/**`) : rendue dans le navigateur seulement (`app.routes.server.ts`),
 * chargée à la demande, jamais par une page publique. Toutes les pages sont en `noindex`. Les
 * requêtes des pages passent par l'intercepteur des sessions expirées, puis par ceux de
 * l'application (marquage `/api/`, jeton CSRF).
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
    component: AdminFrame,
    providers: [
      provideHttpClient(
        withInterceptors([adminUnauthorizedInterceptor]),
        withRequestsMadeViaParent(),
      ),
    ],
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'Tableau de bord',
        data: { noindex: true },
        loadComponent: () => import('./dashboard/dashboard-page').then((m) => m.DashboardPage),
      },
      {
        path: 'taxonomy',
        loadChildren: () => import('./taxonomy/taxonomy.routes').then((m) => m.taxonomyRoutes),
      },
      {
        path: 'media',
        loadChildren: () => import('./media/media.routes').then((m) => m.mediaRoutes),
      },
    ],
  },
];
