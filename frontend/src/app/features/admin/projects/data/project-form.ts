import {
  applyEach,
  maxLength,
  pattern,
  required,
  SchemaPath,
  SchemaPathTree,
  validate,
} from '@angular/forms/signals';

import {
  AdminProject,
  ProjectStage,
  ProjectVisibility,
  SaveProjectRequest,
} from '../../../../core/api/api-types';

/** Bornes du contrat (`SaveProjectRequest`, `Project`, `ProjectScreenshot`). */
export const TITLE_MAX = 160;
export const SHORT_DESCRIPTION_MAX = 500;
export const DESCRIPTION_MAX = 100_000;
export const URL_MAX = 2048;
export const CAPTION_MAX = 300;

const HAS_LETTER_OR_DIGIT = /[\p{L}\p{N}]/u;
const SLUG_FORMAT = /^[a-z0-9]+(-[a-z0-9]+)*$/;
const URL_FORMAT = /^https?:\/\/\S+$/i;
const WHOLE_NUMBER = /^\d{1,9}$/;

export interface ScreenshotModel {
  mediaId: number;
  caption: string;
}

/** Saisie d'un projet : textes tels que tapés, dates `AAAA-MM-JJ` (vide : aucune). */
export interface ProjectModel {
  title: string;
  slug: string;
  shortDescription: string;
  descriptionMarkdown: string;
  stage: ProjectStage;
  visibility: ProjectVisibility;
  startDate: string;
  endDate: string;
  repositoryUrl: string;
  demoUrl: string;
  featured: boolean;
  displayOrder: string;
  technologyIds: number[];
  coverMediaId: number | null;
  screenshots: ScreenshotModel[];
}

export const EMPTY_PROJECT: ProjectModel = {
  title: '',
  slug: '',
  shortDescription: '',
  descriptionMarkdown: '',
  stage: 'IN_PROGRESS',
  visibility: 'DRAFT',
  startDate: '',
  endDate: '',
  repositoryUrl: '',
  demoUrl: '',
  featured: false,
  displayOrder: '',
  technologyIds: [],
  coverMediaId: null,
  screenshots: [],
};

/** Projet enregistré → saisie. */
export function toModel(project: AdminProject): ProjectModel {
  return {
    title: project.title,
    slug: project.slug,
    shortDescription: project.shortDescription,
    descriptionMarkdown: project.descriptionMarkdown,
    stage: project.stage,
    visibility: project.visibility,
    startDate: project.startDate,
    endDate: project.endDate ?? '',
    repositoryUrl: project.repositoryUrl ?? '',
    demoUrl: project.demoUrl ?? '',
    featured: project.featured,
    displayOrder: String(project.displayOrder),
    technologyIds: [...project.technologyIds],
    coverMediaId: project.coverMediaId,
    screenshots: project.screenshots.map((screenshot) => ({
      mediaId: screenshot.mediaId,
      caption: screenshot.caption ?? '',
    })),
  };
}

function optional(value: string): string | undefined {
  const trimmed = value.trim();
  return trimmed === '' ? undefined : trimmed;
}

/**
 * Saisie → requête : textes rognés, champs facultatifs vides omis ; slug envoyé seulement s'il a
 * changé (absent : généré à la création, conservé à la modification) ; date de fin seulement pour
 * un projet terminé (invariant 21) ; captures dans l'ordre de la liste.
 */
export function toRequest(model: ProjectModel, initialSlug: string): SaveProjectRequest {
  const slug = model.slug.trim();
  return {
    title: model.title.trim(),
    slug: slug !== '' && slug !== initialSlug ? slug : undefined,
    shortDescription: model.shortDescription.trim(),
    descriptionMarkdown: model.descriptionMarkdown,
    stage: model.stage,
    visibility: model.visibility,
    startDate: model.startDate,
    endDate: model.stage === 'COMPLETED' ? optional(model.endDate) : undefined,
    repositoryUrl: optional(model.repositoryUrl),
    demoUrl: optional(model.demoUrl),
    featured: model.featured,
    displayOrder: model.displayOrder.trim() === '' ? 0 : Number(model.displayOrder.trim()),
    technologyIds: [...model.technologyIds],
    coverMediaId: model.coverMediaId ?? undefined,
    screenshots: model.screenshots.map((screenshot) => ({
      mediaId: screenshot.mediaId,
      caption: optional(screenshot.caption),
    })),
  };
}

const URL_MESSAGE = 'Indiquez une adresse complète, commençant par https:// ou http://.';

function address(path: SchemaPath<string>): void {
  pattern(path, URL_FORMAT, { message: URL_MESSAGE });
  maxLength(path, URL_MAX, { message: `${URL_MAX} caractères au plus.` });
}

/** Règles de la saisie, alignées sur `SaveProjectRequest` et `ProjectAdministration` (D-CX). */
export function projectSchema(path: SchemaPathTree<ProjectModel>): void {
  required(path.title, { message: 'Indiquez le titre.' });
  pattern(path.title, HAS_LETTER_OR_DIGIT, {
    message: 'Le titre doit contenir au moins une lettre ou un chiffre.',
  });
  maxLength(path.title, TITLE_MAX, { message: `${TITLE_MAX} caractères au plus.` });
  pattern(path.slug, SLUG_FORMAT, {
    message:
      'Lettres minuscules sans accent, chiffres et tirets seulement, par exemple «\u00a0atlas-erp\u00a0».',
  });
  maxLength(path.slug, TITLE_MAX, { message: `${TITLE_MAX} caractères au plus.` });
  required(path.shortDescription, { message: 'Indiquez le résumé.' });
  validate(path.shortDescription, ({ value }) =>
    value().length > 0 && value().trim() === ''
      ? { kind: 'blank', message: 'Indiquez le résumé.' }
      : undefined,
  );
  maxLength(path.shortDescription, SHORT_DESCRIPTION_MAX, {
    message: `${SHORT_DESCRIPTION_MAX} caractères au plus.`,
  });
  maxLength(path.descriptionMarkdown, DESCRIPTION_MAX, {
    message: '100\u00a0000 caractères au plus.',
  });
  required(path.startDate, { message: 'Indiquez la date de début.' });
  // Invariant 21 : un projet terminé a une date de fin, jamais avant son début (invariant 17)
  validate(path.endDate, ({ value, valueOf }) => {
    if (valueOf(path.stage) !== 'COMPLETED') {
      return undefined;
    }
    if (value() === '') {
      return { kind: 'required', message: 'Indiquez la date de fin d’un projet terminé.' };
    }
    const start = valueOf(path.startDate);
    return start !== '' && value() < start
      ? { kind: 'order', message: 'La fin précède le début.' }
      : undefined;
  });
  address(path.repositoryUrl);
  address(path.demoUrl);
  pattern(path.displayOrder, WHOLE_NUMBER, {
    message: 'Indiquez un nombre entier positif ou nul, par exemple 10.',
  });
  applyEach(path.screenshots, (screenshot) => {
    maxLength(screenshot.caption, CAPTION_MAX, { message: `${CAPTION_MAX} caractères au plus.` });
  });
}
