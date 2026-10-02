/** Les trois vocabulaires de la taxonomie (D-CS, D-CW), une sous-section chacun. */
export type VocabularyKey = 'categories' | 'tags' | 'technologies';

/**
 * Ce qui distingue un vocabulaire d'un autre : adresse de l'API, bornes de la saisie (`V007`,
 * `V005`), champs propres, et les mots pour en parler (accord en genre compris).
 */
export interface Vocabulary {
  key: VocabularyKey;
  /** Route de l'API d'administration. */
  api: string;
  /** « Catégories » : titre de la liste et de la sous-section. */
  plural: string;
  /** « catégorie », « tag » : nom commun, sans article. */
  noun: string;
  feminine: boolean;
  nameMax: number;
  slugMax: number;
  /** Description de 500 caractères au plus (catégories). */
  description: boolean;
  /** Ordre d'affichage, entier positif ou nul (technologies). */
  displayOrder: boolean;
  /** Contenus qui l'utilisent, avec leur article : refus de suppression (`TERM_STILL_USED`). */
  usedBy: string;
}

export const VOCABULARIES: Record<VocabularyKey, Vocabulary> = {
  categories: {
    key: 'categories',
    api: '/api/admin/categories',
    plural: 'Catégories',
    noun: 'catégorie',
    feminine: true,
    nameMax: 80,
    slugMax: 80,
    description: true,
    displayOrder: false,
    usedBy: 'une publication',
  },
  tags: {
    key: 'tags',
    api: '/api/admin/tags',
    plural: 'Tags',
    noun: 'tag',
    feminine: false,
    nameMax: 60,
    slugMax: 60,
    description: false,
    displayOrder: false,
    usedBy: 'une publication',
  },
  technologies: {
    key: 'technologies',
    api: '/api/admin/technologies',
    plural: 'Technologies',
    noun: 'technologie',
    feminine: true,
    nameMax: 80,
    slugMax: 80,
    description: false,
    displayOrder: true,
    usedBy: 'un projet',
  },
};

/** Ordre des sous-sections. */
export const VOCABULARY_KEYS: readonly VocabularyKey[] = ['categories', 'tags', 'technologies'];

/** « la catégorie », « le tag ». */
export function definite(vocabulary: Vocabulary): string {
  return `${vocabulary.feminine ? 'la' : 'le'} ${vocabulary.noun}`;
}

/** « Nouvelle catégorie », « Nouveau tag ». */
export function newLabel(vocabulary: Vocabulary): string {
  return `${vocabulary.feminine ? 'Nouvelle' : 'Nouveau'} ${vocabulary.noun}`;
}

/** Participe passé accordé : `agree(v, 'créé')` → « créée » pour une catégorie. */
export function agree(vocabulary: Vocabulary, participle: string): string {
  return vocabulary.feminine ? `${participle}e` : participle;
}

/** Première lettre en capitale : « Catégorie », « Tag ». */
export function capitalized(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** Nom d'un terme entre guillemets français, espaces insécables comprises. */
export function quoted(name: string): string {
  return `«\u00a0${name}\u00a0»`;
}
