import { Component, inject, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';

import { AdminNavItem, AdminShell } from '../../layout/admin-shell/admin-shell';
import { Toaster } from '../../shared/ui/toast';
import { AdminSession } from './auth/admin-session';

/**
 * Pages d'administration réellement disponibles, dans l'ordre de la navigation : chaque écran
 * ajoute son lien avec sa route (aucun lien vers une page qui n'existe pas encore).
 */
export const ADMIN_NAVIGATION: readonly AdminNavItem[] = [
  { path: '/admin', label: 'Tableau de bord', exact: true },
  { path: '/admin/profile', label: 'Profil', exact: false },
  { path: '/admin/projects', label: 'Projets', exact: false },
  { path: '/admin/taxonomy', label: 'Taxonomie', exact: false },
  { path: '/admin/media', label: 'Médias', exact: false },
];

/**
 * Cadre routé des pages d'administration : le shell, l'administrateur connecté et la
 * déconnexion. Une déconnexion qui échoue (serveur injoignable) garde la session et le dit par
 * une notification.
 */
@Component({
  selector: 'app-admin-frame',
  imports: [AdminShell, RouterOutlet],
  template: `
    <app-admin-shell
      [account]="session.account()"
      [navigation]="navigation"
      [leaving]="leaving()"
      (signOut)="signOut()"
    >
      <router-outlet />
    </app-admin-shell>
  `,
})
export class AdminFrame {
  protected readonly session = inject(AdminSession);
  protected readonly navigation = ADMIN_NAVIGATION;
  protected readonly leaving = signal(false);

  private readonly router = inject(Router);
  private readonly toaster = inject(Toaster);

  protected async signOut(): Promise<void> {
    this.leaving.set(true);
    try {
      await this.session.close();
    } catch {
      this.toaster.show(
        'Déconnexion impossible\u202f: le serveur ne répond pas pour le moment, réessayez dans quelques instants.',
        'danger',
      );
      return;
    } finally {
      this.leaving.set(false);
    }
    await this.router.navigateByUrl('/admin/login');
  }
}
