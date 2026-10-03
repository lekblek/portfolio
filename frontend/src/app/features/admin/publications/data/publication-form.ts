import { maxLength, pattern, required, SchemaPathTree, validate } from '@angular/forms/signals';

import {
  AdminPublication,
  CreatePublicationRequest,
  PublicationType,
  UpdatePublicationRequest,
} from '../../../../core/api/api-types';

/** Bornes du contrat (`Publication`, D-CU). */
export const TITLE_MAX = 160;
export const SUMMARY_MAX = 500;
export const CONTENT_MAX = 100_000;
export const SEO_TITLE_MAX = 120;
export const SEO_DESCRIPTION_MAX = 300;

const HAS_LETTER_OR_DIGIT = /[\p{L}\p{N}]/u;
const SLUG_FORMAT = /^[a-z0-9]+(-[a-z0-9]+)*$/;

/** Saisie d'une publication ; catégorie en texte (valeur d'une liste déroulante, vide : aucune). */
export interface PublicationModel {
  title: string;
  slug: string;
  summary: string;
  contentMarkdown: string;
  categoryId: string;
  tagIds: number[];
  coverMediaId: number | null;
  featured: boolean;
  seoTitle: string;
  seoDescription: string;
}

export const EMPTY_PUBLICATION: PublicationModel = {
  title: '',
  slug: '',
  summary: '',
  contentMarkdown: '',
  categoryId: '',
  tagIds: [],
  coverMediaId: null,
  featured: false,
  seoTitle: '',
  seoDescription: '',
};

export function toModel(publication: AdminPublication): PublicationModel {
  return {
    title: publication.title,
    slug: publication.slug,
    summary: publication.summary,
    contentMarkdown: publication.contentMarkdown,
    categoryId: publication.categoryId === null ? '' : String(publication.categoryId),
    tagIds: [...publication.tagIds],
    coverMediaId: publication.coverMediaId,
    featured: publication.featured,
    seoTitle: publication.seoTitle ?? '',
    seoDescription: publication.seoDescription ?? '',
  };
}

function optional(value: string): string | undefined {
  const trimmed = value.trim();
  return trimmed === '' ? undefined : trimmed;
}

/** Saisie → requête de modification : slug envoyé seulement s'il a changé, champs vides omis. */
export function toUpdateRequest(
  model: PublicationModel,
  initialSlug: string,
): UpdatePublicationRequest {
  const slug = model.slug.trim();
  return {
    title: model.title.trim(),
    slug: slug !== '' && slug !== initialSlug ? slug : undefined,
    summary: model.summary.trim(),
    contentMarkdown: model.contentMarkdown,
    categoryId: model.categoryId === '' ? undefined : Number(model.categoryId),
    tagIds: [...model.tagIds],
    coverMediaId: model.coverMediaId ?? undefined,
    featured: model.featured,
    seoTitle: optional(model.seoTitle),
    seoDescription: optional(model.seoDescription),
  };
}

/** Création : la même saisie, avec le type, fixé une fois pour toutes (D-CU). */
export function toCreateRequest(
  model: PublicationModel,
  type: PublicationType,
): CreatePublicationRequest {
  return { type, ...toUpdateRequest(model, '') };
}

/** Règles de la saisie, alignées sur `CreatePublicationRequest` (D-CU). */
export function publicationSchema(path: SchemaPathTree<PublicationModel>): void {
  required(path.title, { message: 'Indiquez le titre.' });
  pattern(path.title, HAS_LETTER_OR_DIGIT, {
    message: 'Le titre doit contenir au moins une lettre ou un chiffre.',
  });
  maxLength(path.title, TITLE_MAX, { message: `${TITLE_MAX} caractères au plus.` });
  pattern(path.slug, SLUG_FORMAT, {
    message:
      'Lettres minuscules sans accent, chiffres et tirets seulement, par exemple « mon-article ».',
  });
  maxLength(path.slug, TITLE_MAX, { message: `${TITLE_MAX} caractères au plus.` });
  required(path.summary, { message: 'Indiquez le résumé.' });
  validate(path.summary, ({ value }) =>
    value().length > 0 && value().trim() === ''
      ? { kind: 'blank', message: 'Indiquez le résumé.' }
      : undefined,
  );
  maxLength(path.summary, SUMMARY_MAX, { message: `${SUMMARY_MAX} caractères au plus.` });
  maxLength(path.contentMarkdown, CONTENT_MAX, { message: '100 000 caractères au plus.' });
  maxLength(path.seoTitle, SEO_TITLE_MAX, { message: `${SEO_TITLE_MAX} caractères au plus.` });
  maxLength(path.seoDescription, SEO_DESCRIPTION_MAX, {
    message: `${SEO_DESCRIPTION_MAX} caractères au plus.`,
  });
}
