import {
  afterNextRender,
  Component,
  DOCUMENT,
  ElementRef,
  inject,
  Injector,
  signal,
  viewChild,
} from '@angular/core';

import { Button } from './button';

/** Question posée avant une action destructive : ce qui va se passer et sur quel élément. */
export interface ConfirmRequest {
  /** Question qui nomme l'élément : « Supprimer la catégorie « Backend » ? ». */
  title: string;
  /** Conséquence de l'action, en une ou deux phrases. */
  message: string;
  /** Libellé du bouton de confirmation (variante `danger`) : le verbe de l'action. */
  confirmLabel: string;
  /** Libellé du bouton qui renonce ; « Annuler » par défaut. */
  cancelLabel?: string;
}

const CONFIRMED = 'confirmed';
let nextId = 0;

/**
 * Dialogue de confirmation (02-design-system §18) : `<dialog>` modal du navigateur (`showModal()` :
 * page inerte derrière, focus contenu, `Échap` renonce). Le focus va d'abord sur le bouton qui
 * renonce, l'action destructive est en variante `danger` ; à la fermeture, il revient à l'élément
 * qui avait ouvert le dialogue s'il existe encore. Entrée en fondu et à 97 %, 200 ms, absente
 * sous mouvement réduit ; fermeture immédiate.
 */
@Component({
  selector: 'app-confirm-dialog',
  imports: [Button],
  template: `
    <dialog
      #dialog
      class="confirm-dialog"
      [attr.aria-labelledby]="titleId"
      [attr.aria-describedby]="messageId"
      (close)="settle()"
    >
      @if (request(); as current) {
        <h2 class="text-xl tracking-heading" [id]="titleId">{{ current.title }}</h2>
        <p class="mt-3 max-w-prose" [id]="messageId">{{ current.message }}</p>
        <div class="confirm-dialog-actions">
          <button #cancel appButton type="button" variant="secondary" (click)="dismiss()">
            {{ current.cancelLabel ?? 'Annuler' }}
          </button>
          <button appButton type="button" variant="danger" (click)="confirm()">
            {{ current.confirmLabel }}
          </button>
        </div>
      }
    </dialog>
  `,
  styles: `
    .confirm-dialog {
      width: min(calc(100% - 2 * var(--spacing-gutter)), calc(var(--spacing) * 120));
      margin: auto;
      padding: calc(var(--spacing) * 6);
      border: var(--border-strong) solid var(--color-ink);
      border-radius: var(--radius-control);
      background: var(--color-paper);
      color: var(--color-ink);
      box-shadow: var(--shadow-overlay);
    }

    .confirm-dialog::backdrop {
      background: var(--color-backdrop);
    }

    .confirm-dialog-actions {
      display: flex;
      flex-wrap: wrap;
      justify-content: flex-end;
      gap: calc(var(--spacing) * 3);
      margin-block-start: var(--spacing-block);
    }

    @media (prefers-reduced-motion: no-preference) {
      .confirm-dialog[open] {
        transition:
          opacity var(--duration-base) var(--ease-out),
          transform var(--duration-base) var(--ease-out);

        @starting-style {
          opacity: 0;
          transform: scale(0.97);
        }
      }

      .confirm-dialog[open]::backdrop {
        transition: opacity var(--duration-base) var(--ease-out);

        @starting-style {
          opacity: 0;
        }
      }
    }
  `,
})
export class ConfirmDialog {
  protected readonly request = signal<ConfirmRequest | null>(null);
  protected readonly titleId = `confirmation-${++nextId}-titre`;
  protected readonly messageId = `confirmation-${nextId}-texte`;

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private readonly cancelButton = viewChild<ElementRef<HTMLButtonElement>>('cancel');
  private readonly document = inject(DOCUMENT);
  private readonly injector = inject(Injector);
  private opener: HTMLElement | null = null;
  private answer: ((confirmed: boolean) => void) | null = null;

  /** Ouvre le dialogue ; vrai si l'action est confirmée, faux si on y renonce (bouton, `Échap`). */
  ask(request: ConfirmRequest): Promise<boolean> {
    this.answer?.(false);
    this.opener = this.document.activeElement as HTMLElement | null;
    this.request.set(request);
    return new Promise((resolve) => {
      this.answer = resolve;
      // Après le rendu du titre et du message : le dialogue est annoncé avec eux
      afterNextRender(
        () => {
          const dialog = this.dialog().nativeElement;
          dialog.returnValue = '';
          dialog.showModal();
          this.cancelButton()?.nativeElement.focus();
        },
        { injector: this.injector },
      );
    });
  }

  protected confirm(): void {
    this.dialog().nativeElement.close(CONFIRMED);
  }

  protected dismiss(): void {
    this.dialog().nativeElement.close();
  }

  /** Fermeture, quelle qu'en soit la cause : réponse donnée, focus rendu. */
  protected settle(): void {
    const confirmed = this.dialog().nativeElement.returnValue === CONFIRMED;
    this.answer?.(confirmed);
    this.answer = null;
    if (this.opener?.isConnected) {
      this.opener.focus();
    }
    this.opener = null;
  }
}
