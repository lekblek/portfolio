const KIBI = 1024;

const DECIMAL = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 });

/** Espace insécable entre le nombre et l'unité. */
const NBSP = '\u00a0';

/**
 * Taille d'un fichier en français, annoncée avant un téléchargement : « 512 octets », « 12 Ko »,
 * « 1,2 Mo » (multiples de 1 024, usage des systèmes en français).
 */
export function formatFileSize(bytes: number): string {
  if (bytes < KIBI) {
    return `${bytes}${NBSP}${bytes < 2 ? 'octet' : 'octets'}`;
  }
  if (bytes < KIBI * KIBI) {
    return `${DECIMAL.format(bytes / KIBI)}${NBSP}Ko`;
  }
  return `${DECIMAL.format(bytes / (KIBI * KIBI))}${NBSP}Mo`;
}
