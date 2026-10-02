import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';

import { AdminSession } from './admin-session';
import { loginTree } from './return-url';

/**
 * Pages de l'administration (hors connexion) : sans session ouverte, renvoie à la connexion avec
 * l'adresse demandée en `returnUrl` (omise pour l'accueil de l'administration). Un serveur
 * injoignable renvoie aussi à la connexion, qui le signale.
 */
export const adminGuard: CanMatchFn = async (_route, segments) => {
  const session = inject(AdminSession);
  const router = inject(Router);
  try {
    if (await session.isOpen()) {
      return true;
    }
  } catch {
    // Vérification impossible : la page de connexion refait la demande et affiche l'échec
  }
  return loginTree(router, ['/admin', ...segments.map((segment) => segment.path)].join('/'));
};
