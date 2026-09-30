import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { API_ORIGIN } from './api-origin';

/**
 * Préfixe les URL `/api/...` par l'origine interne de l'API pendant le rendu serveur.
 * Sans `API_ORIGIN` (navigateur), la requête reste relative et passe inchangée.
 */
export const serverApiOriginInterceptor: HttpInterceptorFn = (request, next) => {
  const origin = inject(API_ORIGIN, { optional: true });
  if (origin === null || !request.url.startsWith('/api/')) {
    return next(request);
  }
  return next(request.clone({ url: origin + request.url }));
};
