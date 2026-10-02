import { Component, computed } from '@angular/core';

import { ErrorState } from '../../../shared/ui/error-state';
import { adminCountResource } from './data/dashboard.resources';

/**
 * Tableau de bord (`/admin`) : relevé de ce qui attend l'administrateur (messages non lus) et des
 * contenus enregistrés, tous statuts confondus. Chaque nombre vient du total d'une liste
 * d'administration existante. Les liens vers les écrans viendront avec eux (F24 à F30). Une
 * requête en échec : état d'erreur et « Réessayer » ; une 401 renvoie à la connexion
 * (intercepteur des pages d'administration).
 */
@Component({
  selector: 'app-dashboard-page',
  imports: [ErrorState],
  template: `
    <h1 class="text-2xl tracking-heading">Tableau de bord</h1>

    @if (failed()) {
      <app-error-state title="Le relevé n’a pas pu être chargé." (retry)="reload()" />
    } @else if (ready()) {
      <div class="mt-block grid max-w-prose gap-block">
        <section aria-labelledby="a-traiter">
          <h2 id="a-traiter" class="dashboard-heading">À traiter</h2>
          <dl class="dashboard-tally">
            <div class="dashboard-row">
              <dt>Messages non lus</dt>
              <dd>{{ unread.value()?.totalElements }}</dd>
            </div>
          </dl>
        </section>
        <section aria-labelledby="contenus">
          <h2 id="contenus" class="dashboard-heading">Contenus</h2>
          <dl class="dashboard-tally">
            @for (row of contents(); track row.label) {
              <div class="dashboard-row">
                <dt>{{ row.label }}</dt>
                <dd>{{ row.count }}</dd>
              </div>
            }
          </dl>
          <p class="mt-2 text-sm text-ink-muted">
            Publications, projets et séries&#8239;: tous statuts confondus, brouillons compris.
          </p>
        </section>
      </div>
    } @else {
      @defer (on timer(300ms)) {
        <p role="status" class="mt-block text-ink-muted">Chargement du relevé…</p>
      }
    }
  `,
  styles: `
    .dashboard-heading {
      font-size: var(--text-sm);
      font-weight: var(--font-weight-semibold);
      color: var(--color-ink-muted);
    }

    /* Relevé en cartouche (DS01 §4) : trait fort encre, lignes à filet, nombres à chasse fixe */
    .dashboard-tally {
      margin-block-start: calc(var(--spacing) * 2);
      border: var(--border-strong) solid var(--color-ink);
    }

    .dashboard-row {
      display: flex;
      align-items: baseline;
      justify-content: space-between;
      gap: calc(var(--spacing) * 6);
      padding: calc(var(--spacing) * 3) calc(var(--spacing) * 4);
    }

    .dashboard-row + .dashboard-row {
      border-top: var(--border-rule) solid var(--color-rule);
    }

    .dashboard-row dd {
      font-family: var(--font-code);
      font-size: var(--text-xl);
      font-variant-numeric: tabular-nums;
    }
  `,
})
export class DashboardPage {
  protected readonly unread = adminCountResource('/api/admin/contact-messages', { status: 'NEW' });
  private readonly publications = adminCountResource('/api/admin/publications');
  private readonly projects = adminCountResource('/api/admin/projects');
  private readonly series = adminCountResource('/api/admin/series');
  private readonly media = adminCountResource('/api/admin/media');

  private readonly all = [this.unread, this.publications, this.projects, this.series, this.media];

  protected readonly failed = computed(() => this.all.some((resource) => resource.error()));
  protected readonly ready = computed(() => this.all.every((resource) => resource.hasValue()));

  protected readonly contents = computed(() => [
    { label: 'Publications', count: this.publications.value()?.totalElements },
    { label: 'Projets', count: this.projects.value()?.totalElements },
    { label: 'Séries', count: this.series.value()?.totalElements },
    { label: 'Médias', count: this.media.value()?.totalElements },
  ]);

  protected reload(): void {
    for (const resource of this.all) {
      if (resource.error()) {
        resource.reload();
      }
    }
  }
}
