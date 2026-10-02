import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { Alert } from '../../../shared/ui/alert';
import { Button } from '../../../shared/ui/button';
import { AdminSession } from '../auth/admin-session';

/**
 * Accueil de l'administration (`/admin`), atteint après la connexion : administrateur connecté et
 * déconnexion. Le shell d'administration et le tableau de bord (compteurs, raccourcis) viennent en
 * F23. Une déconnexion qui échoue (serveur injoignable) garde la session et le dit.
 */
@Component({
  selector: 'app-dashboard-page',
  imports: [Alert, Button],
  template: `
    <main id="contenu" class="page-container" tabindex="-1">
      <div class="grid max-w-prose gap-block py-section">
        <div>
          <p class="text-sm font-semibold text-ink-muted">Administration</p>
          <h1 class="mt-2 text-3xl leading-tight tracking-title">Tableau de bord</h1>
          @if (session.account(); as account) {
            <p class="mt-flow">
              Connecté en tant que <strong translate="no">{{ account.login }}</strong
              >.
            </p>
          }
        </div>
        @if (failed()) {
          <app-alert tone="danger" title="Déconnexion impossible">
            <p>Le serveur ne répond pas pour le moment&#8239;: réessayez dans quelques instants.</p>
          </app-alert>
        }
        <div>
          <button
            appButton
            type="button"
            variant="secondary"
            [loading]="leaving()"
            (click)="signOut()"
          >
            Se déconnecter
          </button>
        </div>
      </div>
    </main>
  `,
})
export class DashboardPage {
  protected readonly session = inject(AdminSession);
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
