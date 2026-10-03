import {
  applyEach,
  email,
  maxLength,
  pattern,
  required,
  SchemaPath,
  SchemaPathTree,
  validate,
} from '@angular/forms/signals';

import { AdminProfile, SaveProfileRequest } from '../../../../core/api/api-types';

/*
 * Saisie du profil (D-CY) : la forme même de la requête, en chaînes pour les champs (date `AAAA-MM-JJ`,
 * vide pour une valeur absente) ; bornes et règles de `SaveProfileRequest` et d'`UpdateProfileUseCase`.
 */

export interface LinkModel {
  label: string;
  url: string;
}

export interface SkillModel {
  name: string;
  category: string;
}

export interface ExperienceModel {
  organization: string;
  title: string;
  location: string;
  startDate: string;
  endDate: string;
  description: string;
}

export interface EducationModel {
  institution: string;
  degree: string;
  field: string;
  location: string;
  startDate: string;
  endDate: string;
  description: string;
}

export interface CertificationModel {
  name: string;
  issuer: string;
  issuedAt: string;
  expiresAt: string;
  credentialUrl: string;
}

export interface ProfileModel {
  displayName: string;
  professionalTitle: string;
  shortBio: string;
  aboutMarkdown: string;
  publicLocation: string;
  publicEmail: string;
  avatarMediaId: number | null;
  cvMediaId: number | null;
  links: LinkModel[];
  skills: SkillModel[];
  experiences: ExperienceModel[];
  educations: EducationModel[];
  certifications: CertificationModel[];
}

/** Textes longs : présentation et descriptions. */
export const LONG_TEXT = 10_000;
const URL_FORMAT = /^https?:\/\/\S+$/i;
const URL_MESSAGE = 'Indiquez une adresse complète, par exemple https://exemple.fr.';

export const EMPTY_PROFILE: ProfileModel = {
  displayName: '',
  professionalTitle: '',
  shortBio: '',
  aboutMarkdown: '',
  publicLocation: '',
  publicEmail: '',
  avatarMediaId: null,
  cvMediaId: null,
  links: [],
  skills: [],
  experiences: [],
  educations: [],
  certifications: [],
};

export const newLink = (): LinkModel => ({ label: '', url: '' });
export const newSkill = (): SkillModel => ({ name: '', category: '' });
export const newExperience = (): ExperienceModel => ({
  organization: '',
  title: '',
  location: '',
  startDate: '',
  endDate: '',
  description: '',
});
export const newEducation = (): EducationModel => ({
  institution: '',
  degree: '',
  field: '',
  location: '',
  startDate: '',
  endDate: '',
  description: '',
});
export const newCertification = (): CertificationModel => ({
  name: '',
  issuer: '',
  issuedAt: '',
  expiresAt: '',
  credentialUrl: '',
});

/** Profil enregistré → saisie (valeurs absentes → texte vide). */
export function toModel(profile: AdminProfile): ProfileModel {
  return {
    displayName: profile.displayName,
    professionalTitle: profile.professionalTitle,
    shortBio: profile.shortBio,
    aboutMarkdown: profile.aboutMarkdown ?? '',
    publicLocation: profile.publicLocation ?? '',
    publicEmail: profile.publicEmail ?? '',
    avatarMediaId: profile.avatarMediaId,
    cvMediaId: profile.cvMediaId,
    links: profile.links.map((link) => ({ ...link })),
    skills: profile.skills.map((skill) => ({ ...skill })),
    experiences: profile.experiences.map((entry) => ({ ...entry, endDate: entry.endDate ?? '' })),
    educations: profile.educations.map((entry) => ({ ...entry, endDate: entry.endDate ?? '' })),
    certifications: profile.certifications.map((entry) => ({
      ...entry,
      expiresAt: entry.expiresAt ?? '',
      credentialUrl: entry.credentialUrl ?? '',
    })),
  };
}

type Collection = 'links' | 'skills' | 'experiences' | 'educations' | 'certifications';
const COLLECTIONS: readonly Collection[] = [
  'links',
  'skills',
  'experiences',
  'educations',
  'certifications',
];

/**
 * Profil enregistré, en gardant les éléments de liste inchangés de la saisie : les listes suivent
 * leurs éléments par identité, un objet neuf recréerait sa ligne (champs, focus).
 */
export function keepUnchanged(previous: ProfileModel, next: ProfileModel): ProfileModel {
  const merged = { ...next };
  for (const key of COLLECTIONS) {
    const before: readonly object[] = previous[key];
    (merged[key] as object[]) = next[key].map((item, index) =>
      sameFields(before[index], item) ? before[index] : item,
    );
  }
  return merged;
}

/** Mêmes champs, mêmes valeurs (les éléments de liste n'ont que des valeurs simples). */
function sameFields(a: object | undefined, b: object): boolean {
  if (a === undefined) {
    return false;
  }
  const entries = Object.entries(b) as [string, unknown][];
  return (
    Object.keys(a).length === entries.length &&
    entries.every(([name, value]) => (a as Record<string, unknown>)[name] === value)
  );
}

/** Texte rogné ; vide → absent (le serveur range un champ facultatif vide en `null`). */
function optional(value: string): string | undefined {
  const trimmed = value.trim();
  return trimmed === '' ? undefined : trimmed;
}

/** Saisie → requête : textes rognés, champs facultatifs vides omis, ordre des listes conservé. */
export function toRequest(model: ProfileModel): SaveProfileRequest {
  return {
    displayName: model.displayName.trim(),
    professionalTitle: model.professionalTitle.trim(),
    shortBio: model.shortBio.trim(),
    aboutMarkdown: optional(model.aboutMarkdown),
    publicLocation: optional(model.publicLocation),
    publicEmail: optional(model.publicEmail),
    avatarMediaId: model.avatarMediaId ?? undefined,
    cvMediaId: model.cvMediaId ?? undefined,
    links: model.links.map((link) => ({ label: link.label.trim(), url: link.url.trim() })),
    skills: model.skills.map((skill) => ({
      name: skill.name.trim(),
      category: skill.category.trim(),
    })),
    experiences: model.experiences.map((entry) => ({
      organization: entry.organization.trim(),
      title: entry.title.trim(),
      location: entry.location.trim(),
      startDate: entry.startDate,
      endDate: optional(entry.endDate),
      description: entry.description.trim(),
    })),
    educations: model.educations.map((entry) => ({
      institution: entry.institution.trim(),
      degree: entry.degree.trim(),
      field: entry.field.trim(),
      location: entry.location.trim(),
      startDate: entry.startDate,
      endDate: optional(entry.endDate),
      description: entry.description.trim(),
    })),
    certifications: model.certifications.map((entry) => ({
      name: entry.name.trim(),
      issuer: entry.issuer.trim(),
      issuedAt: entry.issuedAt,
      expiresAt: optional(entry.expiresAt),
      credentialUrl: optional(entry.credentialUrl),
    })),
  };
}

/** Champ obligatoire, refusé s'il n'est fait que d'espaces, borné. */
function text(path: SchemaPath<string>, max: number, label: string): void {
  required(path, { message: `Indiquez ${label}.` });
  validate(path, ({ value }) =>
    value().length > 0 && value().trim() === ''
      ? { kind: 'blank', message: `Indiquez ${label}.` }
      : undefined,
  );
  maxLength(path, max, { message: `${max} caractères au plus.` });
}

/** Fin d'une période ou expiration : jamais avant le début (invariant 17, D-I). */
function notBefore(end: SchemaPath<string>, start: SchemaPath<string>, message: string): void {
  validate(end, ({ value, valueOf }) => {
    const from = valueOf(start);
    return value() !== '' && from !== '' && value() < from ? { kind: 'order', message } : undefined;
  });
}

/** Règles de la saisie, alignées sur `SaveProfileRequest` et `UpdateProfileUseCase` (D-CY). */
export function profileSchema(path: SchemaPathTree<ProfileModel>): void {
  text(path.displayName, 120, 'le nom affiché');
  text(path.professionalTitle, 160, 'le titre professionnel');
  text(path.shortBio, 500, 'la présentation courte');
  maxLength(path.aboutMarkdown, LONG_TEXT, { message: `${LONG_TEXT} caractères au plus.` });
  maxLength(path.publicLocation, 120, { message: '120 caractères au plus.' });
  email(path.publicEmail, {
    message: 'Indiquez une adresse complète, par exemple nom@domaine.fr.',
  });
  maxLength(path.publicEmail, 255, { message: '255 caractères au plus.' });

  applyEach(path.links, (link) => {
    text(link.label, 80, 'le libellé');
    required(link.url, { message: 'Indiquez l’adresse.' });
    pattern(link.url, URL_FORMAT, { message: URL_MESSAGE });
    maxLength(link.url, 2048, { message: '2048 caractères au plus.' });
  });

  applyEach(path.skills, (skill) => {
    text(skill.name, 80, 'la compétence');
    text(skill.category, 60, 'la catégorie');
    // Nom en double, majuscules et minuscules confondues (D-CY)
    validate(skill.name, ({ value, valueOf }) => {
      const name = value().trim().toLowerCase();
      const same = valueOf(path.skills).filter((other) => other.name.trim().toLowerCase() === name);
      return name !== '' && same.length > 1
        ? { kind: 'duplicate', message: 'Cette compétence figure déjà dans la liste.' }
        : undefined;
    });
  });

  applyEach(path.experiences, (entry) => {
    text(entry.organization, 120, 'l’organisation');
    text(entry.title, 120, 'l’intitulé du poste');
    text(entry.location, 120, 'le lieu');
    required(entry.startDate, { message: 'Indiquez la date de début.' });
    notBefore(entry.endDate, entry.startDate, 'La fin précède le début.');
    maxLength(entry.description, LONG_TEXT, { message: `${LONG_TEXT} caractères au plus.` });
  });

  applyEach(path.educations, (entry) => {
    text(entry.institution, 160, 'l’établissement');
    text(entry.degree, 160, 'le diplôme');
    text(entry.field, 160, 'le domaine');
    text(entry.location, 120, 'le lieu');
    required(entry.startDate, { message: 'Indiquez la date de début.' });
    notBefore(entry.endDate, entry.startDate, 'La fin précède le début.');
    maxLength(entry.description, LONG_TEXT, { message: `${LONG_TEXT} caractères au plus.` });
  });

  applyEach(path.certifications, (entry) => {
    text(entry.name, 160, 'le nom de la certification');
    text(entry.issuer, 120, 'l’organisme');
    required(entry.issuedAt, { message: 'Indiquez la date de délivrance.' });
    notBefore(entry.expiresAt, entry.issuedAt, 'L’expiration précède la délivrance.');
    pattern(entry.credentialUrl, URL_FORMAT, { message: URL_MESSAGE });
    maxLength(entry.credentialUrl, 2048, { message: '2048 caractères au plus.' });
  });
}
