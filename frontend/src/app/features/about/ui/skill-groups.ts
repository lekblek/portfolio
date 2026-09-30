import { Component, input } from '@angular/core';

import { SkillGroup } from '../../../core/api/api-types';

/**
 * Compétences par catégorie : liste de définitions, catégorie en colonne dès que la liste
 * dispose de 36 rem. Noms techniques protégés de la traduction automatique.
 */
@Component({
  selector: 'app-skill-groups',
  template: `
    <dl class="@container">
      @for (group of groups(); track $index) {
        <div class="skill-group">
          <dt class="font-semibold">{{ group.category }}</dt>
          <dd>
            <ul class="skill-list" translate="no">
              @for (skill of group.skills; track $index) {
                <li>{{ skill.name }}</li>
              }
            </ul>
          </dd>
        </div>
      }
    </dl>
  `,
  styles: `
    .skill-group {
      display: grid;
      grid-template-columns: minmax(0, 1fr);
      column-gap: calc(var(--spacing) * 8);
      row-gap: calc(var(--spacing) * 1);
      padding-block: calc(var(--spacing) * 3);
      border-top: var(--border-rule) solid var(--color-rule);
    }

    .skill-group:first-child {
      padding-block-start: 0;
      border-top: 0;
    }

    .skill-group:last-child {
      padding-block-end: 0;
    }

    .skill-list {
      display: flex;
      flex-wrap: wrap;
      column-gap: calc(var(--spacing) * 2);
    }

    /* Séparateur visible, sans texte de remplacement : la liste énonce déjà chaque élément */
    .skill-list > li:not(:last-child)::after {
      content: '·' / '';
      margin-inline-start: calc(var(--spacing) * 2);
      color: var(--color-ink-muted);
    }

    @container (min-width: 36rem) {
      .skill-group {
        grid-template-columns: 12rem minmax(0, 1fr);
        align-items: baseline;
      }
    }
  `,
})
export class SkillGroups {
  readonly groups = input.required<readonly SkillGroup[]>();
}
