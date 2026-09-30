import { HttpBackend } from '@angular/common/http';
import { ApplicationConfig, mergeApplicationConfig } from '@angular/core';
import { provideServerRendering, withRoutes } from '@angular/ssr';

import { appConfig } from './app.config';
import { serverRoutes } from './app.routes.server';
import { API_ORIGIN } from './core/api/api-origin';
import { ServerApiBackend } from './core/api/server-api-backend';
import { SITE_URL } from './core/seo/site-config';

const DEFAULT_API_ORIGIN = 'http://localhost:8080';
const DEVELOPMENT_SITE_URL = 'http://localhost:4200';

const serverConfig: ApplicationConfig = {
  providers: [
    provideServerRendering(withRoutes(serverRoutes)),
    // Lue dans l'environnement du serveur SSR ; une valeur qui n'est pas une URL échoue au rendu
    {
      provide: API_ORIGIN,
      useFactory: () => new URL(process.env['API_ORIGIN'] ?? DEFAULT_API_ORIGIN).origin,
    },
    // Appels marqués API_REQUEST envoyés à cette origine, après les intercepteurs et le cache de transfert
    { provide: HttpBackend, useClass: ServerApiBackend },
    // Origine publique des adresses canoniques : jamais déduite de l'en-tête Host de la requête
    { provide: SITE_URL, useFactory: siteUrl },
  ],
};

// SITE_URL est exigée au démarrage du serveur de production (src/server.ts) ; ici, repli pour le
// serveur de développement et l'extraction des routes au build.
function siteUrl(): string {
  return new URL(process.env['SITE_URL'] ?? DEVELOPMENT_SITE_URL).origin;
}

export const config = mergeApplicationConfig(appConfig, serverConfig);
