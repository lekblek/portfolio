import { HttpClient } from '@angular/common/http';
import {
  afterNextRender,
  Component,
  effect,
  inject,
  Injector,
  signal,
  viewChild,
} from '@angular/core';
import {
  form,
  FormField,
  FormRoot,
  maxLength,
  pattern,
  required,
  SchemaPath,
  TreeValidationResult,
  validate,
} from '@angular/forms/signals';

import { visibleError } from '../../../shared/forms/visible-error';
import { retryAfterMinutes, toApiError } from '../../../core/api/api-error';
import { Seo } from '../../../core/seo/seo';
import { serverFieldErrors } from '../../../shared/forms/server-errors';
import { Alert } from '../../../shared/ui/alert';
import { Button } from '../../../shared/ui/button';
import { Field, FieldControl } from '../../../shared/ui/field';
import { sendContactMessage } from '../data/contact-messages';

/** Bornes et forme d'adresse du contrat (D-CG, D-EJ). */
const NAME_MAX = 100;
const EMAIL_MAX = 254;
const SUBJECT_MAX = 200;
const MESSAGE_MAX = 5000;
const EMAIL_FORMAT = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

interface ContactModel {
  name: string;
  email: string;
  subject: string;
  message: string;
  /** Piège à robots : caché aux personnes, toujours vide pour elles. */
  website: string;
}

type VisibleField = Exclude<keyof ContactModel, 'website'>;

/** Ordre du formulaire : le focus va à la première erreur dans cet ordre. */
const FIELDS: readonly VisibleField[] = ['name', 'email', 'subject', 'message'];

const EMPTY: ContactModel = { name: '', email: '', subject: '', message: '', website: '' };

const DESCRIPTION =
  'Écrire à Blek Ngossanga\u202f: une question sur un projet, un article ou une collaboration.';

interface Failure {
  title: string;
  text: string;
}

/** Texte seulement fait d'espaces : vide pour le serveur, qui retire les espaces (D-CG). */
function notBlank(path: SchemaPath<string>, message: string): void {
  validate(path, ({ value }) =>
    value().length > 0 && value().trim() === '' ? { kind: 'blank', message } : undefined,
  );
}

/**
 * Contact : formulaire accessible (Signal Forms) validé avec les bornes du contrat, piège à robots
 * hors de l'arbre d'accessibilité, erreurs de champ du serveur rattachées à leur champ, focus sur
 * la première erreur, refus pour trop de messages, échec réseau sans perte du texte, succès qui
 * reçoit le focus. L'adresse électronique publique du profil n'est jamais affichée (D-EA).
 */
@Component({
  selector: 'app-contact-page',
  imports: [Alert, Button, Field, FieldControl, FormField, FormRoot],
  host: { class: 'block page-container wrap-break-word' },
  template: `
    <div class="grid gap-3 pt-section pb-section lg:grid-cols-12 lg:gap-8">
      <p class="text-sm font-semibold text-ink-muted lg:col-span-3 lg:pt-3">Contact</p>
      <div class="min-w-0 lg:col-span-9 lg:col-start-4">
        <h1 class="text-3xl leading-tight tracking-title">Écrire un message</h1>
        <p class="mt-flow max-w-prose font-text text-lg leading-prose">
          Une question sur un projet, un article ou une collaboration&#8239;: la réponse arrive à
          l’adresse électronique indiquée.
        </p>

        @if (sent()) {
          <app-alert #success class="mt-block max-w-prose" tone="success" title="Message envoyé">
            <p>Merci, votre message est bien arrivé.</p>
          </app-alert>
          <button
            appButton
            type="button"
            variant="secondary"
            class="mt-block"
            (click)="writeAgain()"
          >
            Écrire un autre message
          </button>
        } @else {
          <form class="mt-block grid max-w-prose gap-6" [formRoot]="contactForm">
            <p class="text-sm text-ink-muted">Tous les champs sont obligatoires.</p>
            <app-field label="Nom" controlId="contact-nom" [error]="errorOf(contactForm.name)">
              <input
                appFieldControl
                type="text"
                autocomplete="name"
                [formField]="contactForm.name"
              />
            </app-field>
            <app-field
              label="Adresse électronique"
              controlId="contact-adresse"
              hint="Pour vous répondre, par exemple nom@domaine.fr."
              [error]="errorOf(contactForm.email)"
            >
              <input
                appFieldControl
                type="email"
                inputmode="email"
                autocomplete="email"
                spellcheck="false"
                [formField]="contactForm.email"
              />
            </app-field>
            <app-field
              label="Sujet"
              controlId="contact-sujet"
              [error]="errorOf(contactForm.subject)"
            >
              <input
                appFieldControl
                type="text"
                autocomplete="off"
                [formField]="contactForm.subject"
              />
            </app-field>
            <app-field
              label="Message"
              controlId="contact-message"
              hint="5 000 caractères au plus."
              [error]="errorOf(contactForm.message)"
            >
              <textarea
                appFieldControl
                rows="8"
                autocomplete="off"
                [formField]="contactForm.message"
              ></textarea>
            </app-field>

            <!-- Piège à robots : hors de l'écran, de l'arbre d'accessibilité et de la tabulation -->
            <div class="contact-trap" aria-hidden="true">
              <label for="contact-site">Site web (à laisser vide)</label>
              <input
                id="contact-site"
                type="text"
                tabindex="-1"
                autocomplete="off"
                [formField]="contactForm.website"
              />
            </div>

            <p class="text-sm text-ink-muted">
              Votre nom, votre adresse électronique et votre message sont utilisés uniquement pour
              traiter votre demande et vous répondre. Ils ne sont jamais publiés sur le site et
              votre adresse IP n’est pas enregistrée.
            </p>
            @if (failure(); as current) {
              <app-alert tone="danger" [title]="current.title">
                <p>{{ current.text }}</p>
              </app-alert>
            }
            <div>
              <button appButton type="submit" [loading]="contactForm().submitting()">
                Envoyer le message
              </button>
            </div>
          </form>
        }
      </div>
    </div>
  `,
  styles: `
    .contact-trap {
      position: absolute;
      inset-inline-start: -10000px;
      width: 1px;
      height: 1px;
      overflow: hidden;
    }
  `,
})
export class ContactPage {
  private readonly model = signal<ContactModel>({ ...EMPTY });

  protected readonly contactForm = form(
    this.model,
    (path) => {
      required(path.name, { message: 'Indiquez votre nom.' });
      notBlank(path.name, 'Indiquez votre nom.');
      maxLength(path.name, NAME_MAX, { message: `Le nom compte ${NAME_MAX} caractères au plus.` });
      required(path.email, { message: 'Indiquez votre adresse électronique.' });
      pattern(path.email, EMAIL_FORMAT, {
        message: 'Indiquez une adresse complète, par exemple nom@domaine.fr.',
      });
      maxLength(path.email, EMAIL_MAX, {
        message: `L’adresse compte ${EMAIL_MAX} caractères au plus.`,
      });
      required(path.subject, { message: 'Indiquez le sujet du message.' });
      notBlank(path.subject, 'Indiquez le sujet du message.');
      maxLength(path.subject, SUBJECT_MAX, {
        message: `Le sujet compte ${SUBJECT_MAX} caractères au plus.`,
      });
      required(path.message, { message: 'Écrivez votre message.' });
      notBlank(path.message, 'Écrivez votre message.');
      maxLength(path.message, MESSAGE_MAX, {
        message: 'Le message compte 5\u00a0000 caractères au plus.',
      });
    },
    {
      name: 'contact',
      submission: {
        action: () => this.send(),
        onInvalid: () => this.focusFirstError(),
      },
    },
  );

  protected readonly sent = signal(false);
  protected readonly failure = signal<Failure | null>(null);

  private readonly success = viewChild<Alert>('success');
  private readonly http = inject(HttpClient);
  private readonly injector = inject(Injector);

  constructor() {
    const seo = inject(Seo);
    seo.set({ title: 'Contact', description: DESCRIPTION, path: '/contact' });
    // Le succès remplace le formulaire : il reçoit le focus dès qu'il est affiché
    effect(() => this.success()?.focus());
  }

  protected readonly errorOf = visibleError;

  protected writeAgain(): void {
    this.contactForm().reset({ ...EMPTY });
    this.sent.set(false);
    afterNextRender(() => this.contactForm.name().focusBoundControl(), {
      injector: this.injector,
    });
  }

  private async send(): Promise<TreeValidationResult> {
    this.failure.set(null);
    const { name, email, subject, message, website } = this.model();
    try {
      await sendContactMessage(this.http, {
        name: name.trim(),
        email: email.trim(),
        subject: subject.trim(),
        message: message.trim(),
        ...(website ? { website } : {}),
      });
    } catch (error) {
      return this.handleFailure(error);
    }
    this.sent.set(true);
    return undefined;
  }

  /** Erreurs de champ du serveur rattachées à leur champ ; refus et pannes dans un message. */
  private handleFailure(error: unknown): TreeValidationResult {
    const { name, email, subject, message } = this.contactForm;
    const fieldErrors = serverFieldErrors(error, { name, email, subject, message });
    if (fieldErrors.length > 0) {
      this.focusFirstError();
      return fieldErrors;
    }
    if (toApiError(error).code === 'TOO_MANY_CONTACT_MESSAGES') {
      this.failure.set({ title: 'Trop de messages envoyés', text: this.retryText(error) });
    } else {
      this.failure.set({
        title: 'Le message n’a pas pu être envoyé',
        text: 'Le serveur ne répond pas pour le moment. Votre texte est conservé\u202f: réessayez dans quelques instants.',
      });
    }
    return undefined;
  }

  private retryText(error: unknown): string {
    const minutes = retryAfterMinutes(error);
    if (minutes === null) {
      return 'Votre texte est conservé\u202f: réessayez plus tard.';
    }
    return `Votre texte est conservé\u202f: réessayez dans ${minutes} ${minutes === 1 ? 'minute' : 'minutes'}.`;
  }

  /** Après le rendu des erreurs : le lecteur d'écran lit le champ avec son message. */
  private focusFirstError(): void {
    afterNextRender(
      () => {
        const first = FIELDS.find((name) => this.contactForm[name]().invalid());
        if (first) {
          this.contactForm[first]().focusBoundControl();
        }
      },
      { injector: this.injector },
    );
  }
}
