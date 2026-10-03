import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { AdminMedia, AdminProject, AdminTechnology } from '../../../../core/api/api-types';
import { Toaster } from '../../../../shared/ui/toast';
import { ProjectFormPage } from './project-form-page';

const API = '/api/admin/projects';

const TECHNOLOGIES: AdminTechnology[] = [
  { id: 5, name: 'Java', slug: 'java', displayOrder: 0 },
  { id: 6, name: 'Python', slug: 'python', displayOrder: 1 },
];

const PROJECT: AdminProject = {
  id: 7,
  title: 'Atlas ERP',
  slug: 'atlas-erp',
  slugLocked: true,
  shortDescription: 'ERP pour PME.',
  descriptionMarkdown: '',
  stage: 'COMPLETED',
  visibility: 'PUBLISHED',
  startDate: '2024-03-01',
  endDate: '2025-06-30',
  repositoryUrl: null,
  demoUrl: null,
  featured: false,
  displayOrder: 0,
  technologyIds: [5],
  coverMediaId: null,
  screenshots: [{ mediaId: 13, caption: 'Tableau de bord' }],
};

const IMAGE: AdminMedia = {
  id: 13,
  url: '/api/public/media/capture.webp',
  originalName: 'capture.webp',
  format: 'WEBP',
  mimeType: 'image/webp',
  sizeBytes: 50_000,
  width: 1600,
  height: 1000,
  altText: 'Tableau de bord',
  createdAt: '2026-10-02T08:00:00Z',
};

@Component({ template: '<h1>Projets</h1>' })
class ListStub {}

async function open(url: string, project?: AdminProject) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter(
        [
          { path: 'admin/projects', component: ListStub },
          { path: 'admin/projects/new', component: ProjectFormPage },
          { path: 'admin/projects/:id', component: ProjectFormPage },
        ],
        withComponentInputBinding(),
      ),
    ],
  });
  const harness = await RouterTestingHarness.create();
  const page = await harness.navigateByUrl(url, ProjectFormPage);
  const http = TestBed.inject(HttpTestingController);
  http.expectOne('/api/admin/technologies').flush(TECHNOLOGIES);
  if (project) {
    http.expectOne(`${API}/${project.id}`).flush(project);
  }
  const element = () => harness.routeNativeElement as HTMLElement;
  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await harness.fixture.whenStable();
    harness.detectChanges();
  };
  // Requête en attente : `whenStable` attendrait sa réponse
  const tick = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    harness.detectChanges();
  };
  await tick();
  for (const request of http.match((r) => r.url.startsWith('/api/admin/media/'))) {
    request.flush(IMAGE);
  }
  await settle();
  const input = (id: string) => element().querySelector<HTMLInputElement>(`#${id}`)!;
  const type = (id: string, value: string) => {
    input(id).value = value;
    input(id).dispatchEvent(new Event('input'));
  };
  const submit = async () => {
    element()
      .querySelector('form')!
      .dispatchEvent(new Event('submit', { cancelable: true }));
    await settle();
  };
  return { page, http, element, settle, tick, input, type, submit };
}

describe('ProjectFormPage', () => {
  it('creates a draft project, then notifies it and goes back to the list', async () => {
    const { http, element, type, submit, settle } = await open('/admin/projects/new');

    expect(element().querySelector('h1')?.textContent).toBe('Nouveau projet');
    expect(document.activeElement?.id).toBe('projet-titre');
    type('projet-titre', '  Vigie  ');
    type('projet-resume', 'Supervision.');
    type('projet-debut', '2026-02-02');
    await submit();

    const post = http.expectOne({ method: 'POST', url: API });
    expect(post.request.body).toMatchObject({
      title: 'Vigie',
      slug: undefined,
      stage: 'IN_PROGRESS',
      visibility: 'DRAFT',
      startDate: '2026-02-02',
      endDate: undefined,
      displayOrder: 0,
      technologyIds: [],
      screenshots: [],
    });
    post.flush({ ...PROJECT, id: 9, title: 'Vigie', visibility: 'DRAFT', slugLocked: false });
    await settle();

    expect(TestBed.inject(Router).url).toBe('/admin/projects');
    expect(TestBed.inject(Toaster).toasts()[0].message).toBe('Projet «\u00a0Vigie\u00a0» créé.');
  });

  it('asks a completed project for its end date and focuses it', async () => {
    const { http, element, type, submit, settle } = await open('/admin/projects/new');
    type('projet-titre', 'Vigie');
    type('projet-resume', 'Supervision.');
    type('projet-debut', '2026-02-02');
    const completed = element().querySelector<HTMLInputElement>('input[value="COMPLETED"]')!;
    completed.click();
    await settle();

    await submit();

    http.expectNone({ method: 'POST' });
    expect(element().querySelector('#projet-fin-erreur')?.textContent).toContain(
      'Indiquez la date de fin d’un projet terminé.',
    );
    expect(document.activeElement?.id).toBe('projet-fin');
  });

  it('edits a published project: slug frozen, screenshots, and the whole input sent back', async () => {
    const { http, element, input, type, submit, settle } = await open('/admin/projects/7', PROJECT);

    expect(element().querySelector('h1')?.textContent).toContain('Atlas ERP');
    expect(input('projet-slug').readOnly).toBe(true);
    expect(element().textContent).toContain('Figé : ce projet a déjà été publié');
    expect(input('projet-capture-0-legende').value).toBe('Tableau de bord');
    expect(element().querySelector('.multi-select-chip')?.textContent).toContain('Java');

    type('projet-titre', 'Atlas ERP 2');
    await submit();
    const put = http.expectOne({ method: 'PUT', url: `${API}/7` });
    expect(put.request.body).toMatchObject({
      title: 'Atlas ERP 2',
      slug: undefined,
      endDate: '2025-06-30',
      technologyIds: [5],
      screenshots: [{ mediaId: 13, caption: 'Tableau de bord' }],
    });
    put.flush({ ...PROJECT, title: 'Atlas ERP 2' });
    await settle();
  });

  it('shows the errors of the server under their field, inside the screenshots too', async () => {
    const { http, element, submit, settle } = await open('/admin/projects/7', PROJECT);

    await submit();
    http.expectOne({ method: 'PUT', url: `${API}/7` }).flush(
      {
        status: 400,
        code: 'VALIDATION_FAILED',
        errors: [{ field: 'screenshots[0].caption', message: '300 caractères au plus.' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await settle();

    expect(element().querySelector('#projet-capture-0-legende-erreur')?.textContent).toContain(
      '300 caractères au plus.',
    );
  });

  it('says why a frozen slug is refused', async () => {
    const { http, element, submit, settle } = await open('/admin/projects/7', {
      ...PROJECT,
      slugLocked: false,
    });

    await submit();
    http
      .expectOne({ method: 'PUT', url: `${API}/7` })
      .flush({ status: 409, code: 'SLUG_LOCKED' }, { status: 409, statusText: 'Conflict' });
    await settle();

    expect(element().querySelector('#projet-slug-erreur')?.textContent).toContain(
      'son slug ne peut plus changer',
    );
  });

  it('adds a screenshot from the media library, once', async () => {
    const { http, element, tick, settle } = await open('/admin/projects/7', PROJECT);
    const other: AdminMedia = { ...IMAGE, id: 21, originalName: 'factures.webp' };

    const add = () => element().querySelector<HTMLButtonElement>('[data-add-screenshot]')!.click();
    add();
    await tick();
    http
      .expectOne((r) => r.url === '/api/admin/media')
      .flush({ content: [IMAGE, other], page: 0, size: 20, totalElements: 2, totalPages: 1 });
    await tick();
    element().querySelectorAll<HTMLButtonElement>('dialog .media-picker-choice')[1].click();
    await tick();
    for (const request of http.match((r) => r.url.startsWith('/api/admin/media/'))) {
      request.flush(other);
    }
    await settle();

    expect(element().querySelectorAll('app-sortable-item')).toHaveLength(2);
    expect(document.activeElement?.id).toBe('projet-capture-1-legende');

    add();
    await tick();
    element().querySelectorAll<HTMLButtonElement>('dialog .media-picker-choice')[1].click();
    await settle();
    expect(element().querySelectorAll('app-sortable-item')).toHaveLength(2);
  });

  it('says when the project does not exist', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter(
          [{ path: 'admin/projects/:id', component: ProjectFormPage }],
          withComponentInputBinding(),
        ),
      ],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/admin/projects/99');
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/admin/technologies').flush(TECHNOLOGIES);
    http.expectOne(`${API}/99`).flush(null, { status: 404, statusText: 'Not Found' });
    await new Promise((resolve) => setTimeout(resolve));
    await harness.fixture.whenStable();

    expect(harness.routeNativeElement?.textContent).toContain(
      'Ce projet n’existe pas ou n’existe plus.',
    );
  });
});
