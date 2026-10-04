import { Component, input } from '@angular/core';

import { ProfileCertification } from '../../../core/api/api-types';
import { formatMonth } from '../../../shared/format/date';
import { CareerEntry } from './career-entry';

/**
 * Certifications, dans l'ordre du profil, en planches à trait fort sur deux colonnes dès `md`
 * (DS09) : nom, émetteur, date de délivrance, fin de validité si elle existe, lien vers le
 * justificatif s'il est publié.
 */
@Component({
  selector: 'app-certification-list',
  imports: [CareerEntry],
  template: `
    <ol class="certifications">
      @for (certification of certifications(); track $index) {
        <li class="@container certification">
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
  styles: `
    .certifications {
      display: grid;
      gap: var(--spacing-flow) calc(var(--spacing) * 8);
    }

    .certification {
      padding-block-start: calc(var(--spacing) * 3);
      border-top: var(--border-strong) solid var(--color-ink);
    }

    @media (min-width: 48rem) {
      .certifications {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }
  `,
})
export class CertificationList {
  readonly certifications = input.required<readonly ProfileCertification[]>();

  protected month(date: string): string {
    return formatMonth(date);
  }
}
