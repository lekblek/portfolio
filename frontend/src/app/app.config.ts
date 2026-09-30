import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideClientHydration } from '@angular/platform-browser';
import {
  provideRouter,
  TitleStrategy,
  withComponentInputBinding,
  withInMemoryScrolling,
} from '@angular/router';

import { routes } from './app.routes';
import { serverApiOriginInterceptor } from './core/api/server-api-origin.interceptor';
import { PageTitleStrategy } from './core/seo/page-title.strategy';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Fetch est le moteur par défaut en v22 ; l'intercepteur n'agit que si API_ORIGIN est fourni (serveur)
    provideHttpClient(withInterceptors([serverApiOriginInterceptor])),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'enabled', anchorScrolling: 'enabled' }),
    ),
    { provide: TitleStrategy, useClass: PageTitleStrategy },
    provideClientHydration(),
  ],
};
