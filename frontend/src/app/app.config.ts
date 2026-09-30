import { HttpRequest, provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideClientHydration, withHttpTransferCacheOptions } from '@angular/platform-browser';
import {
  provideRouter,
  TitleStrategy,
  withComponentInputBinding,
  withInMemoryScrolling,
} from '@angular/router';

import { routes } from './app.routes';
import { apiRequestInterceptor } from './core/api/api-request';
import { PageTitleStrategy } from './core/seo/page-title.strategy';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Fetch est le moteur par défaut en v22 ; les appels /api/... sont marqués ici, puis envoyés à
    // l'API interne par ServerApiBackend au rendu serveur (app.config.server.ts)
    provideHttpClient(withInterceptors([apiRequestInterceptor])),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'enabled', anchorScrolling: 'enabled' }),
    ),
    { provide: TitleStrategy, useClass: PageTitleStrategy },
    provideClientHydration(
      // Réponses de l'API publique lues au rendu serveur, reprises par le navigateur sans second
      // appel, malgré le Cache-Control: no-store que Spring Security pose sur toute réponse : elles
      // sont anonymes et déjà écrites dans le HTML de la même page (D-EB)
      withHttpTransferCacheOptions({
        filter: isPublicApiRequest,
        includeNonCacheableRequests: true,
      }),
    ),
  ],
};

function isPublicApiRequest(request: HttpRequest<unknown>): boolean {
  return request.url.startsWith('/api/public/');
}
