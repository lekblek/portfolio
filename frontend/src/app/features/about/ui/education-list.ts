import { Component, input } from '@angular/core';

import { ProfileEducation } from '../../../core/api/api-types';
import { formatPeriod } from '../../../shared/format/date';
import { CareerEntry } from './career-entry';

/** Formations, dans l'ordre du profil : diplôme, domaine, établissement et lieu, période. */
@Component({
  selector: 'app-education-list',
  imports: [CareerEntry],
  template: `
    <ol class="@container divide-y divide-rule">
      @for (education of educations(); track $index) {
        <li class="py-flow first:pt-0 last:pb-0">
          <app-career-entry
            [heading]="education.degree"
            [details]="[education.field, education.institution, education.location]"
            [when]="period(education)"
            [description]="education.description"
          />
        </li>
      }
    </ol>
  `,
})
export class EducationList {
  readonly educations = input.required<readonly ProfileEducation[]>();

  protected period(education: ProfileEducation): string {
    return formatPeriod(education.startDate, education.endDate);
  }
}
