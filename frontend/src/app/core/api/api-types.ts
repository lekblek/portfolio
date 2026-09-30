import { components } from './openapi';

/**
 * Schémas du contrat (`docs/api/openapi.json`), générés par `npm run api:types`.
 * Chaque fonctionnalité ajoute ici les alias qu'elle utilise :
 * `export type ProjectSummary = Schemas['ProjectSummaryResponse'];`
 */
export type Schemas = components['schemas'];
