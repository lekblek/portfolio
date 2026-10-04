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
import { AdminMedia, AdminPublication, PublicationType } from '../../../../core/api/api-types';
import { ContentEditor } from '../../editor/content-editor';
import { serverFieldErrors } from '../../../../shared/forms/server-errors';
import { UnsavedChanges } from '../../../../shared/forms/unsaved-changes.guard';
import { visibleError } from '../../../../shared/forms/visible-error';
import { Alert } from '../../../../shared/ui/alert';
import { Button } from '../../../../shared/ui/button';
import { ConfirmDialog } from '../../../../shared/ui/confirm-dialog';
import { EmptyState } from '../../../../shared/ui/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state';
import { Field, FieldControl } from '../../../../shared/ui/field';
import { MediaPicker } from '../../../../shared/ui/media-picker';
import { MediaSlot } from '../../../../shared/ui/media-slot';
import { MultiSelect, MultiSelectOption } from '../../../../shared/ui/multi-select';
import { Toaster } from '../../../../shared/ui/toast';
import {
  CONTENT_MAX,
  EMPTY_PUBLICATION,
  PublicationModel,
  publicationSchema,
  SEO_DESCRIPTION_MAX,
  SEO_TITLE_MAX,
  toCreateRequest,
  toModel,
  toUpdateRequest,
} from '../data/publication-form';
import {
  categoriesResource,
  createPublication,
  publicationResource,
  publicPath,
  tagsResource,
  TYPE_LABELS,
  updatePublication,
} from '../data/publications';
import { transitionDone } from '../data/transitions';
import { PublicationStatusPanel } from '../ui/publication-status-panel';

type FieldRef = ValidationError.WithFieldTree['fieldTree'];

const COUNT = new Intl.NumberFormat('fr-FR');

/**
 * Création et modification d'une publication (`/admin/publications/new/article`, `…/new/news`,
 * `/admin/publications/:id`, D-CU) : métadonnées, contenu Markdown avec compteur et aperçu (le
 * moteur du site), classement, couverture, référencement. Une création donne un brouillon et ouvre
 * sa page ; l'enregistrement d'une modification reste sur la page (un texte se reprend souvent),
 * le statut se change dans son panneau (transitions permises seulement). Slug figé dès la première
 * publication (D11). Erreurs du serveur sous leur champ.
 */
@Component({
  selector: 'app-publication-form-page',
  imports: [
    Alert,
    Button,
    ConfirmDialog,
    ContentEditor,
    EmptyState,
    ErrorState,
    Field,
    FieldControl,
    FormField,
    FormRoot,
    MediaPicker,
    MediaSlot,
    MultiSelect,
    PublicationStatusPanel,
    RouterLink,
  ],
  host: { '(window:beforeunload)': 'warnBeforeUnload($event)' },
  templateUrl: './publication-form-page.html',
  styleUrl: './publication-form-page.css',
})
export class PublicationFormPage implements UnsavedChanges {
  /** Identifiant de la publication à modifier (paramètre de route) ; absent à la création. */
  readonly id = input<string>();
  /** Type de la publication à créer (donnée de route). */
  readonly createType = input<PublicationType>('ARTICLE');

  protected readonly editing = computed(() => this.id() !== undefined);
  protected readonly publication = publicationResource(this.id);
  protected readonly categories = categoriesResource();
  protected readonly tags = tagsResource();
  protected readonly tagOptions = computed<MultiSelectOption<number>[]>(() =>
    (this.tags.value() ?? []).map((tag) => ({ value: tag.id, label: tag.name })),
  );

  /** Publication telle qu'enregistrée : statut, verrou du slug, lien vers le site. */
  protected readonly saved = signal<AdminPublication | null>(null);
  protected readonly slugLocked = computed(() => this.saved()?.slugLocked ?? false);
  protected readonly type = computed(() => this.saved()?.type ?? this.createType());
  protected readonly article = computed(() => this.type() === 'ARTICLE');

  private readonly model = signal<PublicationModel>({ ...EMPTY_PUBLICATION });
  protected readonly publicationForm = form(
    this.model,
    (path) => {
      publicationSchema(path);
      // D11 : le slug d'une publication déjà publiée ne change plus
      readonly(path.slug, { when: () => this.slugLocked() });
    },
    {
      name: 'publication',
      submission: {
        action: () => this.save(),
        onInvalid: () => this.focusFirstError(),
      },
    },
  );

  protected readonly ready = signal(false);
  protected readonly failure = signal<string | null>(null);

  protected readonly title = computed(() => {
    const saved = this.saved();
    if (saved) {
      return `Modifier ${this.article() ? 'l’article' : 'l’actualité'} «\u00a0${saved.title}\u00a0»`;
    }
    if (this.editing()) {
      return 'Modifier la publication';
    }
    return this.article() ? 'Nouvel article' : 'Nouvelle actualité';
  });
  protected readonly typeLabel = computed(() => TYPE_LABELS[this.type()]);
  protected readonly sitePath = computed(() => {
    const saved = this.saved();
    return saved && saved.status === 'PUBLISHED' ? publicPath(saved.type, saved.slug) : null;
  });
  protected readonly contentCount = computed(
    () =>
      `${COUNT.format(this.publicationForm.contentMarkdown().value().length)} sur ${COUNT.format(CONTENT_MAX)} caractères.`,
  );
  protected readonly seoTitleCount = computed(
    () => `${this.publicationForm.seoTitle().value().length} sur ${SEO_TITLE_MAX} caractères.`,
  );
  protected readonly seoDescriptionCount = computed(
    () =>
      `${this.publicationForm.seoDescription().value().length} sur ${SEO_DESCRIPTION_MAX} caractères.`,
  );

  protected readonly cover = adminMediaResource(() => this.model().coverMediaId);
  protected readonly pickerPage = signal<number | null>(null);
  protected readonly pickerMedia = mediaPickerPageResource(this.pickerPage);
  protected readonly pickerState = computed(() =>
    this.pickerMedia.error() ? 'error' : this.pickerMedia.hasValue() ? 'ready' : 'loading',
  );

  protected readonly errorOf = visibleError;

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
      } else if (this.publication.hasValue()) {
        this.load(this.publication.value());
        this.ready.set(true);
      }
    });
  }

  protected openPicker(): void {
    this.pickerPage.set(1);
    this.picker().open();
  }

  protected choose(media: AdminMedia): void {
    this.publicationForm.coverMediaId().value.set(media.id);
    this.publicationForm.coverMediaId().markAsDirty();
  }

  protected clearCover(): void {
    this.publicationForm.coverMediaId().value.set(null);
    this.publicationForm.coverMediaId().markAsDirty();
  }

  /** Transition appliquée par le panneau : la saisie, inchangée, reste telle quelle. */
  protected statusChanged(updated: AdminPublication): void {
    this.saved.set(updated);
    this.toaster.show(
      `${this.article() ? 'Article' : 'Actualité'} «\u00a0${updated.title}\u00a0» ${transitionDone(updated.status)}.`,
    );
  }

  canLeave(): boolean | Promise<boolean> {
    if (this.leaving || !this.publicationForm().dirty()) {
      return true;
    }
    return this.confirm().ask({
      title: 'Quitter sans enregistrer\u202f?',
      message: 'Les modifications de cette publication seront perdues.',
      confirmLabel: 'Quitter sans enregistrer',
      cancelLabel: 'Rester sur la page',
    });
  }

  protected warnBeforeUnload(event: BeforeUnloadEvent): void {
    if (!this.leaving && this.publicationForm().dirty()) {
      event.preventDefault();
    }
  }

  private load(publication: AdminPublication): void {
    this.saved.set(publication);
    this.publicationForm().reset(toModel(publication));
  }

  private async save(): Promise<TreeValidationResult> {
    this.failure.set(null);
    const saved = this.saved();
    let result: AdminPublication;
    try {
      result = saved
        ? await updatePublication(this.http, saved.id, toUpdateRequest(this.model(), saved.slug))
        : await createPublication(this.http, toCreateRequest(this.model(), this.createType()));
    } catch (error) {
      return this.handleFailure(error);
    }
    const noun = this.article() ? 'Article' : 'Actualité';
    if (saved) {
      this.load(result);
      this.toaster.show(
        `${noun} «\u00a0${result.title}\u00a0» enregistré${this.article() ? '' : 'e'}.`,
      );
      return undefined;
    }
    // Création : le brouillon s'ouvre dans sa propre page, où se trouve son statut
    this.leaving = true;
    this.toaster.show(`Brouillon «\u00a0${result.title}\u00a0» créé.`);
    await this.router.navigate(['/admin/publications', result.id], { replaceUrl: true });
    return undefined;
  }

  private handleFailure(error: unknown): TreeValidationResult {
    const fieldErrors = serverFieldErrors(error, this.fieldRefs(), {
      SLUG_LOCKED: {
        field: 'slug',
        message: 'Cette publication a déjà été publiée\u202f: son slug ne peut plus changer.',
      },
    });
    if (fieldErrors.length > 0) {
      this.focusFirstError();
      return fieldErrors;
    }
    this.failure.set(
      toApiError(error).code === 'RESOURCE_NOT_FOUND'
        ? 'Cette publication n’existe plus.'
        : 'Le serveur ne répond pas pour le moment. Votre saisie est conservée\u202f: réessayez dans quelques instants.',
    );
    return undefined;
  }

  private fieldRefs(): Record<string, FieldRef> {
    const f = this.publicationForm;
    return {
      title: f.title,
      slug: f.slug,
      summary: f.summary,
      contentMarkdown: f.contentMarkdown,
      categoryId: f.categoryId,
      tagIds: f.tagIds,
      coverMediaId: f.coverMediaId,
      seoTitle: f.seoTitle,
      seoDescription: f.seoDescription,
    };
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
