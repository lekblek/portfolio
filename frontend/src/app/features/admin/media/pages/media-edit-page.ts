import { HttpClient } from '@angular/common/http';
import {
  afterNextRender,
  Component,
  computed,
  effect,
  inject,
  Injector,
  input,
  signal,
  viewChild,
} from '@angular/core';
import {
  FieldTree,
  form,
  FormField,
  FormRoot,
  maxLength,
  required,
  TreeValidationResult,
} from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';

import { toApiError } from '../../../../core/api/api-error';
import { AdminMedia } from '../../../../core/api/api-types';
import { serverFieldErrors } from '../../../../shared/forms/server-errors';
import { UnsavedChanges } from '../../../../shared/forms/unsaved-changes.guard';
import { formatDayTime } from '../../../../shared/format/date';
import { formatFileSize } from '../../../../shared/format/file-size';
import { Alert } from '../../../../shared/ui/alert';
import { Button } from '../../../../shared/ui/button';
import { ConfirmDialog } from '../../../../shared/ui/confirm-dialog';
import { EmptyState } from '../../../../shared/ui/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state';
import { Field, FieldControl } from '../../../../shared/ui/field';
import { Toaster } from '../../../../shared/ui/toast';
import { isImage, mediaResource, updateAltText } from '../data/media';

const ALT_TEXT_MAX = 300;

const FORMAT_LABEL: Record<AdminMedia['format'], string> = {
  PNG: 'Image PNG',
  JPEG: 'Image JPEG',
  WEBP: 'Image WebP',
  PDF: 'Document PDF',
};

/**
 * Page d'un média (`/admin/media/:id`) : aperçu, caractéristiques (format, dimensions, poids, date,
 * adresse publique) et texte alternatif, seule donnée modifiable (D-CT). Une image doit en avoir
 * un (règle d'interface, WCAG 1.1.1) ; un PDF n'en a pas : le lien qui le propose le nomme.
 */
@Component({
  selector: 'app-media-edit-page',
  imports: [
    Alert,
    Button,
    ConfirmDialog,
    EmptyState,
    ErrorState,
    Field,
    FieldControl,
    FormField,
    FormRoot,
    RouterLink,
  ],
  host: { '(window:beforeunload)': 'warnBeforeUnload($event)' },
  template: `
    <p class="text-sm font-semibold text-ink-muted">
      <a class="font-medium" routerLink="/admin/media">Médias</a>
    </p>
    <h1 class="mt-1 text-2xl tracking-heading wrap-break-word">{{ title() }}</h1>

    @if (media.error(); as error) {
      @if (missing()) {
        <app-empty-state class="mt-block block" message="Ce média n’existe pas ou n’existe plus.">
          <a routerLink="/admin/media">Retour à la médiathèque</a>
        </app-empty-state>
      } @else {
        <app-error-state [title]="'Le média n’a pas pu être chargé.'" (retry)="media.reload()" />
      }
    } @else if (media.hasValue()) {
      @let current = media.value();
      <div class="mt-block grid gap-block lg:grid-cols-12 lg:gap-8">
        <div class="min-w-0 lg:col-span-7">
          @if (image()) {
            <img
              class="media-preview"
              [src]="current.url"
              [width]="current.width"
              [height]="current.height"
              [alt]="current.altText ?? ''"
            />
          } @else {
            <p>
              <a [href]="current.url"
                >Ouvrir le PDF<span class="sr-only"> {{ current.originalName }}</span></a
              >
            </p>
          }
        </div>
        <dl class="media-facts lg:col-span-5">
          <div>
            <dt>Format</dt>
            <dd>{{ formatLabel(current) }}</dd>
          </div>
          @if (current.width !== null && current.height !== null) {
            <div>
              <dt>Dimensions</dt>
              <dd class="tabular-nums">
                {{ current.width }}&#160;×&#160;{{ current.height }} pixels
              </dd>
            </div>
          }
          <div>
            <dt>Poids</dt>
            <dd>{{ size(current.sizeBytes) }}</dd>
          </div>
          <div>
            <dt>Envoyé le</dt>
            <dd>{{ when(current.createdAt) }}</dd>
          </div>
          <div>
            <dt>Adresse publique</dt>
            <dd class="font-code text-sm break-all" translate="no">{{ current.url }}</dd>
          </div>
        </dl>
      </div>

      @if (image()) {
        <form class="mt-section grid max-w-prose gap-6" [formRoot]="altForm">
          <app-field
            label="Texte alternatif"
            controlId="media-texte-alternatif"
            hint="Ce que montre l’image, pour qui ne la voit pas, en une phrase. 300 caractères au plus."
            [error]="errorOf(altForm.altText)"
          >
            <textarea
              appFieldControl
              rows="3"
              autocomplete="off"
              [formField]="altForm.altText"
            ></textarea>
          </app-field>
          @if (failure(); as text) {
            <app-alert tone="danger" title="L’enregistrement a échoué">
              <p>{{ text }}</p>
            </app-alert>
          }
          <div class="flex flex-wrap gap-3">
            <button appButton type="submit" [loading]="altForm().submitting()">
              Enregistrer le texte alternatif
            </button>
            <a appButton variant="secondary" routerLink="/admin/media">Annuler</a>
          </div>
        </form>
      } @else {
        <p class="mt-block max-w-prose text-ink-muted">
          Un PDF n’a pas de texte alternatif&#8239;: le lien qui le propose dit ce qu’il contient.
        </p>
      }
    } @else {
      @defer (on timer(300ms)) {
        <p role="status" class="mt-block text-ink-muted">Chargement…</p>
      }
    }

    <app-confirm-dialog #confirm />
  `,
  styles: `
    .media-preview {
      width: 100%;
      height: auto;
      max-height: 70vh;
      object-fit: contain;
      border: var(--border-rule) solid var(--color-rule);
      border-radius: var(--radius-media);
      background: var(--color-paper-sunken);
    }

    .media-facts {
      display: grid;
      align-content: start;
      border-top: var(--border-strong) solid var(--color-ink);
    }

    .media-facts > div {
      padding-block: calc(var(--spacing) * 3);
      border-bottom: var(--border-rule) solid var(--color-rule);
    }

    .media-facts dt {
      color: var(--color-ink-muted);
      font-size: var(--text-sm);
      font-weight: var(--font-weight-semibold);
    }
  `,
})
export class MediaEditPage implements UnsavedChanges {
  /** Identifiant du média (paramètre de route). */
  readonly id = input.required<string>();

  protected readonly media = mediaResource(() => this.id());
  protected readonly size = formatFileSize;
  protected readonly when = formatDayTime;
  protected readonly image = computed(() => this.media.hasValue() && isImage(this.media.value()));
  protected readonly missing = computed(() => toApiError(this.media.error()).status === 404);
  protected readonly title = computed(() =>
    this.media.hasValue() ? `Média «\u00a0${this.media.value().originalName}\u00a0»` : 'Média',
  );

  private readonly model = signal({ altText: '' });
  private saved = false;

  protected readonly altForm = form(
    this.model,
    (path) => {
      required(path.altText, {
        message:
          'Décrivez l’image\u202f: une image sans texte alternatif est muette pour un lecteur d’écran.',
      });
      maxLength(path.altText, ALT_TEXT_MAX, {
        message: `Le texte alternatif compte ${ALT_TEXT_MAX} caractères au plus.`,
      });
    },
    {
      name: 'media',
      submission: {
        action: () => this.save(),
        onInvalid: () => this.focusAltText(),
      },
    },
  );

  protected readonly failure = signal<string | null>(null);

  private readonly confirm = viewChild.required<ConfirmDialog>('confirm');
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly toaster = inject(Toaster);
  private readonly injector = inject(Injector);

  constructor() {
    let filled = false;
    effect(() => {
      if (this.media.hasValue() && !filled) {
        filled = true;
        this.altForm().reset({ altText: this.media.value().altText ?? '' });
      }
    });
  }

  protected formatLabel(media: AdminMedia): string {
    return FORMAT_LABEL[media.format];
  }

  protected errorOf(field: FieldTree<string>): string | null {
    const state = field();
    return state.touched() ? (state.errors()[0]?.message ?? null) : null;
  }

  canLeave(): boolean | Promise<boolean> {
    if (this.saved || !this.altForm().dirty()) {
      return true;
    }
    return this.confirm().ask({
      title: 'Quitter sans enregistrer\u202f?',
      message: 'Le texte alternatif saisi sera perdu.',
      confirmLabel: 'Quitter sans enregistrer',
      cancelLabel: 'Rester sur la page',
    });
  }

  protected warnBeforeUnload(event: BeforeUnloadEvent): void {
    if (!this.saved && this.altForm().dirty()) {
      event.preventDefault();
    }
  }

  private async save(): Promise<TreeValidationResult> {
    this.failure.set(null);
    const media = this.media.value();
    if (!media) {
      return undefined;
    }
    try {
      await updateAltText(this.http, media.id, this.model().altText.trim());
    } catch (error) {
      const fieldErrors = serverFieldErrors(error, { altText: this.altForm.altText });
      if (fieldErrors.length > 0) {
        this.focusAltText();
        return fieldErrors;
      }
      this.failure.set(
        toApiError(error).status === 404
          ? 'Ce média n’existe plus\u202f: il a pu être supprimé entre-temps.'
          : 'Le serveur ne répond pas pour le moment. Votre saisie est conservée\u202f: réessayez dans quelques instants.',
      );
      return undefined;
    }
    this.saved = true;
    this.toaster.show(`Texte alternatif de «\u00a0${media.originalName}\u00a0» enregistré.`);
    await this.router.navigateByUrl('/admin/media');
    return undefined;
  }

  private focusAltText(): void {
    afterNextRender(() => this.altForm.altText().focusBoundControl(), {
      injector: this.injector,
    });
  }
}
