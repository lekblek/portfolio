import { Component, input } from '@angular/core';

import { SkillGroup } from '../../../core/api/api-types';

/**
 * Compétences par catégorie (DS09) : une colonne par catégorie, 2 dès 30 rem de conteneur, 4 dès
 * 56 rem ; liste de définitions, compétences une par ligne. Noms techniques protégés de la
 * traduction automatique.
 */
@Component({
  selector: 'app-skill-groups',
  host: { class: '@container block' },
  template: `
    <dl class="skill-groups">
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
    .skill-groups {
      display: grid;
      gap: var(--spacing-flow) calc(var(--spacing) * 8);
    }

    .skill-group {
      display: grid;
      align-content: start;
      gap: calc(var(--spacing) * 2);
      padding-block-start: calc(var(--spacing) * 3);
      border-top: var(--border-strong) solid var(--color-ink);
    }

    .skill-list {
      display: grid;
      gap: calc(var(--spacing) * 1);
      color: var(--color-ink-muted);
    }

    @container (min-width: 30rem) {
      .skill-groups {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }

    @container (min-width: 56rem) {
      .skill-groups {
        grid-template-columns: repeat(4, minmax(0, 1fr));
      }
    }
  `,
})
export class SkillGroups {
  readonly groups = input.required<readonly SkillGroup[]>();
}
