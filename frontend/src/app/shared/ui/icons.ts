/**
 * Icônes utilisées par l'interface : tracés repris de Lucide (licence ISC, voir
 * frontend/THIRD-PARTY-NOTICES.md), grille de 24, trait de 2. Une icône n'entre ici
 * qu'avec son premier usage (docs/frontend/02-design-system.md §14).
 */
export const ICONS = {
  menu: ['M4 5h16', 'M4 12h16', 'M4 19h16'],
  close: ['M18 6 6 18', 'm6 6 12 12'],
  // Cercles de Lucide écrits en arcs : l'icône ne trace que des chemins
  'circle-alert': ['M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0', 'M12 8v4', 'M12 16h.01'],
  'circle-check': ['M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0', 'm9 12 2 2 4-4'],
} as const satisfies Record<string, readonly string[]>;

export type IconName = keyof typeof ICONS;
