import { Component } from '@angular/core';

import { MarkdownView } from '../shared/markdown/markdown-view';
import specimen from './markdown-specimen.md';

/** Spécimen du rendu Markdown (développement seulement, `/_ui/prose`). */
@Component({
  selector: 'app-prose-specimen',
  imports: [MarkdownView],
  template: `
    <main class="page-container pb-section">
      <header class="border-b border-rule py-section">
        <h1 class="text-3xl leading-tight tracking-title">Rendu Markdown</h1>
        <p class="mt-3 max-w-prose font-text text-lg text-ink-muted">
          Spécimen du moteur de rendu et des styles de lecture. Page de développement.
        </p>
      </header>
      <app-markdown-view class="mt-block" [source]="source" />
    </main>
  `,
})
export class ProseSpecimen {
  protected readonly source = specimen;
}
