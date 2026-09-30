import { DOCUMENT, inject, InjectionToken } from '@angular/core';

/** Identité du site, décidée par le propriétaire (2026-09-30). */
export const SITE_NAME = 'Blek Ngossanga';

/**
 * Signature professionnelle, en petit texte sous le nom : jamais un slogan. Espaces insécables
 * avant chaque séparateur : une ligne ne commence jamais par « · ».
 */
export const SITE_SIGNATURE = 'Software Engineering\u00a0· AI Vision\u00a0· Research';

/** Titre de référence, pour une page sans titre propre (et les partages de la racine). */
export const SITE_TITLE = 'Blek Ngossanga — Software Engineering, AI Vision & Research';

/** Description par défaut, reprise quand une page n'en fournit pas. */
export const SITE_DESCRIPTION =
  'Projets, articles techniques et travaux de recherche de Blek Ngossanga\u202f: ingénierie ' +
  'logicielle, vision par ordinateur et intelligence artificielle.';

/**
 * Origine publique du site, pour les adresses absolues (lien canonique, Open Graph).
 * Rendu serveur : variable d'environnement `SITE_URL` (app.config.server.ts), jamais l'en-tête
 * `Host` de la requête. Navigateur : l'origine de la page affichée.
 */
export const SITE_URL = new InjectionToken<string>('SITE_URL', {
  factory: () => inject(DOCUMENT).location.origin,
});
