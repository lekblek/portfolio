import { InjectionToken } from '@angular/core';

/**
 * Origine interne de l'API (`API_ORIGIN`), fournie seulement par la configuration serveur.
 * Le navigateur appelle `/api/...` en relatif ; le rendu serveur n'a pas d'origine courante fiable
 * et ne la déduit jamais de la requête entrante (en-tête `Host` : falsification de requête).
 */
export const API_ORIGIN = new InjectionToken<string>('API_ORIGIN');
