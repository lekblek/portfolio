import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { unsavedChangesGuard } from '../../../../shared/forms/unsaved-changes.guard';
import { Toaster } from '../../../../shared/ui/toast';
import { TermFormPage } from './term-form-page';

const API = '/api/admin/categories';
const CATEGORIES = [{ id: 7, name: 'Backend', slug: 'backend', description: 'Spring Boot.' }];

@Component({ template: '<h1>Catégories</h1>' })
class ListStub {}

async function setUp(path: string) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter(
        [
          { path: 'admin/taxonomy/categories', component: ListStub },
          {
            path: 'admin/taxonomy/categories/new',
            component: TermFormPage,
            data: { vocabularyKey: 'categories' },
            canDeactivate: [unsavedChangesGuard],
          },
          {
            path: 'admin/taxonomy/categories/:id',
            component: TermFormPage,
            data: { vocabularyKey: 'categories' },
            canDeactivate: [unsavedChangesGuard],
          },
        ],
        withComponentInputBinding(),
      ),
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(path);
  const http = TestBed.inject(HttpTestingController);
  const element = () => harness.routeNativeElement as HTMLElement;
  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await harness.fixture.whenStable();
    harness.detectChanges();
  };
  const control = (id: string) =>
    element().querySelector<HTMLInputElement | HTMLTextAreaElement>(`#${id}`)!;
  const type = (id: string, value: string) => {
    control(id).value = value;
    control(id).dispatchEvent(new Event('input'));
  };
  const submit = async () => {
    element()
      .querySelector('form')!
      .dispatchEvent(new Event('submit', { cancelable: true }));
    await settle();
  };
  return { http, element, settle, control, type, submit, router: TestBed.inject(Router) };
}

describe('TermFormPage', () => {
  it('creates a term without a slug, then notifies it and goes back to the list', async () => {
    const { http, element, type, submit, settle, router } = await setUp(
      '/admin/taxonomy/categories/new',
    );
    http.expectNone({ method: 'GET', url: API });
    expect(element().querySelector('h1')?.textContent).toBe('Nouvelle catégorie');

    type('terme-nom', '  Données  ');
    type('terme-description', 'Bases et requêtes.');
    await submit();
    const post = http.expectOne({ method: 'POST', url: API });
    expect(post.request.body).toEqual({ name: 'Données', description: 'Bases et requêtes.' });
    post.flush({ id: 8, name: 'Données', slug: 'donnees', description: 'Bases et requêtes.' });
    await settle();

    expect(router.url).toBe('/admin/taxonomy/categories');
    expect(TestBed.inject(Toaster).toasts()[0].message).toBe(
      'Catégorie «\u00a0Données\u00a0» créée.',
    );
  });

  it('starts from the saved term and sends the slug only when it changes', async () => {
    const { http, control, type, submit, settle } = await setUp('/admin/taxonomy/categories/7');
    http.expectOne(API).flush(CATEGORIES);
    await settle();
    expect(control('terme-nom').value).toBe('Backend');
    expect(control('terme-slug').value).toBe('backend');

    type('terme-nom', 'Backend Java');
    await submit();

    const put = http.expectOne({ method: 'PUT', url: `${API}/7` });
    expect(put.request.body).toEqual({ name: 'Backend Java', description: 'Spring Boot.' });
  });

  it('shows a name already used under the name field', async () => {
    const { http, element, type, submit, settle } = await setUp('/admin/taxonomy/categories/new');

    type('terme-nom', 'backend');
    await submit();
    http
      .expectOne({ method: 'POST', url: API })
      .flush({ status: 409, code: 'NAME_ALREADY_USED' }, { status: 409, statusText: 'Conflict' });
    await settle();
    await settle();

    expect(element().querySelector('#terme-nom-erreur')?.textContent).toContain(
      'Ce nom est déjà pris par une autre catégorie',
    );
    expect(TestBed.inject(DOCUMENT).activeElement?.id).toBe('terme-nom');
  });

  it('checks the fields before asking the server', async () => {
    const { http, element, type, submit } = await setUp('/admin/taxonomy/categories/new');

    type('terme-nom', '---');
    type('terme-slug', 'Pas Un Slug');
    await submit();

    http.expectNone({ method: 'POST' });
    expect(
      Array.from(element().querySelectorAll('.field-error')).map((error) =>
        error.textContent?.trim(),
      ),
    ).toEqual([
      'Le nom doit contenir au moins une lettre ou un chiffre.',
      'Lettres minuscules sans accent, chiffres et tirets seulement, par exemple «\u00a0backend-java\u00a0».',
    ]);
  });

  it('asks before leaving unsaved changes', async () => {
    const { element, type, router } = await setUp('/admin/taxonomy/categories/new');
    type('terme-nom', 'Brouillon');

    const leaving = router.navigateByUrl('/admin/taxonomy/categories');
    // La navigation attend la réponse : la page n'est pas stable avant
    await new Promise((resolve) => setTimeout(resolve));
    const dialog = element().querySelector('dialog')!;
    expect(dialog.open).toBe(true);
    expect(dialog.textContent).toContain('Quitter sans enregistrer');
    Array.from(dialog.querySelectorAll('button'))
      .find((button) => button.textContent?.trim() === 'Rester sur la page')!
      .click();

    expect(await leaving).toBe(false);
    expect(router.url).toBe('/admin/taxonomy/categories/new');
  });
});
