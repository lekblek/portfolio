import { isPlatformBrowser } from '@angular/common';
import {
  afterRenderEffect,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  PLATFORM_ID,
} from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { MarkdownMath } from './markdown-math';
import { RenderedMarkdown, renderMarkdown } from './markdown-renderer';
import { observeDiagrams } from './mermaid-diagrams';

/**
 * Affiche un contenu Markdown (texte long d'un profil, d'un projet, d'une publication) avec les
 * styles de lecture `prose`. Le rendu est identique au serveur et dans le navigateur (formules
 * transmises par le rendu serveur, `MarkdownMath`). Feuille de style de KaTeX ajoutée si le
 * contenu a des formules ; diagrammes Mermaid dessinés dans le navigateur à leur approche.
 */
@Component({
  selector: 'app-markdown-view',
  host: { class: 'prose', '[innerHTML]': 'html()' },
  template: '',
})
export class MarkdownView {
  /** Markdown à rendre ; ignoré si `rendered` est fourni. */
  readonly source = input('');
  /** Rendu déjà calculé par la page (qui en tire aussi le sommaire) : un seul rendu. */
  readonly rendered = input<RenderedMarkdown | null>(null);
  /** Niveau du plus haut titre du contenu (`RenderOptions.topLevel`). */
  readonly headingLevel = input<2 | 3>(2);

  private readonly sanitizer = inject(DomSanitizer);
  private readonly math = inject(MarkdownMath);
  private readonly path = inject(Router).url.split(/[?#]/)[0];

  private readonly result = computed(
    () =>
      this.rendered() ??
      renderMarkdown(this.source(), {
        path: this.path,
        topLevel: this.headingLevel(),
        math: this.math.renderer(),
      }),
  );

  protected readonly html = computed(() =>
    // Seul contournement de l'assainisseur d'Angular (01-architecture §16) : il retire les `id`
    // des titres. Le HTML est sûr par construction : HTML brut échappé, liens filtrés, balises
    // produites par les seules règles de markdown-renderer.ts et par KaTeX (`trust: false`)
    // (ADR 0003, corpus des tests).
    this.sanitizer.bypassSecurityTrustHtml(this.result().html),
  );

  constructor() {
    effect(() => {
      if (this.result().hasMath) {
        this.math.ensureStylesheet();
      }
    });
    if (isPlatformBrowser(inject(PLATFORM_ID))) {
      const host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
      afterRenderEffect((onCleanup) => {
        if (this.result().hasDiagrams) {
          onCleanup(observeDiagrams(host));
        }
      });
    }
  }
}
