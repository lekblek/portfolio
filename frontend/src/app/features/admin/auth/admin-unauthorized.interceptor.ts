import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AdminSession } from './admin-session';
import { loginTree } from './return-url';

/**
 * Requêtes des pages d'administration : une 401 de `/api/admin/` (session expirée après 30 minutes
 * d'inactivité, D-CR, ou fermée ailleurs) oublie la session et renvoie à la connexion avec la page
 * en cours en `returnUrl` ; la page de connexion dit que la session a expiré. L'erreur est
 * transmise telle quelle à l'appelant.
 */
export const adminUnauthorizedInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(AdminSession);
  const router = inject(Router);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        request.url.startsWith('/api/admin/')
      ) {
        session.expire();
        void router.navigateByUrl(loginTree(router, router.url));
      }
      return throwError(() => error);
    }),
  );
};
