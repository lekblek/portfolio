import {
  afterNextRender,
  Component,
  computed,
  DOCUMENT,
  ElementRef,
  inject,
  Injector,
  input,
  output,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';

import { AdminMedia } from '../../core/api/api-types';
import { formatFileSize } from '../format/file-size';
import { Button } from './button';
import { Icon } from './icon';

/** Sorte de média attendue par le champ : une image (avatar, couverture) ou un PDF (CV). */
export type MediaKind = 'image' | 'pdf';

/** État de la page de la médiathèque montrée par le sélecteur. */
export type MediaPickerState = 'loading' | 'ready' | 'error';

let nextId = 0;

/**
 * Sélecteur de médias (02-design-system §18) : dialogue modal qui montre une page de la médiathèque,
 * réduite à la sorte attendue, chaque média étant un bouton « Choisir … ». La page qui l'ouvre charge
 * les médias (`media`, `page`, `totalPages`, `state`) et reçoit le choix (`chosen`) ; `Échap` ou
 * « Annuler » renonce, le focus revient au bouton d'origine. Les médias s'envoient dans la
 * médiathèque, dont le lien est proposé (`libraryPath`).
 */
@Component({
  selector: 'app-media-picker',
  imports: [Button, Icon, RouterLink],
  template: `
    <dialog #dialog class="media-picker" [attr.aria-labelledby]="titleId" (close)="restoreFocus()">
      <h2 class="text-xl tracking-heading" [id]="titleId">{{ title() }}</h2>
      <p class="mt-2 text-sm text-ink-muted">
        {{ kind() === 'image' ? 'Images' : 'Documents PDF' }} de la médiathèque, les plus récents
        d’abord.
      </p>

      @switch (state()) {
        @case ('loading') {
          <p role="status" class="mt-block text-ink-muted">Chargement…</p>
        }
        @case ('error') {
          <div role="alert" class="mt-block">
            <p class="font-semibold">La médiathèque n’a pas pu être chargée.</p>
            <button appButton type="button" variant="secondary" class="mt-3" (click)="retry.emit()">
              Réessayer
            </button>
          </div>
        }
        @default {
          @if (choices().length === 0) {
            <p class="mt-block">
              {{ kind() === 'image' ? 'Aucune image' : 'Aucun PDF' }} sur cette page de la
              médiathèque.
            </p>
          } @else {
            <ul class="media-picker-list">
              @for (media of choices(); track media.id) {
                <li>
                  <button
                    type="button"
                    class="media-picker-choice"
                    [class.media-picker-current]="media.id === selectedId()"
                    [attr.aria-current]="media.id === selectedId() ? 'true' : null"
                    (click)="choose(media)"
                  >
                    @if (kind() === 'image') {
                      <img
                        class="media-picker-thumb"
                        [src]="media.url"
                        [width]="media.width"
                        [height]="media.height"
                        alt=""
                        loading="lazy"
                      />
                    } @else {
                      <span class="media-picker-thumb"><app-icon name="file-text" /></span>
                    }
                    <span class="min-w-0 text-start">
                      <span class="sr-only">Choisir </span>
                      <span class="block font-medium wrap-break-word"
                        >{{ media.originalName }}<span class="sr-only">, </span></span
                      >
                      <span class="block text-sm text-ink-muted">{{ details(media) }}</span>
                    </span>
                  </button>
                </li>
              }
            </ul>
          }
          @if (totalPages() > 1) {
            <div class="mt-4 flex flex-wrap gap-3">
              <button
                appButton
                type="button"
                variant="secondary"
                size="sm"
                [disabled]="page() <= 1"
                (click)="pageChange.emit(page() - 1)"
              >
                Plus récents
              </button>
              <button
                appButton
                type="button"
                variant="secondary"
                size="sm"
                [disabled]="page() >= totalPages()"
                (click)="pageChange.emit(page() + 1)"
              >
                Plus anciens
              </button>
            </div>
          }
        }
      }

      <div class="media-picker-footer">
        @if (libraryPath(); as path) {
          <a [routerLink]="path" (click)="close()">Envoyer un fichier dans la médiathèque</a>
        }
        <button #cancel appButton type="button" variant="secondary" (click)="close()">
          Annuler
        </button>
      </div>
    </dialog>
  `,
  styles: `
    .media-picker {
      width: min(calc(100% - 2 * var(--spacing-gutter)), calc(var(--spacing) * 160));
      max-height: calc(100dvh - 2 * var(--spacing-gutter));
      margin: auto;
      padding: calc(var(--spacing) * 6);
      border: var(--border-strong) solid var(--color-ink);
      border-radius: var(--radius-control);
      background: var(--color-paper);
      color: var(--color-ink);
      box-shadow: var(--shadow-overlay);
      overscroll-behavior: contain;
    }

    .media-picker::backdrop {
      background: var(--color-backdrop);
    }

    .media-picker-list {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(calc(var(--spacing) * 56), 1fr));
      gap: calc(var(--spacing) * 2);
      margin-block-start: var(--spacing-block);
    }

    .media-picker-choice {
      display: flex;
      align-items: center;
      gap: calc(var(--spacing) * 3);
      width: 100%;
      min-height: calc(var(--spacing) * 16);
      padding: calc(var(--spacing) * 2);
      border: var(--border-rule) solid var(--color-rule);
      border-radius: var(--radius-control);
      background: var(--color-paper);
      color: var(--color-ink);
      font: inherit;
      cursor: pointer;
    }

    .media-picker-choice:hover {
      border-color: var(--color-ink);
      background: var(--color-paper-sunken);
    }

    .media-picker-current {
      border: var(--border-strong) solid var(--color-accent);
    }

    .media-picker-thumb {
      display: inline-flex;
      flex: none;
      align-items: center;
      justify-content: center;
      width: calc(var(--spacing) * 16);
      height: calc(var(--spacing) * 12);
      object-fit: contain;
      border-radius: var(--radius-media);
      background: var(--color-paper-sunken);
      color: var(--color-ink-muted);
    }

    .media-picker-footer {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: calc(var(--spacing) * 3);
      margin-block-start: var(--spacing-block);
      padding-block-start: calc(var(--spacing) * 4);
      border-top: var(--border-rule) solid var(--color-rule);
    }

    @media (prefers-reduced-motion: no-preference) {
      .media-picker[open] {
        transition:
          opacity var(--duration-base) var(--ease-out),
          transform var(--duration-base) var(--ease-out);

        @starting-style {
          opacity: 0;
          transform: scale(0.97);
        }
      }
    }
  `,
})
export class MediaPicker {
  /** Titre du dialogue : « Choisir l’avatar ». */
  readonly title = input.required<string>();
  readonly kind = input.required<MediaKind>();
  /** Page courante de la médiathèque (tous formats) ; le sélecteur n'en montre que la sorte attendue. */
  readonly media = input<readonly AdminMedia[]>([]);
  readonly page = input(1);
  readonly totalPages = input(1);
  readonly state = input<MediaPickerState>('loading');
  /** Adresse de la médiathèque, où envoyer un nouveau fichier ; aucun lien si absente. */
  readonly libraryPath = input<string | null>(null);
  /** Média déjà choisi, repéré dans la liste. */
  readonly selectedId = input<number | null>(null);

  readonly chosen = output<AdminMedia>();
  readonly pageChange = output<number>();
  readonly retry = output<void>();

  protected readonly titleId = `selecteur-media-${++nextId}`;
  protected readonly choices = computed(() =>
    this.media().filter((media) => (media.format === 'PDF') === (this.kind() === 'pdf')),
  );

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private readonly cancel = viewChild.required<ElementRef<HTMLButtonElement>>('cancel');
  private readonly document = inject(DOCUMENT);
  private readonly injector = inject(Injector);
  private opener: HTMLElement | null = null;

  open(): void {
    this.opener = this.document.activeElement as HTMLElement | null;
    this.dialog().nativeElement.showModal();
    afterNextRender(
      () => {
        const first =
          this.dialog().nativeElement.querySelector<HTMLElement>('.media-picker-choice');
        (first ?? this.cancel().nativeElement).focus();
      },
      { injector: this.injector },
    );
  }

  close(): void {
    this.dialog().nativeElement.close();
  }

  protected choose(media: AdminMedia): void {
    this.chosen.emit(media);
    this.close();
  }

  protected restoreFocus(): void {
    if (this.opener?.isConnected) {
      this.opener.focus();
    }
    this.opener = null;
  }

  protected details(media: AdminMedia): string {
    const size = formatFileSize(media.sizeBytes);
    return media.width !== null && media.height !== null
      ? `${media.width}\u00a0×\u00a0${media.height} · ${size}`
      : size;
  }
}
