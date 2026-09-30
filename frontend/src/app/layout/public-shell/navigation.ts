export interface NavItem {
  /** Chemin absolu de la page (`/about`). */
  path: string;
  label: string;
  /** Reprise dans le pied de page (liens principaux seulement). */
  footer: boolean;
}

/**
 * Pages publiques réellement disponibles, dans l'ordre de la navigation. Chaque page ajoute
 * son lien avec sa route : aucun lien vers une page qui n'existe pas encore.
 */
export const PUBLIC_NAVIGATION: readonly NavItem[] = [
  { path: '/about', label: 'À propos', footer: true },
];
