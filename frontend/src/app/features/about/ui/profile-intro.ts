import { NgOptimizedImage } from '@angular/common';
import { Component, computed, input } from '@angular/core';

import { Profile } from '../../../core/api/api-types';
import { formatFileSize } from '../../../shared/format/file-size';
import { Button } from '../../../shared/ui/button';

/**
 * En-tête de la page : repère et portrait dans la colonne de gauche dès `lg`, puis nom (`<h1>`),
 * titre, lieu, présentation courte, CV et liens professionnels. Seules les données présentes
 * sont affichées ; l'adresse électronique publique ne l'est jamais (D-EA).
 */
@Component({
  selector: 'app-profile-intro',
  imports: [NgOptimizedImage, Button],
  host: { class: 'block' },
  template: `
    <div class="grid gap-6 pt-section pb-block lg:grid-cols-12 lg:gap-8">
      <div class="flex flex-col gap-6 lg:col-span-3 lg:pt-3">
        <p class="text-sm font-semibold text-ink-muted">À propos</p>
        @if (profile().avatar; as avatar) {
          <!-- Emplacement carré de taille fixe : l'image le remplit, recadrée sans déformation,
               sans décalage de mise en page. Au-dessus de la ligne de flottaison : prioritaire -->
          <div class="avatar">
            <img [ngSrc]="avatar.url" fill priority [alt]="avatar.altText ?? ''" />
          </div>
        }
      </div>
      <div class="min-w-0 lg:col-span-9">
        <h1 class="text-3xl leading-tight tracking-title">{{ profile().displayName }}</h1>
        <p class="mt-3 max-w-prose text-xl leading-snug tracking-heading">
          {{ profile().professionalTitle }}
        </p>
        @if (profile().publicLocation; as location) {
          <p class="mt-2 text-ink-muted">{{ location }}</p>
        }
        <p class="mt-flow max-w-prose font-text text-lg leading-prose">{{ profile().shortBio }}</p>
        @if (profile().cv || profile().links.length > 0) {
          <div class="mt-block flex flex-wrap items-center gap-x-8 gap-y-2">
            @if (profile().cv; as cv) {
              <a appButton variant="secondary" [href]="cv.url" [attr.download]="cvFileName()">
                Télécharger le CV
                <span class="font-normal text-ink-muted">(PDF, {{ size(cv.sizeBytes) }})</span>
              </a>
            }
            @if (profile().links.length > 0) {
              <ul class="flex flex-wrap gap-x-6" aria-label="Liens professionnels">
                @for (link of profile().links; track $index) {
                  <li>
                    <a class="external inline-flex min-h-11 items-center" [href]="link.url"
                      >{{ link.label }}<span class="sr-only"> (site externe)</span></a
                    >
                  </li>
                }
              </ul>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: `
    .avatar {
      position: relative;
      width: calc(var(--spacing) * 24);
      height: calc(var(--spacing) * 24);
      overflow: hidden;
      border-radius: var(--radius-full);
      background: var(--color-paper-sunken);
    }

    .avatar img {
      object-fit: cover;
    }

    @media (min-width: 64rem) {
      .avatar {
        width: calc(var(--spacing) * 40);
        height: calc(var(--spacing) * 40);
      }
    }
  `,
})
export class ProfileIntro {
  readonly profile = input.required<Profile>();

  /** Nom proposé à l'enregistrement : la clé de stockage du fichier ne dit rien au visiteur. */
  protected readonly cvFileName = computed(() => `CV ${this.profile().displayName}.pdf`);

  protected size(bytes: number): string {
    return formatFileSize(bytes);
  }
}
