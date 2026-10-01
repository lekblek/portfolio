import { Component, input } from '@angular/core';

/**
 * Zone de l'accueil : filet, repère (titre `<h2>`) dans la colonne de gauche dès `lg`, contenu
 * dans les neuf autres colonnes (02-design-system §4.3), comme les zones de la page À propos.
 */
@Component({
  selector: 'app-home-zone',
  host: { class: 'block' },
  template: `
    <section
      class="grid gap-4 border-t border-rule py-block lg:grid-cols-12 lg:gap-8"
      [attr.aria-labelledby]="headingId()"
    >
      <h2 [id]="headingId()" class="text-2xl tracking-heading lg:col-span-3 lg:text-lg">
        {{ heading() }}
      </h2>
      <div class="@container min-w-0 lg:col-span-9">
        <ng-content />
      </div>
    </section>
  `,
})
export class HomeZone {
  readonly heading = input.required<string>();
  /** Identifiant du titre, préfixé `zone-`. */
  readonly headingId = input.required<string>();
}
