import { Component, input } from '@angular/core';

import { adminMediaResource } from '../../../../core/api/admin-media';

/**
 * Vignette d'une capture dans le formulaire d'un projet : l'image et son nom, lus dans la
 * médiathèque ; le texte alternatif reste celui du média (la légende, elle, se saisit à côté).
 */
@Component({
  selector: 'app-screenshot-preview',
  template: `
    @if (media.value(); as current) {
      <img
        class="screenshot-thumb"
        [src]="current.url"
        [width]="current.width"
        [height]="current.height"
        [alt]="current.altText ?? ''"
        loading="lazy"
        decoding="async"
      />
      <p class="mt-1 text-sm text-ink-muted wrap-break-word">{{ current.originalName }}</p>
    } @else {
      <span class="screenshot-thumb" aria-hidden="true"></span>
    }
  `,
  styles: `
    .screenshot-thumb {
      display: block;
      width: calc(var(--spacing) * 40);
      max-width: 100%;
      height: auto;
      aspect-ratio: 16 / 10;
      object-fit: cover;
      border: var(--border-rule) solid var(--color-rule);
      border-radius: var(--radius-media);
      background: var(--color-paper-sunken);
    }
  `,
})
export class ScreenshotPreview {
  readonly mediaId = input.required<number>();

  protected readonly media = adminMediaResource(() => this.mediaId());
}
