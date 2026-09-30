import { Component, input } from '@angular/core';

import { ProfileExperience } from '../../../core/api/api-types';
import { formatPeriod } from '../../../shared/format/date';
import { CareerEntry } from './career-entry';

/** Expériences, dans l'ordre du profil ; une expérience sans date de fin est en cours. */
@Component({
  selector: 'app-experience-list',
  imports: [CareerEntry],
  template: `
    <ol class="@container divide-y divide-rule">
      @for (experience of experiences(); track $index) {
        <li class="py-flow first:pt-0 last:pb-0">
          <app-career-entry
            [heading]="experience.title"
            [details]="[experience.organization, experience.location]"
            [when]="period(experience)"
            [description]="experience.description"
          />
        </li>
      }
    </ol>
  `,
})
export class ExperienceList {
  readonly experiences = input.required<readonly ProfileExperience[]>();

  protected period(experience: ProfileExperience): string {
    return formatPeriod(experience.startDate, experience.endDate);
  }
}
