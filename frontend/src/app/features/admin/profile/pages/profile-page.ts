import { HttpClient } from '@angular/common/http';
import {
  afterNextRender,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  Injector,
  signal,
  viewChild,
} from '@angular/core';
import {
  form,
  FormField,
  FormRoot,
  TreeValidationResult,
  ValidationError,
} from '@angular/forms/signals';
import { RouterLink } from '@angular/router';

import { adminMediaResource, mediaPickerPageResource } from '../../../../core/api/admin-media';
import { toApiError } from '../../../../core/api/api-error';
import { AdminMedia } from '../../../../core/api/api-types';
import { serverFieldErrors } from '../../../../shared/forms/server-errors';
import { UnsavedChanges } from '../../../../shared/forms/unsaved-changes.guard';
import { visibleError } from '../../../../shared/forms/visible-error';
import { MarkdownView } from '../../../../shared/markdown/markdown-view';
import { Alert } from '../../../../shared/ui/alert';
import { Button } from '../../../../shared/ui/button';
import { ConfirmDialog } from '../../../../shared/ui/confirm-dialog';
import { ErrorState } from '../../../../shared/ui/error-state';
import { Field, FieldControl } from '../../../../shared/ui/field';
import { MediaKind, MediaPicker } from '../../../../shared/ui/media-picker';
import { MediaSlot } from '../../../../shared/ui/media-slot';
import { Toaster } from '../../../../shared/ui/toast';
import { adminProfileResource, saveProfile } from '../data/profile';
import {
  CertificationModel,
  EducationModel,
  EMPTY_PROFILE,
  keepUnchanged,
  ExperienceModel,
  LinkModel,
  newCertification,
  newEducation,
  newExperience,
  newLink,
  newSkill,
  ProfileModel,
  profileSchema,
  SkillModel,
  toModel,
  toRequest,
} from '../data/profile-form';
import { CollectionItem, ProfileCollection } from '../ui/profile-collection';

type FieldRef = ValidationError.WithFieldTree['fieldTree'];

const SECTIONS = [
  { id: 'profil-identite', label: 'Identité' },
  { id: 'profil-presentation', label: 'Présentation' },
  { id: 'profil-medias', label: 'Avatar et CV' },
  { id: 'profil-liens', label: 'Liens' },
  { id: 'profil-competences', label: 'Compétences' },
  { id: 'profil-experiences', label: 'Expériences' },
  { id: 'profil-formations', label: 'Formations' },
  { id: 'profil-certifications', label: 'Certifications' },
];

/** « le lien « GitHub » », ou « le lien n° 2 » tant que le nom n'est pas saisi. */
function named(text: string, noun: string, index: number): string {
  const name = text.trim();
  return name === '' ? `${noun} n°\u00a0${index + 1}` : `${noun} «\u00a0${name}\u00a0»`;
}

/**
 * Profil (`/admin/profile`, D-CY) : une seule page, un seul enregistrement qui remplace tout le
 * profil, collections comprises, dans l'ordre saisi (ordre d'affichage du site). Sections :
 * identité, présentation Markdown avec aperçu, avatar et CV (sélecteur de médias), puis les cinq
 * collections ordonnables. Erreurs du serveur rattachées à leur champ, y compris dans une
 * collection (`experiences[2].endDate`). Profil jamais enregistré (404) : formulaire vide.
 */
@Component({
  selector: 'app-profile-page',
  imports: [
    Alert,
    Button,
    CollectionItem,
    ConfirmDialog,
    ErrorState,
    Field,
    FieldControl,
    FormField,
    FormRoot,
    MarkdownView,
    MediaPicker,
    MediaSlot,
    ProfileCollection,
    RouterLink,
  ],
  host: { '(window:beforeunload)': 'warnBeforeUnload($event)' },
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.css',
})
export class ProfilePage implements UnsavedChanges {
  protected readonly sections = SECTIONS;
  protected readonly profile = adminProfileResource();
  /** Profil jamais enregistré : l'API répond 404, la saisie part de zéro. */
  protected readonly firstTime = computed(() => toApiError(this.profile.error()).status === 404);
  protected readonly failedToLoad = computed(
    () => this.profile.error() !== undefined && !this.firstTime(),
  );
  protected readonly ready = signal(false);

  private readonly model = signal<ProfileModel>({ ...EMPTY_PROFILE });
  protected readonly profileForm = form(this.model, profileSchema, {
    name: 'profil',
    submission: {
      action: () => this.save(),
      onInvalid: () => this.focusFirstError(),
    },
  });

  protected readonly preview = signal(false);
  protected readonly failure = signal<string | null>(null);

  protected readonly avatar = adminMediaResource(() => this.model().avatarMediaId);
  protected readonly cv = adminMediaResource(() => this.model().cvMediaId);

  /** Sélecteur de médias : sorte attendue et page affichée (aucune requête avant l'ouverture). */
  protected readonly pickerKind = signal<MediaKind>('image');
  protected readonly pickerPage = signal<number | null>(null);
  protected readonly pickerMedia = mediaPickerPageResource(this.pickerPage);
  protected readonly pickerState = computed(() =>
    this.pickerMedia.error() ? 'error' : this.pickerMedia.hasValue() ? 'ready' : 'loading',
  );

  protected readonly errorOf = visibleError;

  protected readonly linkName = (link: LinkModel, index: number) =>
    named(link.label, 'le lien', index);
  protected readonly skillName = (skill: SkillModel, index: number) =>
    named(skill.name, 'la compétence', index);
  protected readonly experienceName = (entry: ExperienceModel, index: number) =>
    named(entry.title, 'l’expérience', index);
  protected readonly educationName = (entry: EducationModel, index: number) =>
    named(entry.degree, 'la formation', index);
  protected readonly certificationName = (entry: CertificationModel, index: number) =>
    named(entry.name, 'la certification', index);
  protected readonly newLink = newLink;
  protected readonly newSkill = newSkill;
  protected readonly newExperience = newExperience;
  protected readonly newEducation = newEducation;
  protected readonly newCertification = newCertification;

  private readonly picker = viewChild.required<MediaPicker>('picker');
  private readonly confirm = viewChild.required<ConfirmDialog>('confirm');
  private readonly host: HTMLElement = inject(ElementRef).nativeElement;
  private readonly http = inject(HttpClient);
  private readonly toaster = inject(Toaster);
  private readonly injector = inject(Injector);

  constructor() {
    // La saisie part du profil enregistré (ou de rien, la première fois), une seule fois
    effect(() => {
      if (this.ready()) {
        return;
      }
      if (this.profile.hasValue()) {
        this.profileForm().reset(toModel(this.profile.value()));
        this.ready.set(true);
      } else if (this.firstTime()) {
        this.ready.set(true);
      }
    });
  }

  protected sectionLink(id: string): string {
    return `/admin/profile#${id}`;
  }

  protected openPicker(kind: MediaKind): void {
    this.pickerKind.set(kind);
    this.pickerPage.set(1);
    this.picker().open();
  }

  protected choose(media: AdminMedia): void {
    const key = this.pickerKind() === 'image' ? 'avatarMediaId' : 'cvMediaId';
    this.profileForm[key]().value.set(media.id);
    this.profileForm[key]().markAsDirty();
  }

  protected clear(key: 'avatarMediaId' | 'cvMediaId'): void {
    this.profileForm[key]().value.set(null);
    this.profileForm[key]().markAsDirty();
  }

  canLeave(): boolean | Promise<boolean> {
    if (!this.profileForm().dirty()) {
      return true;
    }
    return this.confirm().ask({
      title: 'Quitter sans enregistrer\u202f?',
      message: 'Les modifications du profil seront perdues.',
      confirmLabel: 'Quitter sans enregistrer',
      cancelLabel: 'Rester sur la page',
    });
  }

  protected warnBeforeUnload(event: BeforeUnloadEvent): void {
    if (this.profileForm().dirty()) {
      event.preventDefault();
    }
  }

  private async save(): Promise<TreeValidationResult> {
    this.failure.set(null);
    try {
      const saved = await saveProfile(this.http, toRequest(this.model()));
      this.profileForm().reset(keepUnchanged(this.model(), toModel(saved)));
    } catch (error) {
      const fieldErrors = serverFieldErrors(error, this.fieldRefs());
      if (fieldErrors.length > 0) {
        this.focusFirstError();
        return fieldErrors;
      }
      this.failure.set(
        'Le serveur ne répond pas pour le moment. Votre saisie est conservée\u202f: réessayez dans quelques instants.',
      );
      return undefined;
    }
    this.toaster.show('Profil enregistré.');
    return undefined;
  }

  /**
   * Champs nommés comme dans les erreurs de l'API : `displayName`, `skills`, `links[0].url`,
   * `experiences[2].endDate`…
   */
  private fieldRefs(): Record<string, FieldRef> {
    const form = this.profileForm;
    const refs: Record<string, FieldRef> = {
      displayName: form.displayName,
      professionalTitle: form.professionalTitle,
      shortBio: form.shortBio,
      aboutMarkdown: form.aboutMarkdown,
      publicLocation: form.publicLocation,
      publicEmail: form.publicEmail,
      avatarMediaId: form.avatarMediaId,
      cvMediaId: form.cvMediaId,
      skills: form.skills,
    };
    const model = this.model();
    model.links.forEach((_, i) => {
      refs[`links[${i}].label`] = form.links[i].label;
      refs[`links[${i}].url`] = form.links[i].url;
    });
    model.skills.forEach((_, i) => {
      refs[`skills[${i}].name`] = form.skills[i].name;
      refs[`skills[${i}].category`] = form.skills[i].category;
    });
    model.experiences.forEach((_, i) => {
      const entry = form.experiences[i];
      Object.assign(refs, {
        [`experiences[${i}].organization`]: entry.organization,
        [`experiences[${i}].title`]: entry.title,
        [`experiences[${i}].location`]: entry.location,
        [`experiences[${i}].startDate`]: entry.startDate,
        [`experiences[${i}].endDate`]: entry.endDate,
        [`experiences[${i}].description`]: entry.description,
      });
    });
    model.educations.forEach((_, i) => {
      const entry = form.educations[i];
      Object.assign(refs, {
        [`educations[${i}].institution`]: entry.institution,
        [`educations[${i}].degree`]: entry.degree,
        [`educations[${i}].field`]: entry.field,
        [`educations[${i}].location`]: entry.location,
        [`educations[${i}].startDate`]: entry.startDate,
        [`educations[${i}].endDate`]: entry.endDate,
        [`educations[${i}].description`]: entry.description,
      });
    });
    model.certifications.forEach((_, i) => {
      const entry = form.certifications[i];
      Object.assign(refs, {
        [`certifications[${i}].name`]: entry.name,
        [`certifications[${i}].issuer`]: entry.issuer,
        [`certifications[${i}].issuedAt`]: entry.issuedAt,
        [`certifications[${i}].expiresAt`]: entry.expiresAt,
        [`certifications[${i}].credentialUrl`]: entry.credentialUrl,
      });
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
