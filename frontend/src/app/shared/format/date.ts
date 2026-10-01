/**
 * Dates du contrat (`LocalDate` du backend, `AAAA-MM-JJ`) affichées en français, au mois près :
 * « janvier 2024 ». Lues et formatées en UTC : le fuseau du serveur de rendu ou du navigateur ne
 * décale jamais le jour, et le texte est identique des deux côtés (hydratation).
 */
const MONTH_YEAR = new Intl.DateTimeFormat('fr-FR', {
  month: 'long',
  year: 'numeric',
  timeZone: 'UTC',
});

/** Espace insécable avant le tiret d'une période : une ligne ne commence jamais par « – ». */
const NBSP = '\u00a0';

function parseDate(date: string): Date {
  const [year, month, day] = date.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day));
}

/** « janvier 2024 ». */
export function formatMonth(date: string): string {
  return MONTH_YEAR.format(parseDate(date));
}

/**
 * Période : « janvier 2024 – juin 2025 », « depuis janvier 2024 » (en cours), « mars 2024 »
 * (début et fin dans le même mois).
 */
export function formatPeriod(start: string, end: string | null): string {
  if (end === null) {
    return `depuis ${formatMonth(start)}`;
  }
  const from = formatMonth(start);
  const to = formatMonth(end);
  return from === to ? from : `${from}${NBSP}– ${to}`;
}

/**
 * Fuseau de référence des dates de publication (instants du contrat) : le jour affiché ne dépend
 * ni du lecteur ni du serveur de rendu, et reste identique à l'hydratation.
 */
const SITE_TIME_ZONE = 'Europe/Paris';

const DAY = new Intl.DateTimeFormat('fr-FR', {
  day: 'numeric',
  month: 'long',
  year: 'numeric',
  timeZone: SITE_TIME_ZONE,
});

const ISO_DAY = new Intl.DateTimeFormat('en-CA', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  timeZone: SITE_TIME_ZONE,
});

/** « 17 septembre 2026 », « 1er octobre 2026 » (ordinal du premier jour, usage français). */
export function formatDay(instant: string): string {
  return DAY.formatToParts(new Date(instant))
    .map((part) => (part.type === 'day' && part.value === '1' ? '1er' : part.value))
    .join('');
}

/** Valeur `datetime` d'un `<time>` au jour près, dans le fuseau de référence : « 2026-09-17 ». */
export function isoDay(instant: string): string {
  return ISO_DAY.format(new Date(instant));
}
