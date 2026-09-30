import { FetchBackend, HttpEvent, HttpRequest } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';

import { API_ORIGIN } from './api-origin';
import { API_REQUEST } from './api-request';

/**
 * Transport du rendu serveur : le transport Fetch d'Angular, qui envoie d'abord toute requête
 * marquée `API_REQUEST` à l'origine interne de l'API (`API_ORIGIN`), en ne gardant que son chemin
 * et sa requête. Fourni seulement par `app.config.server.ts`.
 *
 * Pourquoi au transport, après tous les intercepteurs (D-EB) :
 * - le cache de transfert calcule sa clé sur l'URL encore relative, la même que dans le
 *   navigateur ; un intercepteur qui la rendait absolue au serveur seulement faisait rejouer
 *   l'appel par le navigateur ;
 * - `@angular/platform-server` rend les URL relatives absolues sur l'origine de la page, tirée
 *   de l'en-tête `Host` : cette origine est ici remplacée, elle ne choisit jamais la destination.
 */
@Service({ autoProvided: false })
export class ServerApiBackend extends FetchBackend {
  private readonly apiOrigin = inject(API_ORIGIN);

  override handle(request: HttpRequest<unknown>): Observable<HttpEvent<unknown>> {
    if (!request.context.get(API_REQUEST)) {
      return super.handle(request);
    }
    const { pathname, search } = new URL(request.url, this.apiOrigin);
    return super.handle(request.clone({ url: `${this.apiOrigin}${pathname}${search}` }));
  }
}
