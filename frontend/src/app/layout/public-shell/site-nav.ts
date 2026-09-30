import { Component, ElementRef, inject, input, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';

import { Button } from '../../shared/ui/button';
import { Icon } from '../../shared/ui/icon';
import { NavItem } from './navigation';

/**
 * Navigation principale. Dès `lg`, les liens sont visibles dans l'en-tête ; en dessous, un
 * bouton « Menu » (`aria-expanded`, `aria-controls`) déplie la même liste dans le flux, sous
 * l'identité, sans recouvrir la page. `Échap` referme et rend le focus au bouton ; une
 * navigation referme. Rien n'est rendu tant qu'aucune page n'est disponible.
 * L'hôte est en `display: contents` : la grille de l'en-tête place le bouton et la liste.
 */
@Component({
  selector: 'app-site-nav',
  imports: [RouterLink, RouterLinkActive, Button, Icon],
  host: { '(keydown.escape)': 'closeAndFocusToggle()' },
  template: `
    @if (items().length > 0) {
      <button
        #toggle
        appButton
        type="button"
        variant="secondary"
        size="sm"
        class="site-nav-toggle lg:hidden"
        aria-controls="navigation-principale"
        [attr.aria-expanded]="open()"
        (click)="open.set(!open())"
      >
        <app-icon [name]="open() ? 'close' : 'menu'" />
        Menu
      </button>
      <nav
        id="navigation-principale"
        aria-label="Navigation principale"
        class="site-nav"
        [class.site-nav-open]="open()"
      >
        <ul class="site-nav-list">
          @for (item of items(); track item.path) {
            <li>
              <a
                class="site-nav-link"
                [routerLink]="item.path"
                routerLinkActive="site-nav-link-current"
                ariaCurrentWhenActive="page"
                >{{ item.label }}</a
              >
            </li>
          }
        </ul>
      </nav>
    }
  `,
  styleUrl: './site-nav.css',
})
export class SiteNav {
  readonly items = input.required<readonly NavItem[]>();

  protected readonly open = signal(false);
  private readonly toggle = viewChild<ElementRef<HTMLButtonElement>>('toggle');

  constructor() {
    inject(Router)
      .events.pipe(takeUntilDestroyed())
      .subscribe((event) => {
        if (event instanceof NavigationEnd) {
          this.open.set(false);
        }
      });
  }

  protected closeAndFocusToggle(): void {
    if (this.open()) {
      this.open.set(false);
      this.toggle()?.nativeElement.focus();
    }
  }
}
