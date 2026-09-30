/**
 * Icônes utilisées par l'interface : tracés repris de Lucide (licence ISC, voir
 * frontend/THIRD-PARTY-NOTICES.md), grille de 24, trait de 2. Une icône n'entre ici
 * qu'avec son premier usage (docs/frontend/02-design-system.md §14).
 */
export const ICONS = {
  menu: ['M4 5h16', 'M4 12h16', 'M4 19h16'],
  close: ['M18 6 6 18', 'm6 6 12 12'],
} as const satisfies Record<string, readonly string[]>;

export type IconName = keyof typeof ICONS;
