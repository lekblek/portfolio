import { Component, input } from '@angular/core';

import { ProfileCertification } from '../../../core/api/api-types';
import { formatMonth } from '../../../shared/format/date';
import { CareerEntry } from './career-entry';

/**
 * Certifications, dans l'ordre du profil : date de délivrance en colonne, émetteur, fin de
 * validité si elle existe, lien vers le justificatif s'il est publié.
 */
@Component({
  selector: 'app-certification-list',
  imports: [CareerEntry],
  template: `
    <ol class="@container divide-y divide-rule">
      @for (certification of certifications(); track $index) {
        <li class="py-flow first:pt-0 last:pb-0">
          <app-career-entry
            [heading]="certification.name"
            [details]="[certification.issuer]"
            [when]="month(certification.issuedAt)"
          >
            @if (certification.expiresAt; as expiresAt) {
              <p class="text-sm text-ink-muted">Valable jusqu’en {{ month(expiresAt) }}</p>
            }
            @if (certification.credentialUrl; as credentialUrl) {
              <p class="mt-2">
                <a class="external" [href]="credentialUrl"
                  >Voir le justificatif<span class="sr-only">
                    de «&nbsp;{{ certification.name }}&nbsp;» (site externe)</span
                  ></a
                >
              </p>
            }
          </app-career-entry>
        </li>
      }
    </ol>
  `,
})
export class CertificationList {
  readonly certifications = input.required<readonly ProfileCertification[]>();

  protected month(date: string): string {
    return formatMonth(date);
  }
}
