import {
  HttpBackend,
  HttpClient,
  HttpContext,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { API_ORIGIN } from './api-origin';
import { API_REQUEST, apiRequestInterceptor } from './api-request';
import { ServerApiBackend } from './server-api-backend';

describe('ServerApiBackend', () => {
  let requested: string[];

  beforeEach(() => {
    requested = [];
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      requested.push(String(input));
      return new Response('{}', { headers: { 'Content-Type': 'application/json' } });
    });
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiRequestInterceptor])),
        { provide: API_ORIGIN, useValue: 'http://backend:8080' },
        { provide: HttpBackend, useClass: ServerApiBackend },
      ],
    });
  });

  afterEach(() => vi.restoreAllMocks());

  it('sends api calls to the internal origin during server rendering', async () => {
    await firstValueFrom(TestBed.inject(HttpClient).get('/api/public/profile?page=2'));

    expect(requested).toEqual(['http://backend:8080/api/public/profile?page=2']);
  });

  it('ignores the page origin that the server platform puts in front of relative urls', async () => {
    const pageOrigin = new HttpContext().set(API_REQUEST, true);

    await firstValueFrom(
      TestBed.inject(HttpClient).get('http://attacker.example/api/public/profile', {
        context: pageOrigin,
      }),
    );

    expect(requested).toEqual(['http://backend:8080/api/public/profile']);
  });

  it('leaves other urls unchanged', async () => {
    const http = TestBed.inject(HttpClient);
    await firstValueFrom(http.get('/assets/data.json'));
    await firstValueFrom(http.get('https://example.org/api/x'));

    expect(requested).toEqual(['/assets/data.json', 'https://example.org/api/x']);
  });
});
