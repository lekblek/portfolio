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

const TIME = new Intl.DateTimeFormat('fr-FR', {
  hour: '2-digit',
  minute: '2-digit',
  timeZone: SITE_TIME_ZONE,
});

/** « 2 octobre 2026 à 10:42 », dans le fuseau de référence. */
export function formatDayTime(instant: string): string {
  return `${formatDay(instant)} à ${TIME.format(new Date(instant))}`;
}

const WALL_CLOCK = new Intl.DateTimeFormat('en-CA', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
  timeZone: SITE_TIME_ZONE,
});

/** Heure murale du fuseau de référence en millisecondes « comme si c'était UTC ». */
function wallClock(instant: number): number {
  const parts = Object.fromEntries(
    WALL_CLOCK.formatToParts(new Date(instant)).map((part) => [part.type, part.value]),
  );
  return Date.UTC(
    +parts['year'],
    +parts['month'] - 1,
    +parts['day'],
    +parts['hour'],
    +parts['minute'],
  );
}

/**
 * Valeur d'un `<input type="datetime-local">` (« 2026-10-15T09:30 ») lue à l'heure du fuseau de
 * référence, quel que soit le fuseau du navigateur → instant ISO. Les affichages (formatDayTime)
 * utilisant le même fuseau, l'heure saisie est l'heure relue.
 */
export function instantFromSiteTime(value: string): string {
  const [day, time] = value.split('T');
  const [year, month, date] = day.split('-').map(Number);
  const [hour, minute] = time.split(':').map(Number);
  const wanted = Date.UTC(year, month - 1, date, hour, minute);
  let instant = wanted;
  // Deux passes : l'écart au fuseau dépend de l'instant (heure d'été)
  for (let pass = 0; pass < 2; pass++) {
    instant = wanted - (wallClock(instant) - instant);
  }
  return new Date(instant).toISOString();
}

/** Instant → valeur d'un `<input type="datetime-local">` à l'heure du fuseau de référence. */
export function siteTimeInput(instant: string): string {
  return new Date(wallClock(new Date(instant).getTime())).toISOString().slice(0, 16);
}

const PUBLICATION_MONTH = new Intl.DateTimeFormat('fr-FR', {
  month: 'long',
  year: 'numeric',
  timeZone: SITE_TIME_ZONE,
});

/** Mois d'une publication dans le fuseau de référence : « septembre 2026 » (groupes de dépêches). */
export function formatPublicationMonth(instant: string): string {
  return PUBLICATION_MONTH.format(new Date(instant));
}

const SHORT_MONTH = new Intl.DateTimeFormat('fr-FR', { month: 'short', timeZone: SITE_TIME_ZONE });

/** Jour et mois abrégé, pour une date en colonne : `{ day: '1er', month: 'oct.' }`. */
export function dayAndShortMonth(instant: string): { day: string; month: string } {
  const date = new Date(instant);
  const day = DAY.formatToParts(date).find((part) => part.type === 'day')?.value ?? '';
  return { day: day === '1' ? '1er' : day, month: SHORT_MONTH.format(date) };
}
