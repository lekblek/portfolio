import {
  afterNextRender,
  Component,
  inject,
  Injector,
  input,
  signal,
  viewChild,
} from '@angular/core';
import {
  form,
  FormField,
  FormRoot,
  maxLength,
  required,
  TreeValidationResult,
} from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';

import { visibleError } from '../../../../shared/forms/visible-error';
import { retryAfterMinutes, toApiError } from '../../../../core/api/api-error';
import { Alert } from '../../../../shared/ui/alert';
import { Button } from '../../../../shared/ui/button';
import { Field, FieldControl } from '../../../../shared/ui/field';
import { AdminSession } from '../admin-session';
import { adminReturnUrl } from '../return-url';

/** Bornes du contrat (`OpenAdminSessionRequest`, D-CO). */
const LOGIN_MAX = 100;
const PASSWORD_MAX = 200;

interface Credentials {
  login: string;
  password: string;
}

interface Failure {
  title: string;
  text: string;
}

/**
 * Connexion à l'administration (`/admin/login`, rendu dans le navigateur seulement). La session
 * est d'abord demandée au serveur : déjà ouverte, la page mène à l'adresse de retour ; sinon, la
 * réponse dépose le jeton CSRF. Refus uniforme (401 : mot de passe effacé et focalisé), trop
 * d'essais (429, délai), serveur injoignable : message `role="alert"`, identifiant conservé.
 * Après une déconnexion ou une session expirée, le message reçoit le focus ; sinon, le champ
 * Identifiant.
 */
@Component({
  selector: 'app-login-page',
  imports: [Alert, Button, Field, FieldControl, FormField, FormRoot, RouterLink],
  template: `
    <main id="contenu" class="page-container" tabindex="-1">
      <div class="grid max-w-prose gap-block py-section">
        <div>
          <p class="text-sm font-semibold text-ink-muted">Administration</p>
          <h1 class="mt-2 text-3xl leading-tight tracking-title">Connexion</h1>
        </div>

        @switch (session.end()) {
          @case ('closed') {
            <app-alert #ended tone="success" title="Vous êtes déconnecté">
              <p>Reconnectez-vous pour reprendre l’administration.</p>
            </app-alert>
          }
          @case ('expired') {
            <app-alert #ended tone="danger" title="Session expirée">
              <p>Reconnectez-vous pour reprendre là où vous en étiez.</p>
            </app-alert>
          }
        }

        <form class="grid gap-6" [formRoot]="loginForm">
          <app-field
            label="Identifiant"
            controlId="connexion-identifiant"
            [error]="errorOf(loginForm.login)"
          >
            <input
              appFieldControl
              type="text"
              autocomplete="username"
              autocapitalize="none"
              spellcheck="false"
              [formField]="loginForm.login"
            />
          </app-field>
          <app-field
            label="Mot de passe"
            controlId="connexion-mot-de-passe"
            [error]="errorOf(loginForm.password)"
          >
            <input
              appFieldControl
              type="password"
              autocomplete="current-password"
              [formField]="loginForm.password"
            />
          </app-field>

          @if (failure(); as current) {
            <app-alert tone="danger" [title]="current.title">
              <p>{{ current.text }}</p>
            </app-alert>
          }
          <div>
            <button appButton type="submit" [loading]="loginForm().submitting()">
              Se connecter
            </button>
          </div>
        </form>

        <p class="text-sm">
          <a routerLink="/">Retour au site</a>
        </p>
      </div>
    </main>
  `,
})
export class LoginPage {
  /** Page demandée avant la connexion (paramètre de requête), filtrée par `adminReturnUrl`. */
  readonly returnUrl = input<string | undefined>();

  protected readonly session = inject(AdminSession);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  private readonly endNotice = viewChild<Alert>('ended');

  private readonly model = signal<Credentials>({ login: '', password: '' });

  protected readonly loginForm = form(
    this.model,
    (path) => {
      required(path.login, { message: 'Indiquez votre identifiant.' });
      maxLength(path.login, LOGIN_MAX, {
        message: `L’identifiant compte ${LOGIN_MAX} caractères au plus.`,
      });
      required(path.password, { message: 'Indiquez votre mot de passe.' });
      maxLength(path.password, PASSWORD_MAX, {
        message: `Le mot de passe compte ${PASSWORD_MAX} caractères au plus.`,
      });
    },
    {
      name: 'connexion',
      submission: {
        action: () => this.signIn(),
        onInvalid: () => this.focusFirstError(),
      },
    },
  );

  protected readonly failure = signal<Failure | null>(null);

  constructor() {
    void this.leaveIfAlreadyOpen();
    afterNextRender(() => {
      const notice = this.endNotice();
      if (notice) {
        notice.focus();
      } else {
        this.loginForm.login().focusBoundControl();
      }
    });
  }

  protected readonly errorOf = visibleError;

  private async leaveIfAlreadyOpen(): Promise<void> {
    try {
      if (await this.session.isOpen()) {
        await this.router.navigateByUrl(adminReturnUrl(this.returnUrl()), { replaceUrl: true });
      }
    } catch {
      this.failure.set(UNREACHABLE);
    }
  }

  private async signIn(): Promise<TreeValidationResult> {
    this.failure.set(null);
    try {
      await this.session.open(this.model());
    } catch (error) {
      this.failure.set(failureOf(error));
      // Refus : le mot de passe est effacé (sans erreur de champ en plus du message) et focalisé
      if (toApiError(error).code === 'INVALID_CREDENTIALS') {
        this.loginForm.password().reset('');
        afterNextRender(() => this.loginForm.password().focusBoundControl(), {
          injector: this.injector,
        });
      }
      return undefined;
    }
    await this.router.navigateByUrl(adminReturnUrl(this.returnUrl()));
    return undefined;
  }

  private focusFirstError(): void {
    afterNextRender(
      () => {
        const first = this.loginForm.login().invalid()
          ? this.loginForm.login
          : this.loginForm.password;
        first().focusBoundControl();
      },
      { injector: this.injector },
    );
  }
}

const UNREACHABLE: Failure = {
  title: 'Connexion impossible',
  text: 'Le serveur ne répond pas pour le moment\u202f: réessayez dans quelques instants.',
};

function failureOf(error: unknown): Failure {
  const failure = toApiError(error);
  if (failure.code === 'INVALID_CREDENTIALS') {
    return {
      title: 'Identifiant ou mot de passe incorrect',
      text: 'Vérifiez votre saisie, puis réessayez.',
    };
  }
  if (failure.code === 'TOO_MANY_LOGIN_ATTEMPTS') {
    const minutes = retryAfterMinutes(error);
    return {
      title: 'Trop de tentatives',
      text:
        minutes === null
          ? 'Réessayez plus tard.'
          : `Réessayez dans ${minutes} ${minutes === 1 ? 'minute' : 'minutes'}.`,
    };
  }
  return UNREACHABLE;
}
