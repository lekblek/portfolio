import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { AdminContactMessage } from '../../../../core/api/api-types';
import { Toaster } from '../../../../shared/ui/toast';
import { forwardFrom, replyLink } from '../data/messages';
import { MessageListPage } from './message-list-page';
import { MessagePage } from './message-page';

const API = '/api/admin/contact-messages';

const MESSAGE: AdminContactMessage = {
  id: 7,
  name: 'Alice Martin',
  email: 'alice@example.test',
  subject: 'Mission <b>Angular</b>',
  message: 'Bonjour,\n\nAuriez-vous un créneau ?',
  status: 'NEW',
  createdAt: '2026-10-02T08:42:00Z',
  updatedAt: '2026-10-02T08:42:00Z',
};

async function open(url: string) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter(
        [
          { path: 'admin/messages', component: MessageListPage },
          { path: 'admin/messages/:id', component: MessagePage },
        ],
        withComponentInputBinding(),
      ),
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  const http = TestBed.inject(HttpTestingController);
  const tick = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    harness.detectChanges();
  };
  const element = () => harness.routeNativeElement as HTMLElement;
  const button = (name: string) =>
    Array.from(element().querySelectorAll('button')).find(
      (candidate) => candidate.textContent?.replace(/\s+/g, ' ').trim() === name,
    )!;
  return { http, tick, element, button };
}

describe('contact message data', () => {
  it('only moves a message forward, skipping steps if needed', () => {
    expect(forwardFrom('NEW').map((t) => t.target)).toEqual(['READ', 'PROCESSED', 'ARCHIVED']);
    expect(forwardFrom('PROCESSED').map((t) => t.label)).toEqual(['Archiver']);
    expect(forwardFrom('ARCHIVED')).toEqual([]);
  });

  it('replies by e-mail with the subject', () => {
    expect(replyLink(MESSAGE)).toBe(
      'mailto:alice@example.test?subject=Re%3A%20Mission%20%3Cb%3EAngular%3C%2Fb%3E',
    );
  });
});

describe('MessageListPage', () => {
  it('lists the messages filtered by status', async () => {
    const { http, element, tick } = await open('/admin/messages?status=NEW');
    const request = http.expectOne((r) => r.url === API);
    expect(request.request.params.get('status')).toBe('NEW');
    request.flush({
      content: [MESSAGE],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    });
    await tick();

    const row = element().querySelector('tbody tr')!;
    expect(row.querySelector('th a')?.getAttribute('href')).toBe('/admin/messages/7');
    expect(row.textContent).toContain('alice@example.test');
    expect(row.querySelector('app-status-badge')?.textContent?.trim()).toBe('Nouveau');
  });
});

describe('MessagePage', () => {
  it('shows the message as written, and moves it forward', async () => {
    const { http, element, tick, button } = await open('/admin/messages/7');
    http.expectOne(`${API}/7`).flush(MESSAGE);
    await tick();

    expect(element().querySelector('h1')?.textContent).toBe('Mission <b>Angular</b>');
    expect(element().querySelector('.message-body')?.textContent).toContain(
      'Bonjour,\n\nAuriez-vous un créneau ?',
    );
    expect(element().querySelector('a[href^="mailto:alice@example.test"]')).not.toBeNull();

    button('Marquer comme lu').click();
    await tick();
    const change = http.expectOne({ method: 'POST', url: `${API}/7/status` });
    expect(change.request.body).toEqual({ status: 'READ' });
    change.flush({ ...MESSAGE, status: 'READ' });
    await tick();

    expect(element().querySelector('app-status-badge')?.textContent?.trim()).toBe('Lu');
    expect(button('Marquer comme lu')).toBeUndefined();
    expect(TestBed.inject(Toaster).toasts()[0].message).toBe(
      'Message de Alice Martin : marqué comme lu.',
    );
  });

  it('deletes a message for good, after a confirmation naming the sender', async () => {
    const { http, element, tick, button } = await open('/admin/messages/7');
    http.expectOne(`${API}/7`).flush(MESSAGE);
    await tick();

    button('Supprimer définitivement').click();
    await tick();
    const dialog = element().querySelector('dialog')!;
    expect(dialog.textContent).toContain('Supprimer le message de Alice Martin');
    expect(TestBed.inject(DOCUMENT).activeElement?.textContent?.trim()).toBe('Annuler');
    Array.from(dialog.querySelectorAll('button'))
      .find((b) => b.textContent?.trim() === 'Supprimer définitivement')!
      .click();
    await tick();

    http
      .expectOne({ method: 'DELETE', url: `${API}/7` })
      .flush(null, { status: 204, statusText: 'No Content' });
    await tick();
    await tick();
    expect(TestBed.inject(Router).url).toBe('/admin/messages');
    expect(TestBed.inject(Toaster).toasts()[0].message).toBe(
      'Message de Alice Martin supprimé définitivement.',
    );
  });
});
