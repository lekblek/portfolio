import {
  afterNextRender,
  Component,
  computed,
  ElementRef,
  inject,
  Injector,
  input,
  model,
  signal,
  viewChild,
} from '@angular/core';
import { FormValueControl } from '@angular/forms/signals';

import { mediaPickerPageResource } from '../../../core/api/admin-media';
import { AdminMedia } from '../../../core/api/api-types';
import { MarkdownView } from '../../../shared/markdown/markdown-view';
import { Button } from '../../../shared/ui/button';
import { Field, FieldControl } from '../../../shared/ui/field';
import { MediaPicker } from '../../../shared/ui/media-picker';
import { TABS } from '../../../shared/ui/tabs';
import {
  codeBlock,
  CODE_LANGUAGES,
  imageMarkdown,
  MATH_TEMPLATE,
  MERMAID_TEMPLATE,
  TABLE_TEMPLATE,
} from './snippets';
import { VisualEditor } from './visual-editor';
import type { FormatCommand } from './milkdown/markdown-editor';

type EditorTab = 'visuel' | 'markdown' | 'apercu';

/** Action de la barre d'outils : mise en forme directe, ou insertion (avec dialogue au besoin). */
interface ToolbarAction {
  label: string;
  run: () => void;
}

/** Dialogue d'insertion ouvert : lien, bloc de code ou image choisie. */
type Insertion = { kind: 'link' } | { kind: 'code' } | { kind: 'image'; media: AdminMedia };

/**
 * Éditeur du contenu d'une publication (F31, ADR 0004) : onglets **Visuel** (Milkdown, chargé à
 * la demande), **Markdown** (zone de texte, alternative complète et accessible) et **Aperçu** (le
 * moteur du site). Contrôle de Signal Forms : la valeur est le Markdown, seule source ; aucun
 * onglet ne perd la saisie (les trois panneaux restent dans le document).
 *
 * Barre d'outils (motif *toolbar* de l'APG : un seul arrêt de tabulation, flèches, Début, Fin) :
 * mise en forme dans l'onglet Visuel ; « Image » dans les deux onglets d'écriture, par le
 * sélecteur de la médiathèque, avec le texte alternatif repris ou saisi et une légende
 * facultative (figure numérotée sur le site, D-EV).
 */
@Component({
  selector: 'app-content-editor',
  imports: [Button, Field, FieldControl, MarkdownView, MediaPicker, VisualEditor, ...TABS],
  host: { class: 'block' },
  templateUrl: './content-editor.html',
  styleUrl: './content-editor.css',
})
export class ContentEditor implements FormValueControl<string> {
  readonly value = model('');
  readonly touched = model(false);

  /** Identifiant de la zone de texte Markdown (`publication-texte`) ; préfixe des autres. */
  readonly controlId = input.required<string>();
  readonly label = input('Contenu');
  readonly hint = input<string | null>(null);
  /** Erreur visible du champ (validation ou serveur). */
  readonly error = input<string | null>(null);

  protected readonly tab = signal<EditorTab>('visuel');
  protected readonly visual = viewChild<VisualEditor>('visual');
  private readonly textarea = viewChild<ElementRef<HTMLTextAreaElement>>('source');
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private readonly picker = viewChild.required<MediaPicker>('picker');
  private readonly injector = inject(Injector);

  protected readonly languages = CODE_LANGUAGES;
  protected readonly hintId = computed(() => `${this.controlId()}-editeur-aide`);
  protected readonly errorId = computed(() => `${this.controlId()}-editeur-erreur`);

  /** Mise en forme (onglet Visuel). */
  protected readonly formatActions: readonly (ToolbarAction & { command?: FormatCommand })[] = [
    { label: 'Titre', run: () => this.format('heading2') },
    { label: 'Sous-titre', run: () => this.format('heading3') },
    { label: 'Gras', run: () => this.format('strong') },
    { label: 'Italique', run: () => this.format('emphasis') },
    { label: 'Code', run: () => this.format('code') },
    { label: 'Lien', run: () => this.openInsertion({ kind: 'link' }) },
    { label: 'Liste', run: () => this.format('bulletList') },
    { label: 'Liste numérotée', run: () => this.format('orderedList') },
    { label: 'Citation', run: () => this.format('blockquote') },
    { label: 'Bloc de code', run: () => this.openInsertion({ kind: 'code' }) },
    { label: 'Formule', run: () => this.insert(MATH_TEMPLATE) },
    { label: 'Diagramme', run: () => this.insert(MERMAID_TEMPLATE) },
    { label: 'Tableau', run: () => this.insert(TABLE_TEMPLATE) },
    { label: 'Image', run: () => this.chooseImage() },
  ];
  /** Onglet Markdown : la syntaxe s'écrit directement ; seule l'image passe par la médiathèque. */
  protected readonly sourceActions: readonly ToolbarAction[] = [
    { label: 'Image', run: () => this.chooseImage() },
  ];

  /** Dialogue d'insertion ouvert et ses champs. */
  protected readonly insertion = signal<Insertion | null>(null);
  protected readonly href = signal('');
  protected readonly language = signal<string>(CODE_LANGUAGES[0]);
  protected readonly altText = signal('');
  protected readonly caption = signal('');
  protected readonly insertionError = signal<string | null>(null);

  protected readonly pickerPage = signal<number | null>(null);
  protected readonly pickerMedia = mediaPickerPageResource(this.pickerPage);
  protected readonly pickerState = computed(() =>
    this.pickerMedia.error() ? 'error' : this.pickerMedia.hasValue() ? 'ready' : 'loading',
  );

  /** Position du curseur dans la zone Markdown, gardée pendant le dialogue. */
  private sourceSelection: [number, number] | null = null;
  /** Index de l'action focalisable dans chaque barre (tabindex itinérant). */
  protected readonly activeAction = signal(0);

  protected fromVisual(markdown: string): void {
    this.value.set(markdown);
  }

  protected fromSource(event: Event): void {
    this.value.set((event.target as HTMLTextAreaElement).value);
  }

  protected markTouched(): void {
    this.touched.set(true);
  }

  /** Flèches, Début, Fin dans une barre d'outils : le focus passe d'une action à l'autre. */
  protected toolbarKeys(event: KeyboardEvent, count: number): void {
    const keys: Record<string, (index: number) => number> = {
      ArrowRight: (index) => (index + 1) % count,
      ArrowLeft: (index) => (index - 1 + count) % count,
      Home: () => 0,
      End: () => count - 1,
    };
    const move = keys[event.key];
    if (!move) {
      return;
    }
    event.preventDefault();
    const next = move(this.activeAction());
    this.activeAction.set(next);
    const toolbar = (event.currentTarget as HTMLElement).closest('[role="toolbar"]');
    toolbar?.querySelectorAll<HTMLButtonElement>('button')[next]?.focus();
  }

  protected closeInsertion(): void {
    this.dialog().nativeElement.close();
  }

  protected onDialogClose(): void {
    this.insertion.set(null);
    this.insertionError.set(null);
    this.restoreFocus();
  }

  protected confirmInsertion(event: Event): void {
    event.preventDefault();
    const current = this.insertion();
    if (current === null) {
      return;
    }
    switch (current.kind) {
      case 'link': {
        const href = this.href().trim();
        if (!/^(https?:\/\/|\/|#|mailto:)/.test(href)) {
          this.insertionError.set(
            'Indiquez une adresse complète (https://…) ou un chemin du site (/…).',
          );
          return;
        }
        this.dialog().nativeElement.close();
        this.visual()?.link(href);
        return;
      }
      case 'code':
        this.dialog().nativeElement.close();
        this.insert(codeBlock(this.language()));
        return;
      case 'image': {
        const alt = this.altText().trim();
        if (alt === '') {
          this.insertionError.set(
            'Décrivez l’image : le texte alternatif est lu à la place de l’image.',
          );
          return;
        }
        this.dialog().nativeElement.close();
        this.insert(imageMarkdown(current.media.url, alt, this.caption().trim()));
        return;
      }
    }
  }

  /** Erreur signalée hors de l'onglet Markdown : on y passe, le focus va dans la zone de texte. */
  protected showSource(): void {
    this.tab.set('markdown');
    afterNextRender(() => this.textarea()?.nativeElement.focus(), { injector: this.injector });
  }

  protected mediaChosen(media: AdminMedia): void {
    this.openInsertion({ kind: 'image', media });
  }

  private format(command: FormatCommand): void {
    this.visual()?.format(command);
  }

  private chooseImage(): void {
    this.rememberSourceSelection();
    this.pickerPage.set(1);
    this.picker().open();
  }

  private openInsertion(insertion: Insertion): void {
    this.rememberSourceSelection();
    this.insertionError.set(null);
    if (insertion.kind === 'link') {
      this.href.set('');
    } else if (insertion.kind === 'image') {
      this.altText.set(insertion.media.altText ?? '');
      this.caption.set('');
    }
    this.insertion.set(insertion);
    afterNextRender(() => this.dialog().nativeElement.showModal(), { injector: this.injector });
  }

  /** Insère du Markdown à l'endroit de la saisie, dans l'onglet d'écriture courant. */
  private insert(markdown: string): void {
    if (this.tab() === 'visuel') {
      this.visual()?.insert(markdown);
      return;
    }
    const current = this.value();
    const [start, end] = this.sourceSelection ?? [current.length, current.length];
    const before = current.slice(0, start);
    const after = current.slice(end);
    // Un bloc commence et finit sur sa propre ligne, entouré de lignes vides
    const block = markdown.includes('\n') || markdown.startsWith('!');
    const prefix =
      block && before.length > 0 && !before.endsWith('\n\n')
        ? before.endsWith('\n')
          ? '\n'
          : '\n\n'
        : '';
    const suffix =
      block && after.length > 0 && !after.startsWith('\n\n')
        ? after.startsWith('\n')
          ? '\n'
          : '\n\n'
        : '';
    const inserted = `${prefix}${markdown}${suffix}`;
    this.value.set(before + inserted + after);
    const caret = before.length + inserted.length;
    this.sourceSelection = [caret, caret];
    afterNextRender(
      () => {
        const field = this.textarea()?.nativeElement;
        field?.focus();
        field?.setSelectionRange(caret, caret);
      },
      { injector: this.injector },
    );
  }

  private rememberSourceSelection(): void {
    const field = this.textarea()?.nativeElement;
    if (this.tab() === 'markdown' && field) {
      this.sourceSelection = [field.selectionStart, field.selectionEnd];
    }
  }

  private restoreFocus(): void {
    if (this.tab() === 'visuel') {
      this.visual()?.focus();
    } else {
      this.textarea()?.nativeElement.focus();
    }
  }
}
