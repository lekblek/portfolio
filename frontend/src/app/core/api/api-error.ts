import { HttpErrorResponse } from '@angular/common/http';

/**
 * Corps d'erreur de l'API : `ProblemDetail` (RFC 9457) et extensions du projet (05-conventions-api §10).
 * Absent de `openapi.json` : écrit d'après la documentation et `GlobalExceptionHandler`.
 */
export interface ApiProblem {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
  /** Code stable (05 §11) : seule base des décisions de l'interface, jamais `detail`. */
  code?: string;
  /** Erreurs de champ d'une 400 `VALIDATION_FAILED` ; `field` : chemin du champ ou nom du paramètre. */
  errors?: FieldError[];
  /** Identifiant de l'incident d'une 500 `INTERNAL_ERROR`, à citer au support. */
  requestId?: string;
}

export interface FieldError {
  field: string;
  message: string;
}

/** Codes stables que l'interface traite ; les autres codes restent de simples chaînes. */
export type ApiErrorCode =
  | 'RESOURCE_NOT_FOUND'
  | 'VALIDATION_FAILED'
  | 'AUTHENTICATION_REQUIRED'
  | 'TOO_MANY_LOGIN_ATTEMPTS'
  | 'SLUG_LOCKED';

/** Erreur d'appel à l'API, prête pour l'affichage (textes de l'API en français, D-DJ). */
export interface ApiError {
  /** Statut HTTP ; 0 si le serveur n'a pas répondu (réseau, requête annulée). */
  status: number;
  code: ApiErrorCode | (string & {}) | null;
  detail: string | null;
  fieldErrors: FieldError[];
}

/**
 * Convertit une erreur d'appel (`HttpErrorResponse` le plus souvent) en `ApiError`.
 * Un corps qui n'est pas un `ProblemDetail` (page HTML d'un mandataire, par exemple) garde son
 * statut, sans code ni texte ; toute autre erreur devient une erreur réseau (`status: 0`).
 */
export function toApiError(error: unknown): ApiError {
  if (!(error instanceof HttpErrorResponse)) {
    return { status: 0, code: null, detail: null, fieldErrors: [] };
  }
  const problem = isApiProblem(error.error) ? error.error : null;
  return {
    status: error.status,
    code: problem?.code ?? null,
    detail: problem?.detail ?? null,
    fieldErrors: problem?.errors ?? [],
  };
}

function isApiProblem(body: unknown): body is ApiProblem {
  return (
    typeof body === 'object' &&
    body !== null &&
    typeof (body as Partial<ApiProblem>).status === 'number'
  );
}
