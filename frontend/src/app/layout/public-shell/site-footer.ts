import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SITE_NAME, SITE_SIGNATURE } from '../../core/seo/site-config';
import { PUBLIC_NAVIGATION } from './navigation';

/**
 * Pied de page sobre : identité, liens principaux (seulement les pages disponibles), droits.
 * Les liens professionnels viendront des données réelles du profil ; aucun lien n'est inventé.
 */
@Component({
  selector: 'app-site-footer',
  imports: [RouterLink],
  template: `
    <footer class="mt-section border-t border-rule">
      <div class="page-container grid gap-6 py-block lg:grid-cols-12 lg:gap-8">
        <p class="lg:col-span-6">
          <span class="block font-semibold tracking-heading">{{ name }}</span>
          <span class="block text-sm text-ink-muted">{{ signature }}</span>
        </p>
        @if (links.length > 0) {
          <nav aria-label="Liens principaux" class="lg:col-span-6 lg:justify-self-end">
            <ul class="cluster-6">
              @for (link of links; track link.path) {
                <li>
                  <a [routerLink]="link.path">{{ link.label }}</a>
                </li>
              }
            </ul>
          </nav>
        }
        <p class="text-sm text-ink-muted lg:col-span-12">© {{ year }} {{ name }}</p>
      </div>
    </footer>
  `,
})
export class SiteFooter {
  protected readonly name = SITE_NAME;
  protected readonly signature = SITE_SIGNATURE;
  protected readonly links = PUBLIC_NAVIGATION.filter((item) => item.footer);
  protected readonly year = new Date().getFullYear();
}
