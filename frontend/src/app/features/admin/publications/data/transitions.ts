import { PublicationStatus } from '../../../../core/api/api-types';

/** Action proposée sur une publication : statut visé et libellé du bouton. */
export interface Transition {
  target: PublicationStatus;
  label: string;
  /** Programmation : une date et une heure futures sont demandées. */
  needsDate?: boolean;
  /** Le site perd la publication : confirmation demandée. */
  confirm?: boolean;
}

/**
 * Transitions permises depuis le statut observable (02-modele-metier §15, D-AV) : seules celles-ci
 * sont proposées ; le serveur reste juge (409 `INVALID_PUBLICATION_TRANSITION`). Action principale
 * en premier.
 */
const TRANSITIONS: Record<PublicationStatus, readonly Transition[]> = {
  DRAFT: [
    { target: 'PUBLISHED', label: 'Publier maintenant' },
    { target: 'SCHEDULED', label: 'Programmer', needsDate: true },
    { target: 'IN_REVIEW', label: 'Passer en relecture' },
  ],
  IN_REVIEW: [
    { target: 'PUBLISHED', label: 'Publier maintenant' },
    { target: 'SCHEDULED', label: 'Programmer', needsDate: true },
    { target: 'DRAFT', label: 'Repasser en brouillon' },
  ],
  SCHEDULED: [
    { target: 'PUBLISHED', label: 'Publier maintenant' },
    { target: 'SCHEDULED', label: 'Changer la date', needsDate: true },
    { target: 'DRAFT', label: 'Annuler la programmation' },
  ],
  PUBLISHED: [{ target: 'ARCHIVED', label: 'Archiver', confirm: true }],
  ARCHIVED: [
    { target: 'PUBLISHED', label: 'Republier' },
    { target: 'DRAFT', label: 'Repasser en brouillon' },
  ],
};

export function transitionsFrom(status: PublicationStatus): readonly Transition[] {
  return TRANSITIONS[status];
}

/** Message d'issue d'une transition, accordé au féminin (une publication). */
export function transitionDone(target: PublicationStatus): string {
  switch (target) {
    case 'DRAFT':
      return 'repassée en brouillon';
    case 'IN_REVIEW':
      return 'passée en relecture';
    case 'SCHEDULED':
      return 'programmée';
    case 'PUBLISHED':
      return 'publiée';
    case 'ARCHIVED':
      return 'archivée';
  }
}
