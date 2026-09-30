import { HttpContextToken, HttpInterceptorFn } from '@angular/common/http';

/** Requête adressée à l'API du projet (`/api/...`), marquée avant toute réécriture d'adresse. */
export const API_REQUEST = new HttpContextToken<boolean>(() => false);

/**
 * Marque les requêtes `/api/...` sans changer leur URL, au serveur comme dans le navigateur.
 * L'URL reste relative pour le cache de transfert (même clé des deux côtés) ; au rendu serveur,
 * `ServerApiBackend` envoie les requêtes marquées à l'origine interne de l'API, même après que
 * `@angular/platform-server` les a rendues absolues sur l'origine de la page.
 */
export const apiRequestInterceptor: HttpInterceptorFn = (request, next) =>
  next(
    request.url.startsWith('/api/')
      ? request.clone({ context: request.context.set(API_REQUEST, true) })
      : request,
  );
