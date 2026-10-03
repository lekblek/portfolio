import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { AdminProjectSummary, AdminTechnology } from '../../../../core/api/api-types';
import { ProjectListPage } from './project-list-page';

const PROJECTS: AdminProjectSummary[] = [
  {
    id: 1,
    title: 'Atlas ERP',
    slug: 'atlas-erp',
    visibility: 'PUBLISHED',
    stage: 'COMPLETED',
    featured: true,
    displayOrder: 0,
  },
  {
    id: 2,
    title: 'Covoiturage',
    slug: 'covoiturage',
    visibility: 'DRAFT',
    stage: 'IN_PROGRESS',
    featured: false,
    displayOrder: 3,
  },
];
const TECHNOLOGIES: AdminTechnology[] = [
  { id: 5, name: 'Java', slug: 'java', displayOrder: 0 },
  { id: 6, name: 'Python', slug: 'python', displayOrder: 1 },
];

function page(
  content: AdminProjectSummary[],
  number = 0,
  totalElements = content.length,
  totalPages = 1,
) {
  return {
    content,
    page: number,
    size: 20,
    totalElements,
    totalPages,
    first: number === 0,
    last: number === totalPages - 1,
  };
}

async function open(url: string) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter(
        [{ path: 'admin/projects', component: ProjectListPage }],
        withComponentInputBinding(),
      ),
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  const http = TestBed.inject(HttpTestingController);
  http.expectOne('/api/admin/technologies').flush(TECHNOLOGIES);
  const element = () => harness.routeNativeElement as HTMLElement;
  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await harness.fixture.whenStable();
    harness.detectChanges();
  };
  const tick = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    harness.detectChanges();
  };
  return { harness, http, element, settle, tick };
}

describe('ProjectListPage', () => {
  it('lists the projects with their status, progress and a link to the site', async () => {
    const { http, element, settle } = await open('/admin/projects');
    const request = http.expectOne((r) => r.url === '/api/admin/projects');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.has('visibility')).toBe(false);
    request.flush(page(PROJECTS, 0, 24, 2));
    await settle();

    expect(element().querySelector('[role="status"]')?.textContent).toContain('24 projets');
    const rows = element().querySelectorAll('tbody tr');
    expect(rows).toHaveLength(2);
    expect(rows[0].querySelector('th')?.textContent).toContain('Atlas ERP');
    expect(rows[0].querySelector('app-status-badge')?.textContent?.trim()).toBe('Publié');
    expect(rows[0].textContent).toContain('Mis en avant');
    expect(rows[0].querySelector('a[href="/projects/atlas-erp"]')).not.toBeNull();
    expect(rows[1].querySelector('app-status-badge')?.textContent?.trim()).toBe('Brouillon');
    expect(rows[1].textContent).toContain('En cours');
    expect(rows[1].querySelector('a[href^="/projects/"]')).toBeNull();
    expect(element().querySelector('nav[aria-label="Pages des projets"]')).not.toBeNull();
  });

  it('reads its filters in the address and sends them with the page', async () => {
    const { http, element, settle } = await open(
      '/admin/projects?visibility=DRAFT&technology=python&page=2',
    );
    const request = http.expectOne((r) => r.url === '/api/admin/projects');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('visibility')).toBe('DRAFT');
    expect(request.request.params.get('technology')).toBe('python');
    request.flush(page([], 1, 0, 0));
    await settle();

    expect(element().querySelector<HTMLSelectElement>('#filtre-visibilite')?.value).toBe('DRAFT');
    expect(element().querySelector<HTMLSelectElement>('#filtre-technologie')?.value).toBe('python');
    expect(element().textContent).toContain('Cette page de la liste est vide.');
  });

  it('applies the filters from the first page', async () => {
    const { http, element, settle, tick } = await open('/admin/projects?page=2');
    http.expectOne((r) => r.url === '/api/admin/projects').flush(page(PROJECTS, 1, 22, 2));
    await settle();

    const visibility = element().querySelector<HTMLSelectElement>('#filtre-visibilite')!;
    visibility.value = 'ARCHIVED';
    visibility.dispatchEvent(new Event('change'));
    element()
      .querySelector('form')!
      .dispatchEvent(new Event('submit', { cancelable: true }));
    await tick();
    await tick();

    expect(TestBed.inject(Router).url).toBe('/admin/projects?visibility=ARCHIVED');
    const request = http.expectOne((r) => r.url === '/api/admin/projects');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('visibility')).toBe('ARCHIVED');
  });

  it('says when no project matches the filters', async () => {
    const { http, element, settle } = await open('/admin/projects?technology=cobol');
    http.expectOne((r) => r.url === '/api/admin/projects').flush(page([]));
    await settle();

    expect(element().textContent).toContain('Aucun projet ne correspond à ces filtres.');
    expect(element().querySelector('a[href="/admin/projects"]')?.textContent).toContain(
      'Effacer les filtres',
    );
  });
});
