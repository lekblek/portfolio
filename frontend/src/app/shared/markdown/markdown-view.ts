import { Component, computed, inject, input } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { renderMarkdown } from './markdown-renderer';

/**
 * Affiche un contenu Markdown (texte long d'un profil, d'un projet, d'une publication) avec les
 * styles de lecture `prose`. Le rendu est identique au serveur et dans le navigateur.
 */
@Component({
  selector: 'app-markdown-view',
  host: { class: 'prose', '[innerHTML]': 'html()' },
  template: '',
})
export class MarkdownView {
  readonly source = input.required<string>();
  /** Niveau du plus haut titre du contenu (`RenderOptions.topLevel`). */
  readonly headingLevel = input<2 | 3>(2);

  private readonly sanitizer = inject(DomSanitizer);
  private readonly path = inject(Router).url.split(/[?#]/)[0];

  protected readonly html = computed(() =>
    // Seul contournement de l'assainisseur d'Angular (01-architecture §16) : il retire les `id`
    // des titres. Le HTML est sûr par construction : HTML brut échappé, liens filtrés, balises
    // produites par les seules règles de markdown-renderer.ts (ADR 0003, corpus des tests).
    this.sanitizer.bypassSecurityTrustHtml(
      renderMarkdown(this.source(), { path: this.path, topLevel: this.headingLevel() }).html,
    ),
  );
}
