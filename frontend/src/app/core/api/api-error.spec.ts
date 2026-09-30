import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';

import { ApiProblem, toApiError } from './api-error';

function problemResponse(problem: ApiProblem): HttpErrorResponse {
  return new HttpErrorResponse({
    status: problem.status,
    error: problem,
    headers: new HttpHeaders({ 'Content-Type': 'application/problem+json' }),
    url: '/api/admin/publications',
  });
}

describe('toApiError', () => {
  it('keeps the field errors of a failed validation', () => {
    const error = problemResponse({
      status: 400,
      title: 'Bad Request',
      detail: 'Requête invalide.',
      code: 'VALIDATION_FAILED',
      errors: [{ field: 'title', message: 'ne doit pas être vide' }],
    });

    expect(toApiError(error)).toEqual({
      status: 400,
      code: 'VALIDATION_FAILED',
      detail: 'Requête invalide.',
      fieldErrors: [{ field: 'title', message: 'ne doit pas être vide' }],
    });
  });

  it('converts a not found problem', () => {
    const error = problemResponse({
      status: 404,
      detail: 'Publication introuvable.',
      code: 'RESOURCE_NOT_FOUND',
    });

    expect(toApiError(error)).toEqual({
      status: 404,
      code: 'RESOURCE_NOT_FOUND',
      detail: 'Publication introuvable.',
      fieldErrors: [],
    });
  });

  it('keeps the stable code of a business conflict', () => {
    const error = problemResponse({
      status: 409,
      detail: 'Le slug d’un contenu déjà publié ne change plus.',
      code: 'SLUG_LOCKED',
    });

    expect(toApiError(error)).toMatchObject({ status: 409, code: 'SLUG_LOCKED' });
  });

  it('keeps the status of a response that is not a problem detail', () => {
    const error = new HttpErrorResponse({ status: 502, error: '<html>Bad Gateway</html>' });

    expect(toApiError(error)).toEqual({ status: 502, code: null, detail: null, fieldErrors: [] });
  });

  it('reports a network failure with status 0', () => {
    const error = new HttpErrorResponse({ status: 0, error: new ProgressEvent('error') });

    expect(toApiError(error)).toEqual({ status: 0, code: null, detail: null, fieldErrors: [] });
  });

  it('treats any other error as a network failure', () => {
    expect(toApiError(new Error('boom'))).toMatchObject({ status: 0, code: null });
  });
});
