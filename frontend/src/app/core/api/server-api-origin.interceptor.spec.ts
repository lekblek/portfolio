import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Provider } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { API_ORIGIN } from './api-origin';
import { serverApiOriginInterceptor } from './server-api-origin.interceptor';

describe('serverApiOriginInterceptor', () => {
  function setUp(...providers: Provider[]) {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([serverApiOriginInterceptor])),
        provideHttpClientTesting(),
        ...providers,
      ],
    });
    return {
      http: TestBed.inject(HttpClient),
      backend: TestBed.inject(HttpTestingController),
    };
  }

  it('prefixes api calls with the internal origin during server rendering', () => {
    const { http, backend } = setUp({ provide: API_ORIGIN, useValue: 'http://backend:8080' });

    http.get('/api/public/profile').subscribe();

    backend.expectOne('http://backend:8080/api/public/profile').flush({});
    backend.verify();
  });

  it('leaves other urls unchanged', () => {
    const { http, backend } = setUp({ provide: API_ORIGIN, useValue: 'http://backend:8080' });

    http.get('/assets/data.json').subscribe();
    http.get('https://example.org/api/x').subscribe();

    backend.expectOne('/assets/data.json').flush({});
    backend.expectOne('https://example.org/api/x').flush({});
    backend.verify();
  });

  it('keeps api calls relative in the browser, where no origin is provided', () => {
    const { http, backend } = setUp();

    http.get('/api/public/profile').subscribe();

    backend.expectOne('/api/public/profile').flush({});
    backend.verify();
  });
});
