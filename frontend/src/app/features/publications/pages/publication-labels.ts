import { PublicationType } from '../../../core/api/api-types';

/** Textes propres à chaque type de publication (accords compris). */
export interface PublicationLabels {
  list: string;
  one: string;
  many: string;
  description: string;
  none: string;
  noMatch: string;
  listError: string;
  notFound: string;
  seeAll: string;
  unavailable: string;
  loadError: string;
}

export const PUBLICATION_LABELS: Record<PublicationType, PublicationLabels> = {
  ARTICLE: {
    list: 'Articles',
    one: 'article',
    many: 'articles',
    description:
      'Articles techniques de Blek Ngossanga\u202f: ingénierie logicielle, vision par ordinateur et intelligence artificielle.',
    none: 'Aucun article n’est encore publié.',
    noMatch: 'Aucun article publié ne correspond à ce filtre.',
    listError: 'La liste des articles n’a pas pu être chargée.',
    notFound: 'Article introuvable',
    seeAll: 'Voir tous les articles',
    unavailable: 'Article indisponible',
    loadError: 'L’article n’a pas pu être chargé.',
  },
  NEWS: {
    list: 'Actualités',
    one: 'actualité',
    many: 'actualités',
    description:
      'Actualités de Blek Ngossanga\u202f: annonces sur les projets et les publications.',
    none: 'Aucune actualité n’est encore publiée.',
    noMatch: 'Aucune actualité publiée ne correspond à ce filtre.',
    listError: 'La liste des actualités n’a pas pu être chargée.',
    notFound: 'Actualité introuvable',
    seeAll: 'Voir toutes les actualités',
    unavailable: 'Actualité indisponible',
    loadError: 'L’actualité n’a pas pu être chargée.',
  },
};
