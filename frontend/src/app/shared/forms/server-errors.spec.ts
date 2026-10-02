import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { form } from '@angular/forms/signals';

import { serverFieldErrors } from './server-errors';

function failure(status: number, body: object): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: { status, ...body } });
}

function setUp() {
  return TestBed.runInInjectionContext(() => form(signal({ name: '', slug: '' })));
}

describe('serverFieldErrors', () => {
  it('attaches the field errors of a failed validation to the known fields only', () => {
    const terms = setUp();

    const errors = serverFieldErrors(
      failure(400, {
        code: 'VALIDATION_FAILED',
        errors: [
          { field: 'name', message: 'ne doit pas être vide' },
          { field: 'unknown', message: 'ignoré' },
        ],
      }),
      { name: terms.name, slug: terms.slug },
    );

    expect(errors).toEqual([
      { fieldTree: terms.name, kind: 'server', message: 'ne doit pas être vide' },
    ]);
  });

  it('attaches a conflict to the field its code names', () => {
    const terms = setUp();

    const errors = serverFieldErrors(
      failure(409, { code: 'NAME_ALREADY_USED' }),
      { name: terms.name, slug: terms.slug },
      { NAME_ALREADY_USED: { field: 'name', message: 'Ce nom est déjà pris.' } },
    );

    expect(errors).toEqual([
      { fieldTree: terms.name, kind: 'server', message: 'Ce nom est déjà pris.' },
    ]);
  });

  it('leaves any other failure to the page', () => {
    const terms = setUp();
    const fields = { name: terms.name, slug: terms.slug };

    expect(serverFieldErrors(failure(409, { code: 'TERM_STILL_USED' }), fields)).toEqual([]);
    expect(serverFieldErrors(failure(500, { code: 'INTERNAL_ERROR' }), fields)).toEqual([]);
    expect(serverFieldErrors(new Error('réseau'), fields)).toEqual([]);
  });
});
