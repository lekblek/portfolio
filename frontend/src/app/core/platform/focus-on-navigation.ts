import { isPlatformBrowser } from '@angular/common';
import {
  afterNextRender,
  DestroyRef,
  DOCUMENT,
  inject,
  Injector,
  PLATFORM_ID,
} from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';

/**
 * Après chaque navigation dans le navigateur (pas au premier affichage), place le focus sur le
 * titre `<h1>` de la nouvelle page, ou sur `<main>` à défaut : un lecteur d'écran annonce le
 * changement de page et la tabulation repart du contenu. Une adresse avec ancre garde le
 * comportement du routeur (défilement vers l'ancre). À appeler dans un contexte d'injection.
 */
export function focusPageHeadingOnNavigation(): void {
  if (!isPlatformBrowser(inject(PLATFORM_ID))) {
    return;
  }
  const document = inject(DOCUMENT);
  const injector = inject(Injector);
  let firstNavigation = true;

  const subscription = inject(Router).events.subscribe((event) => {
    if (!(event instanceof NavigationEnd)) {
      return;
    }
    if (firstNavigation) {
      firstNavigation = false;
      return;
    }
    if (event.urlAfterRedirects.includes('#')) {
      return;
    }
    afterNextRender(
      () => {
        const target =
          document.querySelector<HTMLElement>('main h1') ?? document.querySelector('main');
        if (target === null) {
          return;
        }
        if (!target.hasAttribute('tabindex')) {
          target.setAttribute('tabindex', '-1');
        }
        target.focus({ preventScroll: true });
      },
      { injector },
    );
  });
  inject(DestroyRef).onDestroy(() => subscription.unsubscribe());
}
