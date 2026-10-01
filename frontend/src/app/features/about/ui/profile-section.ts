import { Component, input } from '@angular/core';

/**
 * Zone de la page : filet, repère (titre `<h2>`) dans la colonne de gauche dès `lg`, au-dessus
 * du contenu en dessous (02-design-system §4.3). Le contenu est projeté dans les neuf autres
 * colonnes. Empilé sur petit écran, le repère grandit (`text-2xl`) pour rester au-dessus des
 * titres de son contenu ; dans sa colonne, la position suffit à le distinguer (`text-lg`).
 */
@Component({
  selector: 'app-profile-section',
  host: { class: 'block' },
  template: `
    <section
      class="grid gap-4 border-t border-rule py-block lg:grid-cols-12 lg:gap-8"
      [attr.aria-labelledby]="headingId()"
    >
      <h2 [id]="headingId()" class="text-2xl tracking-heading lg:col-span-3 lg:text-lg">
        {{ heading() }}
      </h2>
      <div class="min-w-0 lg:col-span-9">
        <ng-content />
      </div>
    </section>
  `,
})
export class ProfileSection {
  readonly heading = input.required<string>();
  /** Identifiant du titre, préfixé `zone-` : jamais celui d'un titre du Markdown de la page. */
  readonly headingId = input.required<string>();
}
