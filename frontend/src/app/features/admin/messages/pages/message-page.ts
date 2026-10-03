import { HttpClient } from '@angular/common/http';
import {
  afterNextRender,
  Component,
  computed,
  ElementRef,
  inject,
  Injector,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { toApiError } from '../../../../core/api/api-error';
import { AdminContactMessage } from '../../../../core/api/api-types';
import { formatDayTime } from '../../../../shared/format/date';
import { Alert } from '../../../../shared/ui/alert';
import { Button } from '../../../../shared/ui/button';
import { ConfirmDialog } from '../../../../shared/ui/confirm-dialog';
import { EmptyState } from '../../../../shared/ui/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state';
import { StatusBadge } from '../../../../shared/ui/status-badge';
import { Toaster } from '../../../../shared/ui/toast';
import {
  changeMessageStatus,
  deleteMessage,
  forwardFrom,
  MessageTransition,
  messageResource,
  replyLink,
} from '../data/messages';

/**
 * Un message de contact (`/admin/messages/:id`, D-CZ) : sujet, expéditeur, date, texte tel
 * qu'écrit (jamais interprété), statut et les statuts suivants (le cycle n'avance que vers
 * l'avant ; lire ne change rien). « Répondre » ouvre la messagerie. Suppression définitive, à la
 * demande de la personne (F30), confirmée par un dialogue qui nomme l'expéditeur.
 */
@Component({
  selector: 'app-message-page',
  imports: [Alert, Button, ConfirmDialog, EmptyState, ErrorState, RouterLink, StatusBadge],
  template: `
    <p class="text-sm font-semibold text-ink-muted">
      <a class="font-medium" routerLink="/admin/messages">Messages</a>
    </p>

    @if (message.error()) {
      @if (message.statusCode() === 404) {
        <h1 class="mt-1 text-2xl tracking-heading">Message introuvable</h1>
        <app-empty-state
          class="mt-block block"
          message="Ce message n’existe pas ou a été supprimé."
        >
          <a routerLink="/admin/messages">Retour aux messages</a>
        </app-empty-state>
      } @else {
        <h1 class="mt-1 text-2xl tracking-heading">Message</h1>
        <app-error-state
          class="mt-block block"
          [title]="'Le message n’a pas pu être chargé.'"
          (retry)="message.reload()"
        />
      }
    } @else if (current(); as item) {
      <h1 class="mt-1 text-2xl tracking-heading" #heading tabindex="-1">{{ item.subject }}</h1>
      <dl class="message-facts">
        <div>
          <dt>De</dt>
          <dd>
            {{ item.name }}
            <span class="text-ink-muted">&lt;{{ item.email }}&gt;</span>
          </dd>
        </div>
        <div>
          <dt>Reçu le</dt>
          <dd>{{ dayTime(item.createdAt) }}</dd>
        </div>
        <div>
          <dt>Statut</dt>
          <dd><app-status-badge [status]="item.status" /></dd>
        </div>
      </dl>

      <section class="message-body" aria-label="Texte du message">
        <p>{{ item.message }}</p>
      </section>

      <div class="message-actions">
        <a appButton [href]="reply(item)"
          >Répondre<span class="sr-only"> à {{ item.name }} par courriel</span></a
        >
        @for (transition of transitions(); track transition.target) {
          <button
            appButton
            type="button"
            variant="secondary"
            [loading]="pending() === transition.target"
            [disabled]="pending() !== null"
            (click)="advance(transition)"
          >
            {{ transition.label }}
          </button>
        }
      </div>

      @if (failure(); as text) {
        <app-alert class="mt-4 block" tone="danger" title="L’action a échoué">
          <p>{{ text }}</p>
        </app-alert>
      }

      <section class="message-danger" aria-labelledby="message-suppression">
        <h2 id="message-suppression" class="font-semibold">Supprimer le message</h2>
        <p class="mt-1 max-w-prose text-sm text-ink-muted">
          À faire quand la personne demande l’effacement de ses données. Pour simplement ranger un
          message, archivez-le.
        </p>
        <button appButton type="button" variant="danger" class="mt-3" (click)="remove(item)">
          Supprimer définitivement
        </button>
      </section>
    } @else {
      @defer (on timer(300ms)) {
        <p role="status" class="mt-block text-ink-muted">Chargement…</p>
      }
    }

    <app-confirm-dialog #confirm />
  `,
  styles: `
    .message-facts {
      display: grid;
      gap: calc(var(--spacing) * 2);
      margin-block-start: calc(var(--spacing) * 4);
      font-size: var(--text-sm);
    }

    .message-facts > div {
      display: grid;
      grid-template-columns: calc(var(--spacing) * 20) minmax(0, 1fr);
      gap: calc(var(--spacing) * 3);
      align-items: center;
    }

    .message-facts dt {
      color: var(--color-ink-muted);
    }

    .message-facts dd {
      overflow-wrap: anywhere;
    }

    .message-body {
      max-width: var(--container-prose);
      margin-block-start: var(--spacing-block);
      padding: calc(var(--spacing) * 5);
      border: var(--border-rule) solid var(--color-rule);
      border-radius: var(--radius-control);
      background: var(--color-paper-sunken);
      font-family: var(--font-text);
      line-height: var(--leading-prose);
      white-space: pre-wrap;
      overflow-wrap: anywhere;
    }

    .message-actions {
      display: flex;
      flex-wrap: wrap;
      gap: calc(var(--spacing) * 3);
      margin-block-start: var(--spacing-block);
    }

    .message-danger {
      max-width: var(--container-prose);
      margin-block-start: var(--spacing-section);
      padding-block-start: var(--spacing-block);
      border-top: var(--border-rule) solid var(--color-rule);
    }
  `,
})
export class MessagePage {
  readonly id = input.required<string>();

  protected readonly message = messageResource(this.id);
  /** Message affiché : la réponse de l'API, puis celle de chaque changement de statut. */
  private readonly updated = signal<AdminContactMessage | null>(null);
  protected readonly current = computed(() =>
    this.message.hasValue() ? (this.updated() ?? this.message.value()) : null,
  );
  protected readonly transitions = computed(() => {
    const current = this.current();
    return current ? forwardFrom(current.status) : [];
  });
  protected readonly pending = signal<string | null>(null);
  protected readonly failure = signal<string | null>(null);
  protected readonly dayTime = formatDayTime;

  private readonly confirm = viewChild.required<ConfirmDialog>('confirm');
  private readonly heading = viewChild<ElementRef<HTMLElement>>('heading');
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly toaster = inject(Toaster);
  private readonly injector = inject(Injector);

  protected reply(message: AdminContactMessage): string {
    return replyLink(message);
  }

  protected async advance(transition: MessageTransition): Promise<void> {
    const current = this.current();
    if (!current) {
      return;
    }
    this.failure.set(null);
    this.pending.set(transition.target);
    try {
      this.updated.set(await changeMessageStatus(this.http, current.id, transition.target));
      this.toaster.show(`Message de ${current.name} : ${this.doneText(transition)}.`);
      afterNextRender(() => this.heading()?.nativeElement.focus(), { injector: this.injector });
    } catch (error) {
      this.failure.set(
        toApiError(error).code === 'INVALID_CONTACT_MESSAGE_TRANSITION'
          ? 'Ce statut n’est plus possible : le message a changé entre-temps. Rechargez la page.'
          : 'Le serveur ne répond pas pour le moment : réessayez dans quelques instants.',
      );
    } finally {
      this.pending.set(null);
    }
  }

  protected async remove(message: AdminContactMessage): Promise<void> {
    const confirmed = await this.confirm().ask({
      title: `Supprimer le message de ${message.name}\u202f?`,
      message:
        'Le message, le nom et l’adresse de son expéditeur seront effacés définitivement. Cette action ne peut pas être annulée.',
      confirmLabel: 'Supprimer définitivement',
    });
    if (!confirmed) {
      return;
    }
    this.failure.set(null);
    try {
      await deleteMessage(this.http, message.id);
    } catch (error) {
      if (toApiError(error).status !== 404) {
        this.failure.set('La suppression a échoué : réessayez dans quelques instants.');
        return;
      }
    }
    this.toaster.show(`Message de ${message.name} supprimé définitivement.`);
    await this.router.navigate(['/admin/messages']);
  }

  private doneText(transition: MessageTransition): string {
    switch (transition.target) {
      case 'READ':
        return 'marqué comme lu';
      case 'PROCESSED':
        return 'marqué comme traité';
      case 'ARCHIVED':
        return 'archivé';
      default:
        return 'marqué comme nouveau';
    }
  }
}
