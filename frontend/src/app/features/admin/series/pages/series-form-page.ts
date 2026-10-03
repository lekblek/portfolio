import { HttpClient } from '@angular/common/http';
import {
  afterNextRender,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  Injector,
  input,
  signal,
  viewChild,
} from '@angular/core';
import {
  form,
  FormField,
  FormRoot,
  readonly,
  TreeValidationResult,
  ValidationError,
} from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';

import { adminMediaResource, mediaPickerPageResource } from '../../../../core/api/admin-media';
import { toApiError } from '../../../../core/api/api-error';
import { AdminMedia, AdminSeries } from '../../../../core/api/api-types';
import { serverFieldErrors } from '../../../../shared/forms/server-errors';
import { UnsavedChanges } from '../../../../shared/forms/unsaved-changes.guard';
import { visibleError } from '../../../../shared/forms/visible-error';
import { MarkdownView } from '../../../../shared/markdown/markdown-view';
import { Alert } from '../../../../shared/ui/alert';
import { Button } from '../../../../shared/ui/button';
import { ConfirmDialog } from '../../../../shared/ui/confirm-dialog';
import { EmptyState } from '../../../../shared/ui/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state';
import { Field, FieldControl } from '../../../../shared/ui/field';
import { MediaPicker } from '../../../../shared/ui/media-picker';
import { MediaSlot } from '../../../../shared/ui/media-slot';
import { Toaster } from '../../../../shared/ui/toast';
import {
  articlesResource,
  createSeries,
  EMPTY_SERIES,
  SeriesModel,
  seriesResource,
  seriesSchema,
  toModel,
  toRequest,
  updateSeries,
} from '../data/series';
import { SeriesChapters } from '../ui/series-chapters';

type FieldRef = ValidationError.WithFieldTree['fieldTree'];

/**
 * Création et modification d'une série (`/admin/series/new`, `/admin/series/:id`, D-CV) : titre,
 * slug (figé dès qu'un de ses articles a été public), description Markdown avec aperçu,
 * couverture ; puis les chapitres, enregistrés à part (la liste remplace l'ancienne). Une création
 * ouvre la page de la série, où s'ajoutent ses chapitres ; une modification reste sur la page.
 */
@Component({
  selector: 'app-series-form-page',
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
    MarkdownView,
    MediaPicker,
    MediaSlot,
    RouterLink,
    SeriesChapters,
  ],
  host: { '(window:beforeunload)': 'warnBeforeUnload($event)' },
  template: `
    <p class="text-sm font-semibold text-ink-muted">
      <a class="font-medium" routerLink="/admin/series">Séries</a>
    </p>
    <h1 class="mt-1 text-2xl tracking-heading">{{ title() }}</h1>

    @if (editing() && seriesData.error()) {
      @if (seriesData.statusCode() === 404) {
        <app-empty-state
          class="mt-block block"
          message="Cette série n’existe pas ou n’existe plus."
        >
          <a routerLink="/admin/series">Retour à la liste</a>
        </app-empty-state>
      } @else {
        <app-error-state
          class="mt-block block"
          [title]="'La série n’a pas pu être chargée.'"
          (retry)="seriesData.reload()"
        />
      }
    } @else if (!ready()) {
      @defer (on timer(300ms)) {
        <p role="status" class="mt-block text-ink-muted">Chargement…</p>
      }
    } @else {
      <form class="series-form" [formRoot]="seriesForm">
        <app-field label="Titre" controlId="serie-titre" [error]="errorOf(seriesForm.title)">
          <input appFieldControl type="text" autocomplete="off" [formField]="seriesForm.title" />
        </app-field>
        <app-field
          label="Slug"
          controlId="serie-slug"
          optional
          [hint]="
            slugLocked()
              ? 'Figé : un article de cette série a déjà été public.'
              : 'Identifiant dans l’adresse de la série. Vide : tiré du titre ; figé dès qu’un de ses articles est public.'
          "
          [error]="errorOf(seriesForm.slug)"
        >
          <input
            appFieldControl
            type="text"
            autocomplete="off"
            autocapitalize="none"
            spellcheck="false"
            translate="no"
            [formField]="seriesForm.slug"
          />
        </app-field>
        <app-field
          label="Description"
          controlId="serie-description"
          optional
          hint="En Markdown, en tête de la page de la série. 10 000 caractères au plus."
          [error]="errorOf(seriesForm.descriptionMarkdown)"
        >
          <textarea
            appFieldControl
            rows="6"
            autocomplete="off"
            class="font-code"
            [formField]="seriesForm.descriptionMarkdown"
          ></textarea>
        </app-field>
        <div>
          <button
            appButton
            type="button"
            variant="secondary"
            size="sm"
            aria-controls="serie-apercu"
            [attr.aria-expanded]="preview()"
            (click)="preview.set(!preview())"
          >
            {{ preview() ? 'Masquer l’aperçu' : 'Afficher l’aperçu' }}
          </button>
        </div>
        <div id="serie-apercu" class="series-preview" [hidden]="!preview()">
          @if (preview()) {
            @if (seriesForm.descriptionMarkdown().value().trim(); as source) {
              <app-markdown-view [source]="source" [headingLevel]="3" />
            } @else {
              <p class="text-ink-muted">La description est vide.</p>
            }
          }
        </div>
        <app-media-slot
          label="Couverture"
          hint="Une image de la médiathèque, sur la page de la série et dans la liste des séries."
          kind="image"
          subject="la couverture"
          [mediaId]="seriesForm.coverMediaId().value()"
          [media]="cover.value()"
          [error]="errorOf(seriesForm.coverMediaId)"
          (choose)="openPicker()"
          (clear)="clearCover()"
        />
        @if (failure(); as text) {
          <app-alert tone="danger" title="L’enregistrement a échoué">
            <p>{{ text }}</p>
          </app-alert>
        }
        <div class="flex flex-wrap gap-3">
          <button appButton type="submit" [loading]="seriesForm().submitting()">
            {{ saved() ? 'Enregistrer la série' : 'Créer la série' }}
          </button>
          <a appButton variant="secondary" routerLink="/admin/series">Retour à la liste</a>
        </div>
      </form>

      @if (saved(); as current) {
        <app-series-chapters
          #chapters
          class="series-chapters"
          [series]="current"
          [articles]="articles.value()?.content ?? []"
          (saved)="chaptersSaved($event)"
        />
      } @else {
        <p class="series-chapters text-sm text-ink-muted">
          Les chapitres s’ajoutent une fois la série créée.
        </p>
      }
    }

    <app-media-picker
      #picker
      title="Choisir la couverture"
      kind="image"
      [media]="pickerMedia.value()?.content ?? []"
      [page]="pickerPage() ?? 1"
      [totalPages]="pickerMedia.value()?.totalPages ?? 1"
      [state]="pickerState()"
      [selectedId]="seriesForm.coverMediaId().value()"
      libraryPath="/admin/media"
      (chosen)="choose($event)"
      (pageChange)="pickerPage.set($event)"
      (retry)="pickerMedia.reload()"
    />
    <app-confirm-dialog #confirm />
  `,
  styles: `
    .series-form {
      display: grid;
      gap: calc(var(--spacing) * 6);
      max-width: var(--container-prose);
      margin-block-start: var(--spacing-block);
    }

    .series-chapters {
      display: block;
      max-width: var(--container-prose);
      margin-block-start: var(--spacing-section);
      padding-block-start: var(--spacing-block);
      border-top: var(--border-strong) solid var(--color-ink);
    }

    .series-preview {
      padding: calc(var(--spacing) * 4) calc(var(--spacing) * 5);
      border: var(--border-rule) solid var(--color-rule);
      border-radius: var(--radius-control);
      background: var(--color-paper-sunken);
    }
  `,
})
export class SeriesFormPage implements UnsavedChanges {
  readonly id = input<string>();

  protected readonly editing = computed(() => this.id() !== undefined);
  protected readonly seriesData = seriesResource(this.id);
  protected readonly articles = articlesResource();

  protected readonly saved = signal<AdminSeries | null>(null);
  protected readonly slugLocked = computed(() => this.saved()?.slugLocked ?? false);

  private readonly model = signal<SeriesModel>({ ...EMPTY_SERIES });
  protected readonly seriesForm = form(
    this.model,
    (path) => {
      seriesSchema(path);
      readonly(path.slug, { when: () => this.slugLocked() });
    },
    {
      name: 'serie',
      submission: {
        action: () => this.save(),
        onInvalid: () => this.focusFirstError(),
      },
    },
  );

  protected readonly ready = signal(false);
  protected readonly preview = signal(false);
  protected readonly failure = signal<string | null>(null);
  protected readonly title = computed(() => {
    const saved = this.saved();
    if (saved) {
      return `Modifier la série «\u00a0${saved.title}\u00a0»`;
    }
    return this.editing() ? 'Modifier la série' : 'Nouvelle série';
  });

  protected readonly cover = adminMediaResource(() => this.model().coverMediaId);
  protected readonly pickerPage = signal<number | null>(null);
  protected readonly pickerMedia = mediaPickerPageResource(this.pickerPage);
  protected readonly pickerState = computed(() =>
    this.pickerMedia.error() ? 'error' : this.pickerMedia.hasValue() ? 'ready' : 'loading',
  );

  protected readonly errorOf = visibleError;

  private readonly chapters = viewChild<SeriesChapters>('chapters');
  private readonly picker = viewChild.required<MediaPicker>('picker');
  private readonly confirm = viewChild.required<ConfirmDialog>('confirm');
  private readonly host: HTMLElement = inject(ElementRef).nativeElement;
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly toaster = inject(Toaster);
  private readonly injector = inject(Injector);
  private leaving = false;

  constructor() {
    effect(() => {
      if (this.ready()) {
        return;
      }
      if (!this.editing()) {
        this.ready.set(true);
      } else if (this.seriesData.hasValue()) {
        this.load(this.seriesData.value());
        this.ready.set(true);
      }
    });
  }

  protected openPicker(): void {
    this.pickerPage.set(1);
    this.picker().open();
  }

  protected choose(media: AdminMedia): void {
    this.seriesForm.coverMediaId().value.set(media.id);
    this.seriesForm.coverMediaId().markAsDirty();
  }

  protected clearCover(): void {
    this.seriesForm.coverMediaId().value.set(null);
    this.seriesForm.coverMediaId().markAsDirty();
  }

  protected chaptersSaved(updated: AdminSeries): void {
    this.saved.set(updated);
    const count = updated.chapters.length;
    this.toaster.show(
      `Chapitres de «\u00a0${updated.title}\u00a0» enregistrés (${count} ${count > 1 ? 'chapitres' : 'chapitre'}).`,
    );
  }

  canLeave(): boolean | Promise<boolean> {
    const dirty = this.seriesForm().dirty() || (this.chapters()?.dirty() ?? false);
    if (this.leaving || !dirty) {
      return true;
    }
    return this.confirm().ask({
      title: 'Quitter sans enregistrer\u202f?',
      message: 'Les modifications de cette série (ou de ses chapitres) seront perdues.',
      confirmLabel: 'Quitter sans enregistrer',
      cancelLabel: 'Rester sur la page',
    });
  }

  protected warnBeforeUnload(event: BeforeUnloadEvent): void {
    if (!this.leaving && (this.seriesForm().dirty() || (this.chapters()?.dirty() ?? false))) {
      event.preventDefault();
    }
  }

  private load(series: AdminSeries): void {
    this.saved.set(series);
    this.seriesForm().reset(toModel(series));
  }

  private async save(): Promise<TreeValidationResult> {
    this.failure.set(null);
    const saved = this.saved();
    let result: AdminSeries;
    try {
      result = saved
        ? await updateSeries(this.http, saved.id, toRequest(this.model(), saved.slug))
        : await createSeries(this.http, toRequest(this.model(), ''));
    } catch (error) {
      return this.handleFailure(error);
    }
    if (saved) {
      this.load(result);
      this.toaster.show(`Série «\u00a0${result.title}\u00a0» enregistrée.`);
      return undefined;
    }
    this.leaving = true;
    this.toaster.show(`Série «\u00a0${result.title}\u00a0» créée : ajoutez ses chapitres.`);
    await this.router.navigate(['/admin/series', result.id], { replaceUrl: true });
    return undefined;
  }

  private handleFailure(error: unknown): TreeValidationResult {
    const f = this.seriesForm;
    const fields: Record<string, FieldRef> = {
      title: f.title,
      slug: f.slug,
      descriptionMarkdown: f.descriptionMarkdown,
      coverMediaId: f.coverMediaId,
    };
    const fieldErrors = serverFieldErrors(error, fields, {
      SLUG_LOCKED: {
        field: 'slug',
        message:
          'Un article de cette série a déjà été public\u202f: son slug ne peut plus changer.',
      },
    });
    if (fieldErrors.length > 0) {
      this.focusFirstError();
      return fieldErrors;
    }
    this.failure.set(
      toApiError(error).code === 'RESOURCE_NOT_FOUND'
        ? 'Cette série n’existe plus.'
        : 'Le serveur ne répond pas pour le moment. Votre saisie est conservée\u202f: réessayez dans quelques instants.',
    );
    return undefined;
  }

  private focusFirstError(): void {
    afterNextRender(
      () =>
        this.host
          .querySelector<HTMLElement>('[aria-invalid="true"], [data-invalid="true"]')
          ?.focus(),
      { injector: this.injector },
    );
  }
}
