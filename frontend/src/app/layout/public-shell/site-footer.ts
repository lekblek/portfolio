import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SITE_NAME, SITE_SIGNATURE } from '../../core/seo/site-config';
import { PUBLIC_NAVIGATION } from './navigation';

/**
 * Pied de page (DS09) : identité, plan du site en deux colonnes (contenus publiés, pages du
 * site), droits. Aucun appel au profil depuis le cadre (D-EC) : les liens professionnels restent
 * sur l'accueil, la page À propos et le contact ; aucun lien n'est inventé.
 */
@Component({
  selector: 'app-site-footer',
  imports: [RouterLink],
  template: `
    <footer class="mt-section border-t border-rule bg-paper-sunken">
      <div class="page-container grid gap-block py-block lg:grid-cols-12 lg:gap-8">
        <p class="lg:col-span-6">
          <span class="block font-semibold tracking-heading">{{ name }}</span>
          <span class="block text-sm text-ink-muted">{{ signature }}</span>
        </p>
        <nav aria-label="Plan du site" class="footer-plan lg:col-span-6">
          @for (column of columns; track column.title) {
            <div>
              <h2 class="text-sm font-semibold text-ink-muted">{{ column.title }}</h2>
              <ul class="mt-2">
                @for (link of column.links; track link.path) {
                  <li>
                    <a class="inline-flex min-h-8 items-center" [routerLink]="link.path">{{
                      link.label
                    }}</a>
                  </li>
                }
              </ul>
            </div>
          }
        </nav>
        <p class="text-sm text-ink-muted lg:col-span-12">© {{ year }} {{ name }}</p>
      </div>
    </footer>
  `,
  styles: `
    .footer-plan {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: var(--spacing-flow) calc(var(--spacing) * 8);
    }
  `,
})
export class SiteFooter {
  protected readonly name = SITE_NAME;
  protected readonly signature = SITE_SIGNATURE;
  protected readonly columns = [
    { title: 'Contenus', links: PUBLIC_NAVIGATION.filter((item) => item.section === 'content') },
    { title: 'Le site', links: PUBLIC_NAVIGATION.filter((item) => item.section === 'site') },
  ];
  protected readonly year = new Date().getFullYear();
}
