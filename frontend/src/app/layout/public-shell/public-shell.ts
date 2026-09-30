import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';

import { focusPageHeadingOnNavigation } from '../../core/platform/focus-on-navigation';
import { Seo } from '../../core/seo/seo';
import { SiteFooter } from './site-footer';
import { SiteHeader } from './site-header';

const CONTENT_ID = 'contenu';

/**
 * Cadre des pages publiques : lien d'évitement, en-tête, `<main>`, pied de page.
 * Le lien d'évitement pointe vers l'adresse courante suivie de `#contenu` : avec
 * `<base href="/">`, un simple `#contenu` renverrait vers la racine du site.
 */
@Component({
  selector: 'app-public-shell',
  imports: [RouterOutlet, SiteHeader, SiteFooter],
  template: `
    <a class="skip-link" [attr.href]="skipLink()">Aller au contenu</a>
    <app-site-header />
    <main [id]="contentId" tabindex="-1">
      <router-outlet />
    </main>
    <app-site-footer />
  `,
  styles: `
    .skip-link {
      position: absolute;
      inset-inline-start: var(--spacing-gutter);
      top: calc(var(--spacing) * -16);
      z-index: var(--z-skip);
      padding: calc(var(--spacing) * 3) calc(var(--spacing) * 4);
      border-radius: var(--radius-control);
      background: var(--color-ink);
      color: var(--color-paper);
      font-weight: var(--font-weight-medium);
    }

    .skip-link:focus {
      top: calc(var(--spacing) * 2);
    }
  `,
})
export class PublicShell {
  protected readonly contentId = CONTENT_ID;

  private readonly router = inject(Router);

  protected readonly skipLink = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map((event) => withContentAnchor(event.urlAfterRedirects)),
    ),
    { initialValue: withContentAnchor(this.router.url) },
  );

  constructor() {
    focusPageHeadingOnNavigation();
    inject(Seo).setWebsiteJsonLd();
  }
}

function withContentAnchor(url: string): string {
  return `${url.split('#')[0]}#${CONTENT_ID}`;
}
