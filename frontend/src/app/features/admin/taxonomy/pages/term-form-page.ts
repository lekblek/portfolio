import { HttpClient } from '@angular/common/http';
import {
  afterNextRender,
  Component,
  computed,
  effect,
  inject,
  Injector,
  input,
  signal,
  viewChild,
} from '@angular/core';
import {
  FieldTree,
  form,
  FormField,
  FormRoot,
  maxLength,
  pattern,
  required,
  TreeValidationResult,
} from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';

import { toApiError } from '../../../../core/api/api-error';
import { serverFieldErrors } from '../../../../shared/forms/server-errors';
import { UnsavedChanges } from '../../../../shared/forms/unsaved-changes.guard';
import { Alert } from '../../../../shared/ui/alert';
import { Button } from '../../../../shared/ui/button';
import { ConfirmDialog } from '../../../../shared/ui/confirm-dialog';
import { EmptyState } from '../../../../shared/ui/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state';
import { Field, FieldControl } from '../../../../shared/ui/field';
import { Toaster } from '../../../../shared/ui/toast';
import {
  createTerm,
  descriptionOf,
  displayOrderOf,
  Term,
  termsResource,
  updateTerm,
} from '../data/terms';
import {
  agree,
  capitalized,
  definite,
  newLabel,
  quoted,
  VOCABULARIES,
  VocabularyKey,
} from '../vocabularies';

/** Lettre ou chiffre : sans eux, aucun slug ne pourrait être généré (D-CS). */
const HAS_LETTER_OR_DIGIT = /[\p{L}\p{N}]/u;
const SLUG_FORMAT = /^[a-z0-9]+(-[a-z0-9]+)*$/;
const DESCRIPTION_MAX = 500;
const WHOLE_NUMBER = /^\d{1,9}$/;

interface TermModel {
  name: string;
  slug: string;
  description: string;
  displayOrder: string;
}

type TermField = keyof TermModel;

const EMPTY: TermModel = { name: '', slug: '', description: '', displayOrder: '' };

/**
 * Création et modification d'un terme (motif des formulaires d'administration) : page séparée de
 * la liste, champs aux bornes du contrat, slug facultatif (généré à la création, conservé s'il
 * n'est pas changé), erreurs du serveur rattachées à leur champ (nom ou slug déjà pris), panne en
 * tête du formulaire. Succès : notification et retour à la liste. Quitter une saisie non
 * enregistrée demande confirmation (navigation dans l'application) ou déclenche l'avertissement du
 * navigateur (fermeture, rechargement).
 */
@Component({
  selector: 'app-term-form-page',
  imports: [
    Alert,
    Button,
    ConfirmDialog,
    EmptyState,
    ErrorState,
    Field,
    FieldControl,
    FormField,
    FormRoot,
    RouterLink,
  ],
  host: { '(window:beforeunload)': 'warnBeforeUnload($event)' },
  template: `
    <p class="text-sm font-semibold text-ink-muted">
      Taxonomie ·
      <a class="font-medium" [routerLink]="listPath()">{{ vocabulary().plural }}</a>
    </p>
    <h1 class="mt-1 text-2xl tracking-heading">{{ title() }}</h1>

    @if (editing() && terms.error()) {
      <app-error-state [title]="'Le terme n’a pas pu être chargé.'" (retry)="terms.reload()" />
    } @else if (editing() && terms.hasValue() && !term()) {
      <app-empty-state class="mt-block block" [message]="missingText()">
        <a [routerLink]="listPath()">Retour à la liste</a>
      </app-empty-state>
    } @else if (editing() && !term()) {
      @defer (on timer(300ms)) {
        <p role="status" class="mt-block text-ink-muted">Chargement…</p>
      }
    } @else {
      <form class="mt-block grid max-w-prose gap-6" [formRoot]="termForm">
        <app-field label="Nom" controlId="terme-nom" [error]="errorOf(termForm.name)">
          <input appFieldControl type="text" autocomplete="off" [formField]="termForm.name" />
        </app-field>
        <app-field
          label="Slug"
          controlId="terme-slug"
          optional
          [hint]="slugHint()"
          [error]="errorOf(termForm.slug)"
        >
          <input
            appFieldControl
            type="text"
            autocomplete="off"
            autocapitalize="none"
            spellcheck="false"
            translate="no"
            [formField]="termForm.slug"
          />
        </app-field>
        @if (vocabulary().description) {
          <app-field
            label="Description"
            controlId="terme-description"
            optional
            hint="500 caractères au plus."
            [error]="errorOf(termForm.description)"
          >
            <textarea
              appFieldControl
              rows="4"
              autocomplete="off"
              [formField]="termForm.description"
            ></textarea>
          </app-field>
        }
        @if (vocabulary().displayOrder) {
          <app-field
            label="Ordre d’affichage"
            controlId="terme-ordre"
            optional
            hint="Les technologies sont rangées par ordre croissant, puis par nom. Vide : 0."
            [error]="errorOf(termForm.displayOrder)"
          >
            <input
              appFieldControl
              type="text"
              inputmode="numeric"
              autocomplete="off"
              [formField]="termForm.displayOrder"
            />
          </app-field>
        }

        @if (failure(); as text) {
          <app-alert tone="danger" title="L’enregistrement a échoué">
            <p>{{ text }}</p>
          </app-alert>
        }
        <div class="flex flex-wrap gap-3">
          <button appButton type="submit" [loading]="termForm().submitting()">
            {{ submitLabel() }}
          </button>
          <a appButton variant="secondary" [routerLink]="listPath()">Annuler</a>
        </div>
      </form>
    }

    <app-confirm-dialog #confirm />
  `,
})
export class TermFormPage implements UnsavedChanges {
  /** Vocabulaire de la route (donnée `vocabularyKey`). */
  readonly vocabularyKey = input.required<VocabularyKey>();
  /** Identifiant du terme à modifier (paramètre de route) ; absent à la création. */
  readonly id = input<string>();

  protected readonly vocabulary = computed(() => VOCABULARIES[this.vocabularyKey()]);
  protected readonly editing = computed(() => this.id() !== undefined);
  // Modification seulement : le terme est lu dans son vocabulaire (aucune route de lecture d'un terme)
  protected readonly terms = termsResource(() => (this.editing() ? this.vocabulary() : undefined));
  protected readonly term = computed(() =>
    this.terms.hasValue()
      ? this.terms.value().find((item) => String(item.id) === this.id())
      : undefined,
  );
  protected readonly listPath = computed(() => ['/admin/taxonomy', this.vocabulary().key]);

  protected readonly title = computed(() => {
    const vocabulary = this.vocabulary();
    if (!this.editing()) {
      return newLabel(vocabulary);
    }
    const term = this.term();
    return `Modifier ${definite(vocabulary)}${term ? ` ${quoted(term.name)}` : ''}`;
  });
  protected readonly submitLabel = computed(() =>
    this.editing() ? 'Enregistrer les modifications' : `Créer ${definite(this.vocabulary())}`,
  );
  protected readonly slugHint = computed(() =>
    this.editing()
      ? 'Identifiant dans les adresses du site. Le changer change les adresses filtrées par ce terme.'
      : 'Identifiant dans les adresses du site, en minuscules et tirets. Vide : tiré du nom.',
  );
  protected readonly missingText = computed(() => {
    const vocabulary = this.vocabulary();
    return `${vocabulary.feminine ? 'Cette' : 'Ce'} ${vocabulary.noun} n’existe pas ou n’existe plus.`;
  });

  private readonly model = signal<TermModel>({ ...EMPTY });
  private initialSlug = '';
  private saved = false;

  protected readonly termForm = form(
    this.model,
    (path) => {
      required(path.name, { message: 'Indiquez le nom.' });
      pattern(path.name, HAS_LETTER_OR_DIGIT, {
        message: 'Le nom doit contenir au moins une lettre ou un chiffre.',
      });
      maxLength(path.name, () => this.vocabulary().nameMax, {
        message: () => `Le nom compte ${this.vocabulary().nameMax} caractères au plus.`,
      });
      pattern(path.slug, SLUG_FORMAT, {
        message:
          'Lettres minuscules sans accent, chiffres et tirets seulement, par exemple «\u00a0backend-java\u00a0».',
      });
      maxLength(path.slug, () => this.vocabulary().slugMax, {
        message: () => `Le slug compte ${this.vocabulary().slugMax} caractères au plus.`,
      });
      maxLength(path.description, DESCRIPTION_MAX, {
        message: `La description compte ${DESCRIPTION_MAX} caractères au plus.`,
      });
      pattern(path.displayOrder, WHOLE_NUMBER, {
        message: 'Indiquez un nombre entier positif ou nul, par exemple 10.',
      });
    },
    {
      name: 'terme',
      submission: {
        action: () => this.save(),
        onInvalid: () => this.focusFirstError(),
      },
    },
  );

  protected readonly failure = signal<string | null>(null);

  private readonly confirm = viewChild.required<ConfirmDialog>('confirm');
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly toaster = inject(Toaster);
  private readonly injector = inject(Injector);

  constructor() {
    // Modification : la saisie part du terme enregistré, une seule fois
    let filled = false;
    effect(() => {
      const term = this.term();
      if (term && !filled) {
        filled = true;
        this.initialSlug = term.slug;
        this.termForm().reset({
          name: term.name,
          slug: term.slug,
          description: descriptionOf(term),
          displayOrder: String(displayOrderOf(term) ?? ''),
        });
      }
    });
    afterNextRender(() => {
      if (!this.editing()) {
        this.termForm.name().focusBoundControl();
      }
    });
  }

  /** Première erreur d'un champ, une fois le champ quitté ou le formulaire soumis. */
  protected errorOf(field: FieldTree<string>): string | null {
    const state = field();
    return state.touched() ? (state.errors()[0]?.message ?? null) : null;
  }

  canLeave(): boolean | Promise<boolean> {
    if (this.saved || !this.termForm().dirty()) {
      return true;
    }
    return this.confirm().ask({
      title: 'Quitter sans enregistrer\u202f?',
      message: 'Les modifications de cette page seront perdues.',
      confirmLabel: 'Quitter sans enregistrer',
      cancelLabel: 'Rester sur la page',
    });
  }

  protected warnBeforeUnload(event: BeforeUnloadEvent): void {
    if (!this.saved && this.termForm().dirty()) {
      event.preventDefault();
    }
  }

  private async save(): Promise<TreeValidationResult> {
    this.failure.set(null);
    const vocabulary = this.vocabulary();
    const { name, slug, description, displayOrder } = this.model();
    const request: { name: string; slug?: string; description?: string; displayOrder?: number } = {
      name: name.trim(),
    };
    if (slug.trim() !== '' && slug.trim() !== this.initialSlug) {
      request.slug = slug.trim();
    }
    if (vocabulary.description) {
      request.description = description.trim();
    }
    if (vocabulary.displayOrder && displayOrder.trim() !== '') {
      request.displayOrder = Number(displayOrder.trim());
    }
    let saved: Term;
    try {
      const id = this.term()?.id;
      saved =
        id === undefined
          ? await createTerm(this.http, vocabulary, request)
          : await updateTerm(this.http, vocabulary, id, request);
    } catch (error) {
      return this.handleFailure(error);
    }
    this.saved = true;
    this.toaster.show(
      `${capitalized(vocabulary.noun)} ${quoted(saved.name)} ${agree(vocabulary, this.editing() ? 'enregistré' : 'créé')}.`,
    );
    await this.router.navigate(this.listPath());
    return undefined;
  }

  private handleFailure(error: unknown): TreeValidationResult {
    const vocabulary = this.vocabulary();
    const { name, slug, description, displayOrder } = this.termForm;
    const fields: Record<TermField, FieldTree<string>> = { name, slug, description, displayOrder };
    const fieldErrors = serverFieldErrors(error, fields, {
      NAME_ALREADY_USED: {
        field: 'name',
        message: `Ce nom est déjà pris par ${vocabulary.feminine ? 'une autre' : 'un autre'} ${vocabulary.noun} (majuscules et minuscules confondues).`,
      },
      SLUG_ALREADY_USED: {
        field: 'slug',
        message: 'Ce slug est déjà pris\u202f: choisissez-en un autre.',
      },
    });
    if (fieldErrors.length > 0) {
      this.focusFirstError();
      return fieldErrors;
    }
    this.failure.set(
      toApiError(error).code === 'RESOURCE_NOT_FOUND'
        ? `${capitalized(definite(vocabulary))} n’existe plus\u202f: ${vocabulary.feminine ? 'elle' : 'il'} a pu être ${agree(vocabulary, 'supprimé')} entre-temps.`
        : 'Le serveur ne répond pas pour le moment. Votre saisie est conservée\u202f: réessayez dans quelques instants.',
    );
    return undefined;
  }

  /** Après le rendu des erreurs : le lecteur d'écran lit le champ avec son message. */
  private focusFirstError(): void {
    afterNextRender(
      () => {
        const order: TermField[] = ['name', 'slug', 'description', 'displayOrder'];
        const first = order.find((field) => this.termForm[field]().invalid());
        if (first) {
          this.termForm[first]().focusBoundControl();
        }
      },
      { injector: this.injector },
    );
  }
}
