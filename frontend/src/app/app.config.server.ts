import { ApplicationConfig, mergeApplicationConfig } from '@angular/core';
import { provideServerRendering, withRoutes } from '@angular/ssr';

import { appConfig } from './app.config';
import { serverRoutes } from './app.routes.server';
import { API_ORIGIN } from './core/api/api-origin';

const DEFAULT_API_ORIGIN = 'http://localhost:8080';

const serverConfig: ApplicationConfig = {
  providers: [
    provideServerRendering(withRoutes(serverRoutes)),
    // Lue dans l'environnement du serveur SSR ; une valeur qui n'est pas une URL échoue au rendu
    {
      provide: API_ORIGIN,
      useFactory: () => new URL(process.env['API_ORIGIN'] ?? DEFAULT_API_ORIGIN).origin,
    },
  ],
};

export const config = mergeApplicationConfig(appConfig, serverConfig);
