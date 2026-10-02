import { Component, DestroyRef, inject, Service, signal } from '@angular/core';

import { Icon } from './icon';
import { IconName } from './icons';

export type ToastTone = 'success' | 'danger';

export interface Toast {
  id: number;
  tone: ToastTone;
  message: string;
}

/** Une confirmation se retire seule ; un échec reste jusqu'à ce qu'on le ferme. */
const SUCCESS_DURATION_MS = 6000;

const ICON: Record<ToastTone, IconName> = {
  success: 'circle-check',
  danger: 'circle-alert',
};

/**
 * Notifications légères (02-design-system §18) : résultat d'une action qui ne mérite pas un
 * message dans la page (élément créé, enregistré, supprimé ; suppression refusée). Affichées par
 * `app-toast-region`, annoncées sans prendre le focus.
 */
@Service()
export class Toaster {
  private readonly items = signal<readonly Toast[]>([]);
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();
  private nextId = 0;

  readonly toasts = this.items.asReadonly();

  constructor() {
    inject(DestroyRef).onDestroy(() => this.timers.forEach((timer) => clearTimeout(timer)));
  }

  show(message: string, tone: ToastTone = 'success'): void {
    const id = ++this.nextId;
    this.items.update((items) => [...items, { id, tone, message }]);
    if (tone === 'success') {
      this.timers.set(
        id,
        setTimeout(() => this.dismiss(id), SUCCESS_DURATION_MS),
      );
    }
  }

  dismiss(id: number): void {
    clearTimeout(this.timers.get(id));
    this.timers.delete(id);
    this.items.update((items) => items.filter((item) => item.id !== id));
  }
}

/**
 * Région des notifications, en bas de l'écran : région `aria-live="polite"` présente dès le
 * départ (un lecteur d'écran annonce ce qui s'y ajoute), chaque notification fermable. Entrée en
 * fondu et en montant de 8 px, 200 ms, absente sous mouvement réduit ; retrait immédiat.
 */
@Component({
  selector: 'app-toast-region',
  imports: [Icon],
  host: { class: 'toast-region' },
  template: `
    <div aria-live="polite" class="toast-list">
      @for (toast of toaster.toasts(); track toast.id) {
        <div class="toast" [class.toast-danger]="toast.tone === 'danger'">
          <app-icon [name]="icon[toast.tone]" class="toast-icon" />
          <p class="min-w-0 flex-1">{{ toast.message }}</p>
          <button
            type="button"
            class="toast-close"
            aria-label="Fermer la notification"
            (click)="toaster.dismiss(toast.id)"
          >
            <app-icon name="close" />
          </button>
        </div>
      }
    </div>
  `,
  styles: `
    :host {
      position: fixed;
      inset-inline: var(--spacing-gutter);
      inset-block-end: var(--spacing-gutter);
      z-index: var(--z-toast);
      display: flex;
      justify-content: flex-end;
      pointer-events: none;
    }

    .toast-list {
      display: grid;
      gap: calc(var(--spacing) * 2);
      width: min(100%, calc(var(--spacing) * 100));
    }

    .toast {
      display: flex;
      align-items: flex-start;
      gap: calc(var(--spacing) * 3);
      padding: calc(var(--spacing) * 3) calc(var(--spacing) * 2) calc(var(--spacing) * 3)
        calc(var(--spacing) * 4);
      border: var(--border-rule) solid var(--color-ink);
      border-inline-start: calc(var(--spacing) * 1) solid var(--color-success);
      border-radius: var(--radius-control);
      background: var(--color-paper);
      box-shadow: var(--shadow-overlay);
      pointer-events: auto;
    }

    .toast-danger {
      border-inline-start-color: var(--color-danger);
    }

    .toast-icon {
      margin-block-start: 0.125rem;
      color: var(--color-success);
    }

    .toast-danger .toast-icon {
      color: var(--color-danger);
    }

    .toast-close {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: calc(var(--spacing) * 8);
      min-height: calc(var(--spacing) * 8);
      margin-block: calc(var(--spacing) * -1);
      border: 0;
      border-radius: var(--radius-control);
      background: transparent;
      color: var(--color-ink-muted);
      cursor: pointer;
    }

    .toast-close:hover {
      background: var(--color-paper-sunken);
      color: var(--color-ink);
    }

    @media (prefers-reduced-motion: no-preference) {
      .toast {
        transition:
          opacity var(--duration-base) var(--ease-out),
          transform var(--duration-base) var(--ease-out);

        @starting-style {
          opacity: 0;
          transform: translateY(calc(var(--spacing) * 2));
        }
      }
    }
  `,
})
export class ToastRegion {
  protected readonly toaster = inject(Toaster);
  protected readonly icon = ICON;
}
