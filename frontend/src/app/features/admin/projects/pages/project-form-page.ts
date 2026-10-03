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
import { LiveAnnouncer } from '@angular/cdk/a11y';
import { Router, RouterLink } from '@angular/router';

import { adminMediaResource, mediaPickerPageResource } from '../../../../core/api/admin-media';
import { toApiError } from '../../../../core/api/api-error';
import { AdminMedia, AdminProject } from '../../../../core/api/api-types';
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
import { MultiSelect, MultiSelectOption } from '../../../../shared/ui/multi-select';
import {
  moveItem,
  SortableItem,
  SortableList,
  SortableMove,
} from '../../../../shared/ui/sortable-list';
import { StatusBadge } from '../../../../shared/ui/status-badge';
import { Toaster } from '../../../../shared/ui/toast';
import {
  EMPTY_PROJECT,
  ProjectModel,
  projectSchema,
  toModel,
  toRequest,
} from '../data/project-form';
import {
  createProject,
  projectResource,
  technologiesResource,
  updateProject,
} from '../data/projects';
import { ScreenshotPreview } from '../ui/screenshot-preview';

type FieldRef = ValidationError.WithFieldTree['fieldTree'];

/** Où va l'image choisie dans le sélecteur : la couverture, ou une nouvelle capture. */
type PickerTarget = 'cover' | 'screenshot';

/**
 * Création et modification d'un projet (`/admin/projects/new`, `/admin/projects/:id`, D-CX) :
 * toute la saisie en une page et un enregistrement, visibilité comprise. Slug figé dès qu'un projet
 * a été publié (D11), expliqué sous le champ. Avancement et période liés (invariant 21) : la date de
 * fin n'est demandée qu'à un projet terminé. Technologies par le choix multiple, couverture et
 * captures par le sélecteur de médias, captures ordonnables et légendées. Erreurs du serveur
 * rattachées à leur champ (`screenshots[1].caption` compris). Succès : notification et retour à la
 * liste ; quitter une saisie non enregistrée demande confirmation.
 */
@Component({
  selector: 'app-project-form-page',
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
    MultiSelect,
    RouterLink,
    ScreenshotPreview,
    SortableItem,
    SortableList,
    StatusBadge,
  ],
  host: { '(window:beforeunload)': 'warnBeforeUnload($event)' },
  templateUrl: './project-form-page.html',
  styleUrl: './project-form-page.css',
})
export class ProjectFormPage implements UnsavedChanges {
  /** Identifiant du projet à modifier (paramètre de route) ; absent à la création. */
  readonly id = input<string>();

  protected readonly editing = computed(() => this.id() !== undefined);
  protected readonly project = projectResource(this.id);
  protected readonly technologies = technologiesResource();
  protected readonly technologyOptions = computed<MultiSelectOption<number>[]>(() =>
    (this.technologies.value() ?? []).map((technology) => ({
      value: technology.id,
      label: technology.name,
    })),
  );

  private readonly model = signal<ProjectModel>({ ...EMPTY_PROJECT });
  /** Projet tel qu'enregistré : slug initial, verrou du slug, lien vers le site. */
  protected readonly saved = signal<AdminProject | null>(null);
  protected readonly slugLocked = computed(() => this.saved()?.slugLocked ?? false);

  protected readonly projectForm = form(
    this.model,
    (path) => {
      projectSchema(path);
      // D11 : le slug d'un projet déjà publié ne change plus
      readonly(path.slug, () => this.slugLocked());
    },
    {
      name: 'projet',
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
    if (!this.editing()) {
      return 'Nouveau projet';
    }
    return saved ? `Modifier le projet «\u00a0${saved.title}\u00a0»` : 'Modifier le projet';
  });
  protected readonly cover = adminMediaResource(() => this.model().coverMediaId);

  protected readonly pickerTarget = signal<PickerTarget>('cover');
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
  private readonly announcer = inject(LiveAnnouncer);
  private readonly injector = inject(Injector);
  private leaving = false;

  constructor() {
    // Modification : la saisie part du projet enregistré, une seule fois ; création : formulaire vide
    effect(() => {
      if (this.ready()) {
        return;
      }
      if (!this.editing()) {
        this.ready.set(true);
        afterNextRender(() => this.projectForm.title().focusBoundControl(), {
          injector: this.injector,
        });
      } else if (this.project.hasValue()) {
        this.saved.set(this.project.value());
        this.projectForm().reset(toModel(this.project.value()));
        this.ready.set(true);
      }
    });
  }

  protected openPicker(target: PickerTarget): void {
    this.pickerTarget.set(target);
    this.pickerPage.set(1);
    this.picker().open();
  }

  protected choose(media: AdminMedia): void {
    if (this.pickerTarget() === 'cover') {
      this.projectForm.coverMediaId().value.set(media.id);
      this.projectForm.coverMediaId().markAsDirty();
      return;
    }
    const screenshots = this.projectForm.screenshots;
    if (
      screenshots()
        .value()
        .some((screenshot) => screenshot.mediaId === media.id)
    ) {
      void this.announcer.announce(`${media.originalName} figure déjà parmi les captures.`);
      return;
    }
    screenshots().value.update((list) => [...list, { mediaId: media.id, caption: '' }]);
    screenshots().markAsDirty();
    const index = screenshots().value().length - 1;
    afterNextRender(
      () => this.host.querySelector<HTMLElement>(`#projet-capture-${index}-legende`)?.focus(),
      { injector: this.injector },
    );
  }

  protected clearCover(): void {
    this.projectForm.coverMediaId().value.set(null);
    this.projectForm.coverMediaId().markAsDirty();
  }

  protected moveScreenshot(move: SortableMove): void {
    this.projectForm.screenshots().value.update((list) => moveItem(list, move));
    this.projectForm.screenshots().markAsDirty();
  }

  protected removeScreenshot(index: number): void {
    this.projectForm.screenshots().value.update((list) => list.filter((_, i) => i !== index));
    this.projectForm.screenshots().markAsDirty();
    void this.announcer.announce(
      `Capture ${index + 1} retirée. Enregistrez le projet pour la retirer du site.`,
    );
    afterNextRender(
      () => {
        const remaining = this.projectForm.screenshots().value().length;
        const target =
          remaining > 0
            ? this.host.querySelector<HTMLElement>(
                `#projet-capture-${Math.min(index, remaining - 1)}-legende`,
              )
            : this.host.querySelector<HTMLElement>('[data-add-screenshot]');
        target?.focus();
      },
      { injector: this.injector },
    );
  }

  /** Avancement « en cours » : la date de fin n'a plus de sens, elle est vidée. */
  protected stageChanged(): void {
    if (this.model().stage === 'IN_PROGRESS') {
      this.projectForm.endDate().value.set('');
    }
  }

  protected screenshotName(index: number): string {
    return `la capture ${index + 1}`;
  }

  canLeave(): boolean | Promise<boolean> {
    if (this.leaving || !this.projectForm().dirty()) {
      return true;
    }
    return this.confirm().ask({
      title: 'Quitter sans enregistrer\u202f?',
      message: 'Les modifications de ce projet seront perdues.',
      confirmLabel: 'Quitter sans enregistrer',
      cancelLabel: 'Rester sur la page',
    });
  }

  protected warnBeforeUnload(event: BeforeUnloadEvent): void {
    if (!this.leaving && this.projectForm().dirty()) {
      event.preventDefault();
    }
  }

  private async save(): Promise<TreeValidationResult> {
    this.failure.set(null);
    const request = toRequest(this.model(), this.saved()?.slug ?? '');
    let saved: AdminProject;
    try {
      const id = this.saved()?.id;
      saved =
        id === undefined
          ? await createProject(this.http, request)
          : await updateProject(this.http, id, request);
    } catch (error) {
      return this.handleFailure(error);
    }
    this.leaving = true;
    this.toaster.show(
      `Projet «\u00a0${saved.title}\u00a0» ${this.editing() ? 'enregistré' : 'créé'}.`,
    );
    await this.router.navigate(['/admin/projects']);
    return undefined;
  }

  private handleFailure(error: unknown): TreeValidationResult {
    const fieldErrors = serverFieldErrors(error, this.fieldRefs(), {
      SLUG_LOCKED: {
        field: 'slug',
        message: 'Ce projet a déjà été publié\u202f: son slug ne peut plus changer.',
      },
    });
    if (fieldErrors.length > 0) {
      this.focusFirstError();
      return fieldErrors;
    }
    this.failure.set(
      toApiError(error).code === 'RESOURCE_NOT_FOUND'
        ? 'Ce projet n’existe plus\u202f: il a pu être supprimé entre-temps.'
        : 'Le serveur ne répond pas pour le moment. Votre saisie est conservée\u202f: réessayez dans quelques instants.',
    );
    return undefined;
  }

  /** Champs nommés comme dans les erreurs de l'API : `endDate`, `screenshots[1].caption`… */
  private fieldRefs(): Record<string, FieldRef> {
    const f = this.projectForm;
    const refs: Record<string, FieldRef> = {
      title: f.title,
      slug: f.slug,
      shortDescription: f.shortDescription,
      descriptionMarkdown: f.descriptionMarkdown,
      stage: f.stage,
      visibility: f.visibility,
      startDate: f.startDate,
      endDate: f.endDate,
      repositoryUrl: f.repositoryUrl,
      demoUrl: f.demoUrl,
      displayOrder: f.displayOrder,
      technologyIds: f.technologyIds,
      coverMediaId: f.coverMediaId,
      screenshots: f.screenshots,
    };
    this.model().screenshots.forEach((_, i) => {
      refs[`screenshots[${i}].caption`] = f.screenshots[i].caption;
    });
    return refs;
  }

  /** Après le rendu des erreurs : premier champ invalide dans l'ordre de la page. */
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
