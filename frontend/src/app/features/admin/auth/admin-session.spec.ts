import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AdminSession } from './admin-session';

const URL = '/api/admin/session';
const ACCOUNT = { login: 'admin', lastLoginAt: '2026-10-02T08:00:00Z' };

function setUp() {
  TestBed.configureTestingModule({
    providers: [provideHttpClient(), provideHttpClientTesting()],
  });
  return { session: TestBed.inject(AdminSession), http: TestBed.inject(HttpTestingController) };
}

describe('AdminSession', () => {
  it('asks the server once, then keeps the open session', async () => {
    const { session, http } = setUp();

    const first = session.isOpen();
    http.expectOne({ method: 'GET', url: URL }).flush(ACCOUNT);

    expect(await first).toBe(true);
    expect(session.account()).toEqual(ACCOUNT);
    expect(await session.isOpen()).toBe(true);
    http.verify();
  });

  it('answers no when the server requires a login', async () => {
    const { session, http } = setUp();

    const open = session.isOpen();
    http.expectOne(URL).flush({ status: 401 }, { status: 401, statusText: 'Unauthorized' });

    expect(await open).toBe(false);
    expect(session.account()).toBeNull();
  });

  it('rejects when the server cannot tell', async () => {
    const { session, http } = setUp();

    const open = session.isOpen();
    http.expectOne(URL).error(new ProgressEvent('error'));

    await expect(open).rejects.toBeDefined();
  });

  it('opens the session with the credentials, then closes it', async () => {
    const { session, http } = setUp();

    const opening = session.open({ login: 'admin', password: 'secret' });
    const post = http.expectOne({ method: 'POST', url: URL });
    expect(post.request.body).toEqual({ login: 'admin', password: 'secret' });
    post.flush(ACCOUNT);
    await opening;
    expect(session.account()).toEqual(ACCOUNT);
    expect(session.closedByUser()).toBe(false);

    const closing = session.close();
    http.expectOne({ method: 'DELETE', url: URL }).flush(null, { status: 204, statusText: '' });
    await closing;

    expect(session.account()).toBeNull();
    expect(session.closedByUser()).toBe(true);
  });

  it('keeps the session when the server refuses to close it', async () => {
    const { session, http } = setUp();
    const opening = session.open({ login: 'admin', password: 'secret' });
    http.expectOne(URL).flush(ACCOUNT);
    await opening;

    const closing = session.close();
    http.expectOne(URL).error(new ProgressEvent('error'));

    await expect(closing).rejects.toBeDefined();
    expect(session.account()).toEqual(ACCOUNT);
  });
});
