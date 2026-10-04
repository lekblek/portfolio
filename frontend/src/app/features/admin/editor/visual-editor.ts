import {
  afterNextRender,
  Component,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  output,
  signal,
  viewChild,
  ViewEncapsulation,
} from '@angular/core';

import { createMarkdownEditor, FormatCommand, MarkdownEditor } from './milkdown/markdown-editor';

/**
 * Éditeur visuel (Milkdown Kit, ADR 0004), chargé à la demande par l'éditeur de contenu (bloc
 * `@defer`) : seul ce composant importe Milkdown. Il lit le Markdown reçu et émet le Markdown
 * réécrit à chaque modification réelle.
 *
 * À l'ouverture, remark normalise le Markdown (puces, échappements…) : cette version de
 * référence n'est jamais émise. Revenir exactement à elle rend le texte d'origine, si bien
 * qu'afficher l'éditeur ne modifie jamais le formulaire.
 */
@Component({
  selector: 'app-visual-editor',
  host: { class: 'block' },
  // La surface est créée par ProseMirror, hors du gabarit : styles globaux bornés à `.editor-root`
  encapsulation: ViewEncapsulation.None,
  styles: `
    .editor-root .ProseMirror {
      min-height: calc(var(--spacing) * 96);
      padding: calc(var(--spacing) * 6);
      border: var(--border-strong) solid var(--color-ink);
      border-radius: var(--radius-control);
      background: var(--color-paper);
      white-space: pre-wrap;
      word-wrap: break-word;
      font-variant-ligatures: none;
    }

    .editor-root .ProseMirror:focus-visible {
      outline: 2px solid var(--color-focus);
      outline-offset: 2px;
    }

    .editor-root .ProseMirror-selectednode {
      outline: 2px solid var(--color-focus);
    }

    .editor-root .ProseMirror-hideselection *::selection {
      background: transparent;
    }

    .editor-root .ProseMirror img {
      max-width: 100%;
      height: auto;
    }

    /* Blocs de code et formules : source sur fond en retrait ; en édition, les longues lignes
       passent à la ligne (aucune zone qui défile, le curseur reste visible) */
    .editor-root .ProseMirror pre {
      padding: calc(var(--spacing) * 4);
      border: var(--border-rule) solid var(--color-rule);
      border-radius: var(--radius-media);
      background: var(--color-paper-sunken);
      font-family: var(--font-code);
      font-size: var(--text-xs);
      white-space: pre-wrap;
      overflow-wrap: anywhere;
    }

    /* Langage d'un bloc de code, et repère des formules : la source reste lisible et éditable */
    .editor-root pre[data-language]::before,
    .editor-root pre[data-math-block]::before {
      content: attr(data-language);
      display: block;
      margin-block-end: calc(var(--spacing) * 2);
      color: var(--color-ink-muted);
      font-family: var(--font-display);
      font-size: var(--text-xs);
    }

    .editor-root pre[data-math-block]::before {
      content: 'Formule';
    }

    .editor-root .editor-math-inline::before,
    .editor-root .editor-math-inline::after {
      content: '$';
      color: var(--color-ink-muted);
    }

    .editor-root .ProseMirror table {
      border-collapse: collapse;
    }

    .editor-root .ProseMirror td,
    .editor-root .ProseMirror th {
      min-width: calc(var(--spacing) * 16);
      padding: calc(var(--spacing) * 2) calc(var(--spacing) * 3);
      border: var(--border-rule) solid var(--color-rule);
    }
  `,
  template: `
    <div #root class="editor-root" [attr.aria-busy]="ready() ? null : 'true'"></div>
    @if (failed()) {
      <p role="alert" class="mt-2 text-danger">
        L’éditeur visuel n’a pas pu démarrer&#8239;: écrivez dans l’onglet Markdown.
      </p>
    }
  `,
})
export class VisualEditor {
  /** Markdown du formulaire ; une valeur venue d'ailleurs (onglet Markdown) remplace le contenu. */
  readonly markdown = input.required<string>();
  /** Nom accessible de la zone d'édition. */
  readonly label = input.required<string>();
  readonly describedBy = input<string | undefined>(undefined);
  readonly changed = output<string>();

  protected readonly ready = signal(false);
  protected readonly failed = signal(false);

  private readonly root = viewChild.required<ElementRef<HTMLElement>>('root');
  private editor: MarkdownEditor | null = null;
  /** Markdown d'origine et sa version normalisée par remark. */
  private source = '';
  private baseline = '';
  /** Markdown que l'éditeur représente (reçu ou émis) : sa valeur en retour ne recrée rien. */
  private shown = '';
  /** Remplacement en cours venu du formulaire : rien n'est émis. */
  private applying = false;

  constructor() {
    const destroyRef = inject(DestroyRef);
    afterNextRender(async () => {
      try {
        this.source = this.markdown();
        this.editor = await createMarkdownEditor(this.root().nativeElement, this.source, {
          label: this.label(),
          describedBy: this.describedBy(),
          onChange: (next) => this.emit(next),
        });
        this.baseline = this.editor.markdown();
        this.shown = this.source;
        this.ready.set(true);
      } catch {
        this.failed.set(true);
      }
    });
    destroyRef.onDestroy(() => void this.editor?.destroy());

    // Valeur modifiée hors de l'éditeur (onglet Markdown, insertion) : le contenu suit
    effect(() => {
      const next = this.markdown();
      if (!this.ready() || this.editor === null || next === this.shown) {
        return;
      }
      this.source = next;
      this.shown = next;
      this.applying = true;
      try {
        this.editor.setMarkdown(next);
        this.baseline = this.editor.markdown();
      } finally {
        this.applying = false;
      }
    });
  }

  format(command: FormatCommand): void {
    this.editor?.format(command);
    this.editor?.focus();
  }

  link(href: string): void {
    this.editor?.link(href);
    this.editor?.focus();
  }

  insert(markdown: string): void {
    this.editor?.insert(markdown);
    this.editor?.focus();
  }

  focus(): void {
    this.editor?.focus();
  }

  private emit(next: string): void {
    if (this.applying) {
      return;
    }
    const value = next === this.baseline ? this.source : next;
    if (value === this.shown) {
      return;
    }
    this.shown = value;
    this.changed.emit(value);
  }
}
