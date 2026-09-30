/** Titre d'un contenu Markdown, pour la table des matières (niveau après décalage : 2 à 6). */
export interface TocEntry {
  id: string;
  text: string;
  level: number;
}

const LIGATURES: Record<string, string> = { œ: 'oe', æ: 'ae', ß: 'ss' };

/**
 * Identifiant stable d'un titre : minuscules, accents et ligatures retirés, tout autre caractère
 * remplacé par un tiret. Réduit à `[a-z0-9-]` : sûr dans un attribut `id` et dans une adresse.
 */
export function slugify(text: string): string {
  const slug = text
    .toLowerCase()
    .replace(/[œæß]/g, (letter) => LIGATURES[letter])
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  return slug || 'section';
}

/** Rend chaque identifiant unique dans un même contenu : `titre`, `titre-2`, `titre-3`… */
export function uniqueSlugger(): (text: string) => string {
  const counts = new Map<string, number>();
  return (text) => {
    const base = slugify(text);
    const count = (counts.get(base) ?? 0) + 1;
    counts.set(base, count);
    return count === 1 ? base : `${base}-${count}`;
  };
}
