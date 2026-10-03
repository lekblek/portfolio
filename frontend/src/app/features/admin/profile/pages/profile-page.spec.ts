import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { AdminMedia, AdminProfile } from '../../../../core/api/api-types';
import { Toaster } from '../../../../shared/ui/toast';
import { ProfilePage } from './profile-page';

const API = '/api/admin/profile';

const PROFILE: AdminProfile = {
  displayName: 'Ada Lovelace',
  professionalTitle: 'Développeuse',
  shortBio: 'Calculs et machines.',
  aboutMarkdown: '## Parcours',
  publicLocation: null,
  publicEmail: null,
  avatarMediaId: null,
  cvMediaId: null,
  links: [
    { label: 'GitHub', url: 'https://example.test/ada' },
    { label: 'Blog', url: 'https://example.test/blog' },
  ],
  skills: [{ name: 'Analyse', category: 'Mathématiques' }],
  experiences: [
    {
      organization: 'Analytical Engine',
      title: 'Programmeuse',
      location: 'Londres',
      startDate: '1842-01-01',
      endDate: null,
      description: '',
    },
  ],
  educations: [],
  certifications: [],
};

const PORTRAIT: AdminMedia = {
  id: 7,
  url: '/api/public/media/portrait.png',
  originalName: 'portrait.png',
  format: 'PNG',
  mimeType: 'image/png',
  sizeBytes: 50_000,
  width: 400,
  height: 400,
  altText: 'Portrait',
  createdAt: '2026-10-02T08:00:00Z',
};

@Component({ template: '<h1>Tableau de bord</h1>' })
class DashboardStub {}

async function setUp(answer: { profile: AdminProfile } | { status: number }) {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter([
        { path: 'admin', component: DashboardStub },
        { path: 'admin/profile', component: ProfilePage },
      ]),
    ],
  });
  const harness = await RouterTestingHarness.create();
  const page = await harness.navigateByUrl('/admin/profile', ProfilePage);
  const http = TestBed.inject(HttpTestingController);
  const request = http.expectOne(API);
  if ('profile' in answer) {
    request.flush(answer.profile);
  } else {
    request.flush({ status: answer.status }, { status: answer.status, statusText: 'Erreur' });
  }
  const element = () => harness.routeNativeElement as HTMLElement;
  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await harness.fixture.whenStable();
    harness.detectChanges();
  };
  await settle();
  const input = (id: string) => element().querySelector<HTMLInputElement>(`#${id}`)!;
  const type = (id: string, value: string) => {
    input(id).value = value;
    input(id).dispatchEvent(new Event('input'));
  };
  const button = (name: string) =>
    Array.from(element().querySelectorAll('button')).find(
      (candidate) => candidate.textContent?.replace(/\s+/g, ' ').trim() === name,
    )!;
  const submit = async () => {
    element()
      .querySelector('form')!
      .dispatchEvent(new Event('submit', { cancelable: true }));
    await settle();
  };
  // Requête en attente : `whenStable` attendrait sa réponse
  const tick = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    harness.detectChanges();
  };
  return { page, http, element, settle, tick, input, type, button, submit };
}

describe('ProfilePage', () => {
  it('fills the form with the saved profile, in the order of the lists', async () => {
    const { element, input } = await setUp({ profile: PROFILE });

    expect(input('profil-nom').value).toBe('Ada Lovelace');
    expect(input('profil-lien-0-libelle').value).toBe('GitHub');
    expect(input('profil-lien-1-libelle').value).toBe('Blog');
    expect(input('profil-experience-0-fin').value).toBe('');
    expect(element().querySelector('#profil-liens [role="list"]')?.getAttribute('aria-label')).toBe(
      'Liens professionnels',
    );
    expect(element().textContent).toContain('Aucune formation.');
  });

  it('starts from an empty form when no profile was ever saved', async () => {
    const { element, input } = await setUp({ status: 404 });

    expect(element().textContent).toContain('Aucun profil n’est encore enregistré');
    expect(input('profil-nom').value).toBe('');
  });

  it('offers to try again when the profile cannot be loaded', async () => {
    const { element } = await setUp({ status: 503 });

    expect(element().querySelector('[role="alert"]')?.textContent).toContain(
      'Le profil n’a pas pu être chargé.',
    );
    expect(element().querySelector('form')).toBeNull();
  });

  it('saves the whole profile, reordered, then notifies it', async () => {
    const { http, type, button, submit, settle, input } = await setUp({ profile: PROFILE });
    type('profil-nom', '  Ada King  ');
    button('Descendre le lien « GitHub »').click();
    await settle();

    await submit();
    const put = http.expectOne({ method: 'PUT', url: API });
    expect(put.request.body).toMatchObject({
      displayName: 'Ada King',
      aboutMarkdown: '## Parcours',
      publicLocation: undefined,
      links: [PROFILE.links[1], PROFILE.links[0]],
      experiences: [{ ...PROFILE.experiences[0], endDate: undefined }],
    });
    put.flush({ ...PROFILE, displayName: 'Ada King', links: [PROFILE.links[1], PROFILE.links[0]] });
    await settle();

    expect(TestBed.inject(Toaster).toasts()[0].message).toBe('Profil enregistré.');
    expect(input('profil-lien-0-libelle').value).toBe('Blog');
  });

  it('sends nothing while the form is invalid and focuses the first error', async () => {
    const { http, element, type, submit } = await setUp({ profile: PROFILE });
    type('profil-experience-0-fin', '1841-12-31');
    type('profil-nom', '');

    await submit();

    http.expectNone({ method: 'PUT' });
    expect(element().querySelector('#profil-experience-0-fin-erreur')?.textContent).toContain(
      'La fin précède le début.',
    );
    expect(document.activeElement?.id).toBe('profil-nom');
  });

  it('shows the errors of the server under their field, inside a list too', async () => {
    const { http, element, submit, settle } = await setUp({ profile: PROFILE });

    await submit();
    http.expectOne({ method: 'PUT', url: API }).flush(
      {
        status: 400,
        code: 'VALIDATION_FAILED',
        errors: [
          { field: 'experiences[0].endDate', message: 'La fin précède le début.' },
          { field: 'skills', message: 'Une compétence figure deux fois : Analyse.' },
        ],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await settle();

    expect(element().querySelector('#profil-competences .collection-error')?.textContent).toContain(
      'Une compétence figure deux fois : Analyse.',
    );
    expect(element().querySelector('#profil-experience-0-fin-erreur')?.textContent).toContain(
      'La fin précède le début.',
    );
  });

  it('keeps the input and explains when the server does not answer', async () => {
    const { http, element, submit, settle, input } = await setUp({ profile: PROFILE });

    await submit();
    http
      .expectOne({ method: 'PUT', url: API })
      .flush(null, { status: 503, statusText: 'Service Unavailable' });
    await settle();

    expect(element().querySelector('app-alert')?.textContent).toContain(
      'Votre saisie est conservée',
    );
    expect(input('profil-nom').value).toBe('Ada Lovelace');
  });

  it('chooses the avatar in the media library', async () => {
    const { http, element, button, settle, tick } = await setUp({ profile: PROFILE });

    button('Choisir l’avatar').click();
    await tick();
    http
      .expectOne((request) => request.url === '/api/admin/media')
      .flush({ content: [PORTRAIT], page: 0, size: 20, totalElements: 1, totalPages: 1 });
    await settle();
    element().querySelector<HTMLButtonElement>('dialog .media-picker-choice')!.click();
    await tick();
    http.expectOne('/api/admin/media/7').flush(PORTRAIT);
    await settle();

    expect(element().querySelector('#profil-medias')?.textContent).toContain('portrait.png');
    expect(button('Retirer l’avatar')).toBeDefined();
  });

  it('asks before leaving with unsaved changes', async () => {
    const { page, type, element } = await setUp({ profile: PROFILE });

    expect(page.canLeave()).toBe(true);
    type('profil-nom', 'Ada King');
    const answer = page.canLeave() as Promise<boolean>;
    await new Promise((resolve) => setTimeout(resolve));

    const dialog = element().querySelector('app-confirm-dialog dialog')!;
    expect(dialog.textContent).toContain('Quitter sans enregistrer');
    Array.from(dialog.querySelectorAll('button'))
      .find((candidate) => candidate.textContent?.trim() === 'Rester sur la page')!
      .click();
    expect(await answer).toBe(false);
  });
});
