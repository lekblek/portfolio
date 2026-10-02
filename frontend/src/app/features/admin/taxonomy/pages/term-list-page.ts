import { HttpClient } from '@angular/common/http';
import { Component, computed, ElementRef, inject, input, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';

import { toApiError } from '../../../../core/api/api-error';
import { Button } from '../../../../shared/ui/button';
import { ConfirmDialog } from '../../../../shared/ui/confirm-dialog';
import { DataTable } from '../../../../shared/ui/data-table';
import { EmptyState } from '../../../../shared/ui/empty-state';
import { ErrorState } from '../../../../shared/ui/error-state';
import { Toaster } from '../../../../shared/ui/toast';
import { deleteTerm, descriptionOf, displayOrderOf, Term, termsResource } from '../data/terms';
import { TaxonomyNav } from '../ui/taxonomy-nav';
import {
  agree,
  capitalized,
  definite,
  newLabel,
  quoted,
  VOCABULARIES,
  VocabularyKey,
} from '../vocabularies';

/**
 * Liste d'un vocabulaire (02-design-system §18, motif des listes d'administration) : titre, sous-
 * sections, nombre et action « Nouveau … », puis tableau (nom, slug, champs propres, actions).
 * Suppression : dialogue de confirmation qui nomme le terme, puis notification du résultat ; un
 * terme encore utilisé est refusé par l'API (`TERM_STILL_USED`) et la notification dit pourquoi.
 */
@Component({
  selector: 'app-term-list-page',
  imports: [Button, ConfirmDialog, DataTable, EmptyState, ErrorState, RouterLink, TaxonomyNav],
  template: `
    <p class="text-sm font-semibold text-ink-muted">Taxonomie</p>
    <h1 #heading class="mt-1 text-2xl tracking-heading" tabindex="-1">{{ vocabulary().plural }}</h1>
    <app-taxonomy-nav class="mt-flow block" />

    <div class="mt-block flex flex-wrap items-center justify-between gap-4">
      <p class="text-sm text-ink-muted">{{ countText() }}</p>
      <a appButton [routerLink]="['/admin/taxonomy', vocabulary().key, 'new']">{{ create() }}</a>
    </div>

    @if (terms.error(); as error) {
      <app-error-state
        [title]="'La liste n’a pas pu être chargée.'"
        [detail]="detailOf(error)"
        (retry)="terms.reload()"
      />
    } @else if (terms.hasValue()) {
      @if (terms.value().length === 0) {
        <app-empty-state class="mt-4 block" [message]="emptyText()" />
      } @else {
        <app-data-table class="mt-4" [label]="vocabulary().plural">
          <table class="data-table">
            <caption class="sr-only">
              {{
                vocabulary().plural
              }}
            </caption>
            <thead>
              <tr>
                <th scope="col">Nom</th>
                <th scope="col">Slug</th>
                @if (vocabulary().description) {
                  <th scope="col">Description</th>
                }
                @if (vocabulary().displayOrder) {
                  <th scope="col" class="data-table-number">Ordre</th>
                }
                <th scope="col" class="data-table-actions"><span class="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              @for (term of terms.value(); track term.id) {
                <tr>
                  <th scope="row">{{ term.name }}</th>
                  <td class="data-table-code" translate="no">{{ term.slug }}</td>
                  @if (vocabulary().description) {
                    <td class="text-sm text-ink-muted">{{ descriptionOf(term) }}</td>
                  }
                  @if (vocabulary().displayOrder) {
                    <td class="data-table-number">{{ orderOf(term) }}</td>
                  }
                  <td class="data-table-actions">
                    <a
                      appButton
                      variant="quiet"
                      size="sm"
                      [routerLink]="['/admin/taxonomy', vocabulary().key, term.id]"
                      >Modifier<span class="sr-only"> {{ theTerm(term) }}</span></a
                    >
                    <button
                      appButton
                      type="button"
                      variant="quiet"
                      size="sm"
                      (click)="remove(term)"
                    >
                      Supprimer<span class="sr-only"> {{ theTerm(term) }}</span>
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </app-data-table>
      }
    } @else {
      @defer (on timer(300ms)) {
        <p role="status" class="mt-4 text-ink-muted">Chargement…</p>
      }
    }

    <app-confirm-dialog #confirm />
  `,
})
export class TermListPage {
  /** Vocabulaire de la route (donnée `vocabularyKey`). */
  readonly vocabularyKey = input.required<VocabularyKey>();

  protected readonly vocabulary = computed(() => VOCABULARIES[this.vocabularyKey()]);
  protected readonly terms = termsResource(this.vocabulary);
  protected readonly create = computed(() => newLabel(this.vocabulary()));

  protected readonly countText = computed(() => {
    if (!this.terms.hasValue()) {
      return '';
    }
    const count = this.terms.value().length;
    const noun = this.vocabulary().noun;
    return count === 0 ? '' : `${count} ${count === 1 ? noun : `${noun}s`}`;
  });

  protected readonly emptyText = computed(() => {
    const vocabulary = this.vocabulary();
    return `Aucun${vocabulary.feminine ? 'e' : ''} ${vocabulary.noun} pour le moment.`;
  });

  private readonly confirm = viewChild.required<ConfirmDialog>('confirm');
  private readonly heading = viewChild.required<ElementRef<HTMLElement>>('heading');
  private readonly http = inject(HttpClient);
  private readonly toaster = inject(Toaster);

  protected theTerm(term: Term): string {
    return `${definite(this.vocabulary())} ${quoted(term.name)}`;
  }

  protected readonly descriptionOf = descriptionOf;
  protected readonly orderOf = displayOrderOf;

  protected detailOf(error: unknown): string | null {
    return toApiError(error).detail;
  }

  protected async remove(term: Term): Promise<void> {
    const vocabulary = this.vocabulary();
    const confirmed = await this.confirm().ask({
      title: `Supprimer ${this.theTerm(term)}\u202f?`,
      message:
        `${capitalized(definite(vocabulary))} ${quoted(term.name)} sera ${agree(vocabulary, 'supprimé')} ` +
        `définitivement. La suppression est refusée tant qu’${vocabulary.feminine ? 'elle' : 'il'} ` +
        `est ${agree(vocabulary, 'utilisé')} par ${vocabulary.usedBy}.`,
      confirmLabel: `Supprimer ${definite(vocabulary)}`,
    });
    if (!confirmed) {
      return;
    }
    try {
      await deleteTerm(this.http, vocabulary, term.id);
      this.toaster.show(
        `${capitalized(vocabulary.noun)} ${quoted(term.name)} ${agree(vocabulary, 'supprimé')}.`,
      );
    } catch (error) {
      this.toaster.show(this.refusal(term, error), 'danger');
    }
    this.terms.reload();
    // La ligne a disparu (ou la liste est rechargée) : le focus repart du titre
    this.heading().nativeElement.focus();
  }

  private refusal(term: Term, error: unknown): string {
    const vocabulary = this.vocabulary();
    const pronoun = vocabulary.feminine ? 'la' : 'le';
    switch (toApiError(error).code) {
      case 'TERM_STILL_USED':
        return (
          `${quoted(term.name)} n’a pas été ${agree(vocabulary, 'supprimé')}\u202f: ` +
          `${vocabulary.feminine ? 'elle' : 'il'} est encore ${agree(vocabulary, 'utilisé')} par ` +
          `${vocabulary.usedBy} au moins. Retirez-${pronoun} de ces contenus, puis réessayez.`
        );
      case 'RESOURCE_NOT_FOUND':
        return `${quoted(term.name)} n’existait plus\u202f: la liste est à jour.`;
      default:
        return `La suppression de ${quoted(term.name)} a échoué\u202f: réessayez dans quelques instants.`;
    }
  }
}
