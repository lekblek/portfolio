import { Component, computed } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AdminContactMessageSummary, AdminPublicationSummary } from '../../../core/api/api-types';
import { formatDay } from '../../../shared/format/date';
import { Button } from '../../../shared/ui/button';
import { ErrorState } from '../../../shared/ui/error-state';
import { adminCountResource, adminLatestResource } from './data/dashboard.resources';

/**
 * Tableau de bord (`/admin`) : actions de création, relevé de ce qui attend l'administrateur
 * (messages non lus, brouillons) et des contenus enregistrés, tous statuts confondus, puis le
 * travail en cours (DS09) : les cinq derniers messages non lus et brouillons, liés à leur page. Chaque nombre vient du total d'une liste
 * d'administration et mène à cette liste, filtrée quand il le faut (messages nouveaux, brouillons). Une
 * requête en échec : état d'erreur et « Réessayer » ; une 401 renvoie à la connexion
 * (intercepteur des pages d'administration).
 */
/** Messages non lus et brouillons récents montrés (DS09). */
const LATEST = 5;

@Component({
  selector: 'app-dashboard-page',
  imports: [Button, ErrorState, RouterLink],
  template: `
    <div class="flex flex-wrap items-baseline justify-between gap-4">
      <h1 class="text-2xl tracking-heading">Tableau de bord</h1>
      <p class="flex flex-wrap gap-3">
        <a appButton routerLink="/admin/publications/new/article">Nouvel article</a>
        <a appButton variant="secondary" routerLink="/admin/projects/new">Nouveau projet</a>
      </p>
    </div>

    @if (failed()) {
      <app-error-state title="Le relevé n’a pas pu être chargé." (retry)="reload()" />
    } @else if (ready()) {
      <div class="dashboard mt-block">
        <div class="grid content-start gap-block">
          <section aria-labelledby="a-traiter">
            <h2 id="a-traiter" class="dashboard-heading">À traiter</h2>
            <dl class="dashboard-tally">
              @for (row of toDo(); track row.label) {
                <div class="dashboard-row">
                  <dt>
                    <a [routerLink]="row.path" [queryParams]="row.query">{{ row.label }}</a>
                  </dt>
                  <dd>{{ row.count }}</dd>
                </div>
              }
            </dl>
          </section>
          <section aria-labelledby="contenus">
            <h2 id="contenus" class="dashboard-heading">Contenus</h2>
            <dl class="dashboard-tally">
              @for (row of contents(); track row.label) {
                <div class="dashboard-row">
                  <dt>
                    <a [routerLink]="row.path">{{ row.label }}</a>
                  </dt>
                  <dd>{{ row.count }}</dd>
                </div>
              }
            </dl>
            <p class="mt-2 text-sm text-ink-muted">
              Publications, projets et séries&#8239;: tous statuts confondus, brouillons compris.
            </p>
          </section>
        </div>
        <div class="grid content-start gap-block">
          <section aria-labelledby="messages-recents">
            <h2 id="messages-recents" class="dashboard-heading">Messages non lus</h2>
            @if (unreadMessages().length > 0) {
              <ul class="dashboard-list">
                @for (message of unreadMessages(); track message.id) {
                  <li>
                    <a [routerLink]="['/admin/messages', message.id]">{{ message.subject }}</a>
                    <p class="text-sm text-ink-muted">
                      {{ message.name }}<span aria-hidden="true"> · </span
                      ><span class="sr-only">, </span>{{ day(message.createdAt) }}
                    </p>
                  </li>
                }
              </ul>
            } @else {
              <p class="mt-2 text-ink-muted">Aucun message non lu.</p>
            }
          </section>
          <section aria-labelledby="brouillons-recents">
            <h2 id="brouillons-recents" class="dashboard-heading">Brouillons récents</h2>
            @if (recentDrafts().length > 0) {
              <ul class="dashboard-list">
                @for (draft of recentDrafts(); track draft.id) {
                  <li>
                    <a [routerLink]="['/admin/publications', draft.id]">{{ draft.title }}</a>
                    <p class="text-sm text-ink-muted">
                      {{ draft.type === 'ARTICLE' ? 'Article' : 'Actualité'
                      }}<span aria-hidden="true"> · </span><span class="sr-only">, </span>modifié le
                      {{ day(draft.updatedAt) }}
                    </p>
                  </li>
                }
              </ul>
            } @else {
              <p class="mt-2 text-ink-muted">Aucun brouillon.</p>
            }
          </section>
        </div>
      </div>
    } @else {
      @defer (on timer(300ms)) {
        <p role="status" class="mt-block text-ink-muted">Chargement du relevé…</p>
      }
    }
  `,
  styles: `
    .dashboard {
      display: grid;
      gap: var(--spacing-block);
    }

    @media (min-width: 80rem) {
      .dashboard {
        grid-template-columns: minmax(0, 5fr) minmax(0, 6fr);
        column-gap: calc(var(--spacing) * 12);
      }
    }

    .dashboard-list {
      margin-block-start: calc(var(--spacing) * 2);
      border-top: var(--border-strong) solid var(--color-ink);
    }

    .dashboard-list > li {
      display: grid;
      gap: calc(var(--spacing) * 1);
      padding-block: calc(var(--spacing) * 3);
      border-bottom: var(--border-rule) solid var(--color-rule);
    }

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
  /** Messages non lus et brouillons : les cinq plus récents, et leur nombre. */
  protected readonly unread = adminLatestResource<AdminContactMessageSummary>(
    '/api/admin/contact-messages',
    { status: 'NEW' },
    LATEST,
  );
  private readonly drafts = adminLatestResource<AdminPublicationSummary>(
    '/api/admin/publications',
    { status: 'DRAFT' },
    LATEST,
  );
  private readonly publications = adminCountResource('/api/admin/publications');
  private readonly projects = adminCountResource('/api/admin/projects');
  private readonly series = adminCountResource('/api/admin/series');
  private readonly media = adminCountResource('/api/admin/media');

  private readonly all = [
    this.unread,
    this.drafts,
    this.publications,
    this.projects,
    this.series,
    this.media,
  ];

  protected readonly failed = computed(() => this.all.some((resource) => resource.error()));
  protected readonly ready = computed(() => this.all.every((resource) => resource.hasValue()));

  protected readonly toDo = computed(() => [
    {
      label: 'Messages non lus',
      path: '/admin/messages',
      query: { status: 'NEW' },
      count: this.unread.value()?.totalElements,
    },
    {
      label: 'Brouillons de publications',
      path: '/admin/publications',
      query: { status: 'DRAFT' },
      count: this.drafts.value()?.totalElements,
    },
  ]);

  protected readonly contents = computed(() => [
    {
      label: 'Publications',
      path: '/admin/publications',
      count: this.publications.value()?.totalElements,
    },
    { label: 'Projets', path: '/admin/projects', count: this.projects.value()?.totalElements },
    { label: 'Séries', path: '/admin/series', count: this.series.value()?.totalElements },
    { label: 'Médias', path: '/admin/media', count: this.media.value()?.totalElements },
  ]);

  protected readonly unreadMessages = computed(() => this.unread.value()?.content ?? []);
  protected readonly recentDrafts = computed(() => this.drafts.value()?.content ?? []);

  protected day(instant: string): string {
    return formatDay(instant);
  }

  protected reload(): void {
    for (const resource of this.all) {
      if (resource.error()) {
        resource.reload();
      }
    }
  }
}
