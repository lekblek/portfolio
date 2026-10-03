import { HttpClient } from '@angular/common/http';
import {
  afterNextRender,
  Component,
  computed,
  ElementRef,
  inject,
  Injector,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';

import { toApiError } from '../../../../core/api/api-error';
import { AdminPublication } from '../../../../core/api/api-types';
import { formatDayTime, instantFromSiteTime, siteTimeInput } from '../../../../shared/format/date';
import { Alert } from '../../../../shared/ui/alert';
import { Button } from '../../../../shared/ui/button';
import { ConfirmDialog } from '../../../../shared/ui/confirm-dialog';
import { Field, FieldControl } from '../../../../shared/ui/field';
import { StatusBadge } from '../../../../shared/ui/status-badge';
import { changePublicationStatus } from '../data/publications';
import { Transition, transitionsFrom } from '../data/transitions';

/**
 * Statut d'une publication (F28, D-AV) : statut observable et date, puis les seules transitions
 * permises depuis ce statut. Programmer demande une date et une heure futures (heure de Paris,
 * celle des affichages) ; archiver retire la publication du site, après confirmation. Une saisie
 * non enregistrée bloque les transitions : le statut s'applique au contenu enregistré.
 */
@Component({
  selector: 'app-publication-status-panel',
  imports: [Alert, Button, ConfirmDialog, Field, FieldControl, StatusBadge],
  template: `
    <section class="status-panel" aria-labelledby="statut-titre">
      <h2 id="statut-titre" #heading class="text-xl tracking-heading" tabindex="-1">Statut</h2>
      <p class="mt-3 flex flex-wrap items-center gap-x-3 gap-y-1">
        <app-status-badge [status]="publication().status" [feminine]="true" />
        @if (dateText(); as text) {
          <span class="text-sm text-ink-muted">{{ text }}</span>
        }
      </p>

      @if (blocked()) {
        <p class="mt-3 text-sm text-ink-muted">
          Enregistrez d’abord vos modifications&#8239;: le statut s’applique au contenu enregistré.
        </p>
      }

      @if (scheduling(); as transition) {
        <form class="mt-4 grid gap-4" novalidate (submit)="schedule($event, transition)">
          <app-field
            label="Date et heure de publication"
            controlId="publication-programmation"
            hint="Heure de Paris. La publication paraît d’elle-même à ce moment."
            [error]="dateError()"
          >
            <input
              appFieldControl
              type="datetime-local"
              [min]="minimum()"
              [value]="when()"
              (input)="when.set($any($event.target).value)"
            />
          </app-field>
          <div class="flex flex-wrap gap-3">
            <button appButton type="submit" [loading]="busy()">Programmer</button>
            <button appButton type="button" variant="secondary" (click)="scheduling.set(null)">
              Annuler
            </button>
          </div>
        </form>
      } @else {
        <div class="mt-4 flex flex-wrap gap-3">
          @for (transition of transitions(); track transition.label; let first = $first) {
            <button
              appButton
              type="button"
              [variant]="first ? 'primary' : 'secondary'"
              [disabled]="blocked()"
              [loading]="busy() && pending() === transition"
              (click)="choose(transition)"
            >
              {{ transition.label }}
            </button>
          }
        </div>
      }

      @if (failure(); as text) {
        <app-alert class="mt-4 block" tone="danger" title="Le statut n’a pas changé">
          <p>{{ text }}</p>
        </app-alert>
      }
    </section>
    <app-confirm-dialog #confirm />
  `,
  styles: `
    .status-panel {
      padding: calc(var(--spacing) * 5);
      border: var(--border-strong) solid var(--color-ink);
      border-radius: var(--radius-control);
    }
  `,
})
export class PublicationStatusPanel {
  readonly publication = input.required<AdminPublication>();
  /** Saisie non enregistrée : transitions désactivées. */
  readonly blocked = input(false);
  /** Publication après la transition, à reprendre par la page. */
  readonly changed = output<AdminPublication>();

  protected readonly transitions = computed(() => transitionsFrom(this.publication().status));
  protected readonly dateText = computed(() => {
    const { status, publishedAt } = this.publication();
    if (publishedAt === null) {
      return null;
    }
    switch (status) {
      case 'SCHEDULED':
        return `Paraîtra le ${formatDayTime(publishedAt)}`;
      case 'PUBLISHED':
        return `Publiée le ${formatDayTime(publishedAt)}`;
      case 'ARCHIVED':
        return `Publiée le ${formatDayTime(publishedAt)}, retirée du site`;
      default:
        return null;
    }
  });

  protected readonly scheduling = signal<Transition | null>(null);
  protected readonly when = signal('');
  protected readonly dateError = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly pending = signal<Transition | null>(null);
  protected readonly failure = signal<string | null>(null);
  protected readonly minimum = signal('');

  private readonly confirm = viewChild.required<ConfirmDialog>('confirm');
  private readonly heading = viewChild.required<ElementRef<HTMLElement>>('heading');
  private readonly host: HTMLElement = inject(ElementRef).nativeElement;
  private readonly http = inject(HttpClient);
  private readonly injector = inject(Injector);

  protected async choose(transition: Transition): Promise<void> {
    this.failure.set(null);
    if (transition.needsDate) {
      const now = new Date().toISOString();
      this.minimum.set(siteTimeInput(now));
      const current = this.publication();
      this.when.set(
        current.status === 'SCHEDULED' && current.publishedAt !== null
          ? siteTimeInput(current.publishedAt)
          : '',
      );
      this.dateError.set(null);
      this.scheduling.set(transition);
      afterNextRender(
        () => this.host.querySelector<HTMLElement>('#publication-programmation')?.focus(),
        { injector: this.injector },
      );
      return;
    }
    if (transition.confirm) {
      const title = this.publication().title;
      const confirmed = await this.confirm().ask({
        title: `Archiver «\u00a0${title}\u00a0»\u202f?`,
        message:
          'La publication quitte le site (sa page répond « introuvable »). Elle reste ici et peut être republiée.',
        confirmLabel: 'Archiver',
      });
      if (!confirmed) {
        return;
      }
    }
    await this.apply(transition);
  }

  protected async schedule(event: Event, transition: Transition): Promise<void> {
    event.preventDefault();
    const value = this.when();
    if (value === '') {
      this.dateError.set('Indiquez la date et l’heure de publication.');
    } else if (new Date(instantFromSiteTime(value)).getTime() <= Date.now()) {
      this.dateError.set('Choisissez un moment à venir.');
    } else {
      this.dateError.set(null);
      await this.apply(transition, instantFromSiteTime(value));
      return;
    }
    afterNextRender(
      () => this.host.querySelector<HTMLElement>('#publication-programmation')?.focus(),
      { injector: this.injector },
    );
  }

  private async apply(transition: Transition, publishedAt?: string): Promise<void> {
    this.busy.set(true);
    this.pending.set(transition);
    try {
      const updated = await changePublicationStatus(
        this.http,
        this.publication().id,
        transition.target,
        publishedAt,
      );
      this.scheduling.set(null);
      this.changed.emit(updated);
      afterNextRender(() => this.heading().nativeElement.focus(), { injector: this.injector });
    } catch (error) {
      this.failure.set(
        toApiError(error).code === 'INVALID_PUBLICATION_TRANSITION'
          ? 'Ce changement n’est plus possible depuis le statut actuel (la date est peut-être passée, ou la publication a changé entre-temps). Rechargez la page.'
          : 'Le serveur ne répond pas pour le moment : réessayez dans quelques instants.',
      );
    } finally {
      this.busy.set(false);
      this.pending.set(null);
    }
  }
}
