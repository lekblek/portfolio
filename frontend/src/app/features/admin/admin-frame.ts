import { Component, inject, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';

import { AdminNavItem, AdminShell } from '../../layout/admin-shell/admin-shell';
import { Alert } from '../../shared/ui/alert';
import { AdminSession } from './auth/admin-session';

/**
 * Pages d'administration réellement disponibles, dans l'ordre de la navigation : chaque écran
 * ajoute son lien avec sa route (aucun lien vers une page qui n'existe pas encore).
 */
export const ADMIN_NAVIGATION: readonly AdminNavItem[] = [
  { path: '/admin', label: 'Tableau de bord' },
];

/**
 * Cadre routé des pages d'administration : le shell, l'administrateur connecté et la
 * déconnexion. Une déconnexion qui échoue (serveur injoignable) garde la session et le dit.
 */
@Component({
  selector: 'app-admin-frame',
  imports: [AdminShell, Alert, RouterOutlet],
  template: `
    <app-admin-shell
      [account]="session.account()"
      [navigation]="navigation"
      [leaving]="leaving()"
      (signOut)="signOut()"
    >
      @if (failed()) {
        <app-alert class="mb-block max-w-prose" tone="danger" title="Déconnexion impossible">
          <p>Le serveur ne répond pas pour le moment&#8239;: réessayez dans quelques instants.</p>
        </app-alert>
      }
      <router-outlet />
    </app-admin-shell>
  `,
})
export class AdminFrame {
  protected readonly session = inject(AdminSession);
  protected readonly navigation = ADMIN_NAVIGATION;
  protected readonly leaving = signal(false);
  protected readonly failed = signal(false);

  private readonly router = inject(Router);

  protected async signOut(): Promise<void> {
    this.leaving.set(true);
    this.failed.set(false);
    try {
      await this.session.close();
    } catch {
      this.failed.set(true);
      return;
    } finally {
      this.leaving.set(false);
    }
    await this.router.navigateByUrl('/admin/login');
  }
}
