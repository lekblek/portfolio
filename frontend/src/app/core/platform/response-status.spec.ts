import { RESPONSE_INIT } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { injectResponseStatus } from './response-status';

describe('injectResponseStatus', () => {
  it('sets the status of the server response', () => {
    const responseInit: ResponseInit = {};
    TestBed.configureTestingModule({
      providers: [{ provide: RESPONSE_INIT, useValue: responseInit }],
    });

    TestBed.runInInjectionContext(injectResponseStatus)(404);

    expect(responseInit.status).toBe(404);
  });

  it('does nothing in the browser, where there is no server response', () => {
    const setStatus = TestBed.runInInjectionContext(injectResponseStatus);

    expect(() => setStatus(404)).not.toThrow();
  });
});
