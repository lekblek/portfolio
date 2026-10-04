import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { SITE_URL } from '../../../core/seo/site-config';
import { ContactPage } from './contact-page';

const URL = '/api/public/contact-messages';

/** Profil public réduit à ce que la page lit : liens professionnels et CV. */
const PROFILE = {
  links: [{ label: 'GitHub', url: 'https://github.com/blek' }],
  cv: { url: '/api/public/media/cv.pdf', sizeBytes: 36_400 },
};

async function setUp() {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter([{ path: 'contact', component: ContactPage }]),
      { provide: SITE_URL, useValue: 'https://blek.example' },
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl('/contact');
  const http = TestBed.inject(HttpTestingController);
  http.expectOne('/api/public/profile').flush(PROFILE);
  await harness.fixture.whenStable();
  harness.detectChanges();
  const element = () => harness.routeNativeElement as HTMLElement;
  const control = (id: string) =>
    element().querySelector<HTMLInputElement | HTMLTextAreaElement>(`#${id}`)!;
  const type = (id: string, value: string) => {
    const field = control(id);
    field.value = value;
    field.dispatchEvent(new Event('input'));
  };
  const fillValid = () => {
    type('contact-nom', '  Camille Martin ');
    type('contact-adresse', 'camille@example.com');
    type('contact-sujet', 'Proposition');
    type('contact-message', 'Bonjour,\nune mission ?');
  };
  const submit = async () => {
    element()
      .querySelector('form')!
      .dispatchEvent(new Event('submit', { cancelable: true }));
    await harness.fixture.whenStable();
    harness.detectChanges();
  };
  // La soumission se termine après la réponse, hors des tâches suivies par whenStable
  const settle = async () => {
    await new Promise((resolve) => setTimeout(resolve));
    await harness.fixture.whenStable();
    harness.detectChanges();
  };
  return { http, element, control, type, fillValid, submit, settle };
}

describe('ContactPage', () => {
  afterEach(() =>
    TestBed.inject(DOCUMENT)
      .head.querySelectorAll('meta, link[rel="canonical"], script[type="application/ld+json"]')
      .forEach((node) => node.remove()),
  );

  it('labels every field and keeps the trap away from people', async () => {
    const { element, control } = await setUp();

    const labels = Array.from(element().querySelectorAll('app-field label')).map((label) => [
      label.textContent?.trim(),
      label.getAttribute('for'),
    ]);
    expect(labels).toEqual([
      ['Nom', 'contact-nom'],
      ['Adresse électronique', 'contact-adresse'],
      ['Sujet', 'contact-sujet'],
      ['Message', 'contact-message'],
    ]);
    expect(control('contact-adresse').getAttribute('autocomplete')).toBe('email');
    expect(control('contact-adresse').getAttribute('aria-describedby')).toBe(
      'contact-adresse-aide',
    );
    expect(control('contact-message').getAttribute('maxlength')).toBe('5000');
    expect(control('contact-nom').hasAttribute('required')).toBe(true);
    const trap = control('contact-site');
    expect(trap.closest('[aria-hidden="true"]')).not.toBeNull();
    expect(trap.getAttribute('tabindex')).toBe('-1');
  });

  it('gives the context beside the form: welcome topics, professional links and the CV', async () => {
    const { element } = await setUp();

    const context = element().querySelector('aside')!;
    expect(context.getAttribute('aria-label')).toBe('Avant d’écrire');
    expect(context.textContent).toContain('Sujets bienvenus');
    expect(context.querySelector('a[href="https://github.com/blek"]')?.textContent).toContain(
      'GitHub',
    );
    expect(context.querySelector('a[href="/api/public/media/cv.pdf"]')).not.toBeNull();
  });

  it('says how the data of the form are used, in the words validated by the owner', async () => {
    const { element } = await setUp();

    expect(element().textContent?.replace(/\s+/g, ' ')).toContain(
      'Votre nom, votre adresse électronique et votre message sont utilisés uniquement pour ' +
        'traiter votre demande et vous répondre. Ils ne sont jamais publiés sur le site et ' +
        'votre adresse IP n’est pas enregistrée.',
    );
  });

  it('shows each error next to its field and moves the focus to the first one', async () => {
    const { http, element, control, type, submit } = await setUp();
    type('contact-adresse', 'camille');

    await submit();

    http.expectNone(URL);
    expect(control('contact-nom').getAttribute('aria-invalid')).toBe('true');
    expect(control('contact-nom').getAttribute('aria-describedby')).toBe('contact-nom-erreur');
    expect(element().querySelector('#contact-nom-erreur')?.textContent).toContain(
      'Indiquez votre nom.',
    );
    expect(element().querySelector('#contact-adresse-erreur')?.textContent).toContain(
      'nom@domaine.fr',
    );
    await vi.waitFor(() => expect(document.activeElement).toBe(control('contact-nom')));
  });

  it('refuses a name made of spaces only', async () => {
    const { http, element, fillValid, type, submit } = await setUp();
    fillValid();
    type('contact-nom', '   ');

    await submit();

    http.expectNone(URL);
    expect(element().querySelector('#contact-nom-erreur')?.textContent).toContain(
      'Indiquez votre nom.',
    );
  });

  it('sends the trimmed message, then gives the focus to the confirmation', async () => {
    const { http, element, fillValid, submit, settle } = await setUp();
    fillValid();

    await submit();
    const request = http.expectOne(URL);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      name: 'Camille Martin',
      email: 'camille@example.com',
      subject: 'Proposition',
      message: 'Bonjour,\nune mission ?',
    });
    request.flush(null, { status: 202, statusText: 'Accepted' });
    await settle();

    const success = element().querySelector('app-alert');
    expect(success?.textContent).toContain('Message envoyé');
    expect(element().querySelector('form')).toBeNull();
    await vi.waitFor(() => expect(document.activeElement).toBe(success));
  });

  it('attaches the field errors of the server to their field', async () => {
    const { http, element, control, fillValid, submit, settle } = await setUp();
    fillValid();

    await submit();
    http.expectOne(URL).flush(
      {
        status: 400,
        code: 'VALIDATION_FAILED',
        errors: [{ field: 'email', message: 'adresse refusée par le serveur' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await settle();

    expect(element().querySelector('#contact-adresse-erreur')?.textContent).toContain(
      'adresse refusée par le serveur',
    );
    expect(control('contact-adresse').getAttribute('aria-invalid')).toBe('true');
    await vi.waitFor(() => expect(document.activeElement).toBe(control('contact-adresse')));
  });

  it('says how long to wait after too many messages, and keeps the text', async () => {
    const { http, element, control, fillValid, submit, settle } = await setUp();
    fillValid();

    await submit();
    http
      .expectOne(URL)
      .flush(
        { status: 429, code: 'TOO_MANY_CONTACT_MESSAGES' },
        { status: 429, statusText: 'Too Many Requests', headers: { 'Retry-After': '2520' } },
      );
    await settle();

    const alert = element().querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Trop de messages envoyés');
    expect(alert?.textContent).toContain('réessayez dans 42 minutes');
    expect(control('contact-message').value).toBe('Bonjour,\nune mission ?');
  });

  it('sends the trap when a robot fills it', async () => {
    const { http, fillValid, type, submit } = await setUp();
    fillValid();
    type('contact-site', 'https://spam.example');

    await submit();

    expect(http.expectOne(URL).request.body.website).toBe('https://spam.example');
  });
});
