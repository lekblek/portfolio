import {
  booleanAttribute,
  Component,
  computed,
  ElementRef,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter, map } from 'rxjs';

import { AdminAccount } from '../../core/api/api-types';
import { focusPageHeadingOnNavigation } from '../../core/platform/focus-on-navigation';
import { SITE_NAME } from '../../core/seo/site-config';
import { formatDayTime } from '../../shared/format/date';
import { Button } from '../../shared/ui/button';
import { Icon } from '../../shared/ui/icon';
import { ToastRegion, Toaster } from '../../shared/ui/toast';

export interface AdminNavItem {
  /** Chemin absolu de la page (`/admin`). */
  path: string;
  label: string;
  /** Repérée sur ce seul chemin (accueil) ; sinon aussi sur ses sous-pages (`/admin/taxonomy/tags`). */
  exact: boolean;
}

const CONTENT_ID = 'contenu';

/**
 * Cadre des pages d'administration : lien d'évitement, en-tête (identité, administrateur connecté,
 * déconnexion), navigation en barre latérale dès 64 rem, `<main>`, région des notifications. En dessous, un bouton « Menu »
 * déplie dans le flux la navigation et la session (même motif que le site public : `Échap`
 * referme et rend le focus au bouton, une navigation referme). Présentation seulement : la session
 * et la déconnexion sont fournies par la page qui l'utilise.
 */
@Component({
  selector: 'app-admin-shell',
  imports: [Button, Icon, RouterLink, RouterLinkActive, ToastRegion],
  host: { '(keydown.escape)': 'closeAndFocusToggle()' },
  template: `
    <a class="skip-link" [attr.href]="skipLink()">Aller au contenu</a>
    <div class="admin-shell">
      <header class="admin-header">
        <p class="admin-identity">
          <a class="admin-site-name" routerLink="/admin">{{ siteName }}</a>
          <span class="admin-context">Administration</span>
        </p>
        <button
          #toggle
          appButton
          type="button"
          variant="secondary"
          size="sm"
          class="admin-menu-toggle"
          aria-controls="navigation-administration"
          [attr.aria-expanded]="open()"
          (click)="open.set(!open())"
        >
          <app-icon [name]="open() ? 'close' : 'menu'" />
          Menu
        </button>
        <div class="admin-session" [class.admin-panel-open]="open()">
          @if (account(); as current) {
            <p class="admin-account">
              Connecté en tant que <strong translate="no">{{ current.login }}</strong
              ><span class="text-ink-muted">, depuis le {{ since() }}</span>
            </p>
          }
          <button
            appButton
            type="button"
            variant="secondary"
            size="sm"
            [loading]="leaving()"
            (click)="signOut.emit()"
          >
            Se déconnecter
          </button>
        </div>
      </header>
      <nav
        id="navigation-administration"
        aria-label="Administration"
        class="admin-nav"
        [class.admin-panel-open]="open()"
      >
        <ul class="admin-nav-list">
          @for (item of navigation(); track item.path) {
            <li>
              <a
                class="admin-nav-link"
                [routerLink]="item.path"
                routerLinkActive="admin-nav-link-current"
                [routerLinkActiveOptions]="{ exact: item.exact }"
                ariaCurrentWhenActive="page"
                >{{ item.label }}</a
              >
            </li>
          }
        </ul>
        <p class="admin-nav-site">
          <a href="/">Voir le site</a>
        </p>
      </nav>
      <main
        class="admin-main"
        [class.admin-main-under-toasts]="toaster.toasts().length > 0"
        [id]="contentId"
        tabindex="-1"
      >
        <ng-content />
      </main>
    </div>
    <app-toast-region />
  `,
  styleUrl: './admin-shell.css',
})
export class AdminShell {
  readonly account = input<AdminAccount | null>(null);
  readonly navigation = input.required<readonly AdminNavItem[]>();
  /** Déconnexion en cours : le bouton l'indique et ignore les activations. */
  readonly leaving = input(false, { transform: booleanAttribute });
  readonly signOut = output<void>();

  protected readonly siteName = SITE_NAME;
  protected readonly contentId = CONTENT_ID;
  protected readonly open = signal(false);
  protected readonly since = computed(() => {
    const current = this.account();
    return current ? formatDayTime(current.lastLoginAt) : '';
  });

  protected readonly toaster = inject(Toaster);

  private readonly toggle = viewChild<ElementRef<HTMLButtonElement>>('toggle');
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
    this.router.events.pipe(takeUntilDestroyed()).subscribe((event) => {
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

function withContentAnchor(url: string): string {
  return `${url.split('#')[0]}#${CONTENT_ID}`;
}
