export interface NavItem {
  /** Chemin absolu de la page (`/about`). */
  path: string;
  label: string;
  /** Colonne du plan du site, dans le pied de page : contenus publiés, ou pages du site. */
  section: 'content' | 'site';
}

/**
 * Pages publiques réellement disponibles, dans l'ordre de la navigation. Chaque page ajoute
 * son lien avec sa route : aucun lien vers une page qui n'existe pas encore.
 */
export const PUBLIC_NAVIGATION: readonly NavItem[] = [
  { path: '/projects', label: 'Projets', section: 'content' },
  { path: '/articles', label: 'Articles', section: 'content' },
  { path: '/series', label: 'Séries', section: 'content' },
  { path: '/news', label: 'Actualités', section: 'content' },
  { path: '/about', label: 'À propos', section: 'site' },
  { path: '/contact', label: 'Contact', section: 'site' },
  { path: '/search', label: 'Recherche', section: 'site' },
];
