import { ProjectStage, PublicationType } from '../../core/api/api-types';

/**
 * Libellés et adresses communs aux cartes de contenu et aux pages (projets, publications,
 * séries) : une seule formulation pour un même fait.
 */

const STAGE_LABELS: Record<ProjectStage, string> = {
  IN_PROGRESS: 'En cours',
  COMPLETED: 'Terminé',
};

/** État d'un projet, en toutes lettres. */
export function projectStageLabel(stage: ProjectStage): string {
  return STAGE_LABELS[stage];
}

const LIST_PATHS: Record<PublicationType, string> = {
  ARTICLE: '/articles',
  NEWS: '/news',
};

/** Liste des publications d'un type : `/articles` ou `/news`. */
export function publicationListPath(type: PublicationType): string {
  return LIST_PATHS[type];
}

/** Adresse canonique d'une publication, selon son type. */
export function publicationPath(type: PublicationType, slug: string): string {
  return `${LIST_PATHS[type]}/${slug}`;
}

/** « 4 min de lecture ». */
export function readingTimeLabel(minutes: number): string {
  return `${minutes}\u00a0min de lecture`;
}

/** « 1 chapitre », « 4 chapitres ». */
export function chapterCountLabel(count: number): string {
  return `${count} ${count <= 1 ? 'chapitre' : 'chapitres'}`;
}
