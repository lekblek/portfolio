import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DashboardPage } from './dashboard-page';

const COUNTS: Record<string, number> = {
  '/api/admin/contact-messages': 3,
  '/api/admin/publications': 12,
  '/api/admin/projects': 4,
  '/api/admin/series': 2,
  '/api/admin/media': 27,
};

function page(totalElements: number) {
  return {
    content: [],
    page: 0,
    size: 1,
    totalElements,
    totalPages: totalElements,
    first: true,
    last: totalElements <= 1,
  };
}

function setUp() {
  TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
  const fixture = TestBed.createComponent(DashboardPage);
  // Les requêtes partent au premier passage ; la page reste instable tant qu'elles n'ont pas répondu
  TestBed.tick();
  const http = TestBed.inject(HttpTestingController);
  const element = fixture.nativeElement as HTMLElement;
  const settle = async () => {
    await fixture.whenStable();
    fixture.detectChanges();
  };
  const rows = () =>
    Array.from(element.querySelectorAll('.dashboard-row')).map((row) => [
      row.querySelector('dt')?.textContent?.trim(),
      row.querySelector('dd')?.textContent?.trim(),
    ]);
  return { http, element, settle, rows };
}

describe('DashboardPage', () => {
  it('counts the unread messages and the contents from the existing lists', async () => {
    const { http, settle, rows } = setUp();

    for (const request of http.match(() => true)) {
      expect(request.request.params.get('size')).toBe('1');
      request.flush(page(COUNTS[request.request.url]));
    }
    await settle();

    expect(http.match(() => true)).toEqual([]);
    expect(rows()).toEqual([
      ['Messages non lus', '3'],
      ['Publications', '12'],
      ['Projets', '4'],
      ['Séries', '2'],
      ['Médias', '27'],
    ]);
  });

  it('asks for the new messages only', async () => {
    const { http } = setUp();

    const unread = http.expectOne((request) => request.url === '/api/admin/contact-messages');

    expect(unread.request.params.get('status')).toBe('NEW');
  });

  it('offers to retry when a count cannot be read, then shows the tally', async () => {
    const { http, element, settle, rows } = setUp();
    for (const request of http.match(() => true)) {
      if (request.request.url === '/api/admin/series') {
        request.flush({ status: 500 }, { status: 500, statusText: 'Server Error' });
      } else {
        request.flush(page(COUNTS[request.request.url]));
      }
    }
    await settle();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Le relevé n’a pas pu être chargé.',
    );
    element.querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
    TestBed.tick();
    http.expectOne('/api/admin/series?size=1').flush(page(2));
    await settle();

    expect(element.querySelector('[role="alert"]')).toBeNull();
    expect(rows()).toHaveLength(5);
  });
});
