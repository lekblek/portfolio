import { Component, input, output } from '@angular/core';

import { AdminMedia } from '../../core/api/api-types';
import { formatFileSize } from '../format/file-size';
import { Button } from './button';
import { Icon } from './icon';
import { MediaKind } from './media-picker';

let nextId = 0;

/**
 * Emplacement d'un média (02-design-system §18) : avatar et CV du profil, couverture d'un projet ; le média choisi (aperçu, nom, poids) ou son
 * absence, et les actions « Choisir » ou « Changer », « Retirer ». Le choix se fait dans le
 * sélecteur de médias ouvert par la page ; une erreur du serveur (pas une image, pas un PDF)
 * s'affiche sous l'emplacement et décrit le bouton.
 */
@Component({
  selector: 'app-media-slot',
  imports: [Button, Icon],
  template: `
    <div class="media-slot">
      <p class="font-semibold" [id]="labelId">{{ label() }}</p>
      <p class="mt-1 text-sm text-ink-muted" [id]="hintId">{{ hint() }}</p>
      <div class="mt-3 flex flex-wrap items-center gap-4">
        @if (mediaId() === null) {
          <p class="text-ink-muted">Aucun média choisi.</p>
        } @else if (media(); as current) {
          @if (kind() === 'image') {
            <img
              class="media-slot-preview"
              [src]="current.url"
              [width]="current.width"
              [height]="current.height"
              [alt]="current.altText ?? ''"
            />
          } @else {
            <span class="media-slot-preview"><app-icon name="file-text" /></span>
          }
          <p class="min-w-0">
            <span class="block font-medium wrap-break-word">{{ current.originalName }}</span>
            <span class="block text-sm text-ink-muted">{{ size(current.sizeBytes) }}</span>
          </p>
        } @else {
          <p class="text-ink-muted">Média n°&#160;{{ mediaId() }}</p>
        }
      </div>
      <div class="mt-3 flex flex-wrap gap-3">
        <button
          appButton
          type="button"
          variant="secondary"
          size="sm"
          [attr.aria-describedby]="describedBy()"
          [attr.data-invalid]="error() ? 'true' : null"
          (click)="choose.emit()"
        >
          {{ mediaId() === null ? 'Choisir' : 'Changer'
          }}<span class="sr-only"> {{ subject() }}</span>
        </button>
        @if (mediaId() !== null) {
          <button appButton type="button" variant="quiet" size="sm" (click)="clear.emit()">
            Retirer<span class="sr-only"> {{ subject() }}</span>
          </button>
        }
      </div>
      @if (error()) {
        <p class="media-slot-error" [id]="errorId">
          <app-icon name="circle-alert" class="mt-0.5" />
          <span>{{ error() }}</span>
        </p>
      }
    </div>
  `,
  styles: `
    .media-slot {
      padding: calc(var(--spacing) * 4);
      border: var(--border-rule) solid var(--color-rule);
      border-radius: var(--radius-control);
    }

    .media-slot-preview {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: calc(var(--spacing) * 20);
      height: calc(var(--spacing) * 20);
      object-fit: cover;
      border-radius: var(--radius-media);
      background: var(--color-paper-sunken);
      color: var(--color-ink-muted);
    }

    .media-slot-error {
      display: flex;
      gap: calc(var(--spacing) * 2);
      margin-block-start: calc(var(--spacing) * 2);
      color: var(--color-danger);
      font-weight: var(--font-weight-medium);
    }
  `,
})
export class MediaSlot {
  readonly label = input.required<string>();
  readonly hint = input.required<string>();
  readonly kind = input.required<MediaKind>();
  /** Désignation pour les boutons : « l’avatar », « le CV ». */
  readonly subject = input.required<string>();
  readonly mediaId = input<number | null>(null);
  /** Caractéristiques du média choisi, une fois chargées. */
  readonly media = input<AdminMedia | undefined>(undefined);
  readonly error = input<string | null>(null);
  readonly choose = output<void>();
  readonly clear = output<void>();

  protected readonly labelId = `emplacement-${++nextId}`;
  protected readonly hintId = `${this.labelId}-aide`;
  protected readonly errorId = `${this.labelId}-erreur`;
  protected readonly size = formatFileSize;

  protected describedBy(): string {
    return this.error() ? `${this.errorId} ${this.hintId}` : this.hintId;
  }
}
