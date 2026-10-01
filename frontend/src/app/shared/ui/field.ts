import { booleanAttribute, Component, computed, Directive, inject, input } from '@angular/core';

import { Icon } from './icon';

/**
 * Champ de formulaire (02-design-system §18) : libellé visible, aide, contrôle natif projeté, erreur.
 * Le contrôle porte la directive `appFieldControl`, qui le relie au libellé, à l'aide et à l'erreur
 * (`id`, `aria-describedby`, `aria-invalid`). `controlId` est fourni par la page : un identifiant
 * stable, identique au rendu serveur et dans le navigateur.
 */
@Component({
  selector: 'app-field',
  imports: [Icon],
  host: { class: 'block' },
  template: `
    <label class="block font-semibold" [for]="controlId()">
      {{ label() }}
      @if (optional()) {
        <span class="font-normal text-ink-muted">(facultatif)</span>
      }
    </label>
    @if (hint()) {
      <p class="mt-1 text-sm text-ink-muted" [id]="hintId()">{{ hint() }}</p>
    }
    <div class="mt-2">
      <ng-content />
    </div>
    @if (error()) {
      <p class="field-error" [id]="errorId()">
        <app-icon name="circle-alert" class="mt-0.5" />
        <span>{{ error() }}</span>
      </p>
    }
  `,
  styles: `
    .field-error {
      display: flex;
      gap: calc(var(--spacing) * 2);
      margin-block-start: calc(var(--spacing) * 2);
      color: var(--color-danger);
      font-weight: var(--font-weight-medium);
    }
  `,
})
export class Field {
  readonly label = input.required<string>();
  readonly controlId = input.required<string>();
  readonly hint = input<string | null>(null);
  /** Message de la première erreur à afficher ; `null` : contrôle valide ou pas encore vérifié. */
  readonly error = input<string | null>(null);
  readonly optional = input(false, { transform: booleanAttribute });

  readonly hintId = computed(() => `${this.controlId()}-aide`);
  readonly errorId = computed(() => `${this.controlId()}-erreur`);

  /** L'erreur d'abord : c'est elle que le lecteur d'écran doit entendre en premier. */
  readonly describedBy = computed(() => {
    const ids = [this.error() ? this.errorId() : null, this.hint() ? this.hintId() : null];
    return ids.filter((id) => id !== null).join(' ') || null;
  });
}

/**
 * Contrôle natif d'un `app-field` : identifiant, description et état invalide viennent du champ ;
 * apparence commune des contrôles (02-design-system §18).
 */
@Directive({
  selector: 'input[appFieldControl], textarea[appFieldControl], select[appFieldControl]',
  host: {
    class: 'field-control',
    '[id]': 'field.controlId()',
    '[attr.aria-describedby]': 'field.describedBy()',
    '[attr.aria-invalid]': "field.error() ? 'true' : null",
  },
})
export class FieldControl {
  protected readonly field = inject(Field);
}
