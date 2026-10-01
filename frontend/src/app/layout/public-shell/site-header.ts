import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { SITE_NAME, SITE_SIGNATURE } from '../../core/seo/site-config';
import { PUBLIC_NAVIGATION } from './navigation';
import { SiteNav } from './site-nav';

/**
 * En-tête du site : identité (le nom mène à l'accueil) et navigation principale.
 */
@Component({
  selector: 'app-site-header',
  imports: [RouterLink, RouterLinkActive, SiteNav],
  template: `
    <header class="border-b border-rule">
      <div class="site-header-inner page-container">
        <p class="site-identity">
          <a
            class="site-name"
            routerLink="/"
            routerLinkActive
            [routerLinkActiveOptions]="{ exact: true }"
            ariaCurrentWhenActive="page"
            >{{ name }}</a
          >
          <span class="block text-sm text-ink-muted">{{ signature }}</span>
        </p>
        <app-site-nav [items]="navigation" />
      </div>
    </header>
  `,
  styles: `
    .site-header-inner {
      display: grid;
      grid-template-columns: minmax(0, 1fr) auto;
      grid-template-areas:
        'identity toggle'
        'nav nav';
      align-items: center;
      column-gap: calc(var(--spacing) * 6);
      min-height: calc(var(--spacing) * 16);
      padding-block: calc(var(--spacing) * 3);
    }

    .site-identity {
      grid-area: identity;
    }

    .site-name {
      display: inline-block;
      color: var(--color-ink);
      font-weight: var(--font-weight-semibold);
      letter-spacing: var(--tracking-heading);
      text-decoration-line: none;
    }

    .site-name:hover,
    .site-name:focus-visible {
      color: var(--color-accent-strong);
      text-decoration-line: underline;
    }

    @media (min-width: 64rem) {
      .site-header-inner {
        grid-template-areas: 'identity nav';
      }
    }
  `,
})
export class SiteHeader {
  protected readonly name = SITE_NAME;
  protected readonly signature = SITE_SIGNATURE;
  protected readonly navigation = PUBLIC_NAVIGATION;
}
