import {
  CmdKey,
  defaultValueCtx,
  Editor,
  editorViewCtx,
  editorViewOptionsCtx,
  remarkStringifyOptionsCtx,
  rootCtx,
} from '@milkdown/kit/core';
import { history } from '@milkdown/kit/plugin/history';
import { listener, listenerCtx } from '@milkdown/kit/plugin/listener';
import {
  commonmark,
  toggleEmphasisCommand,
  toggleInlineCodeCommand,
  toggleLinkCommand,
  toggleStrongCommand,
  wrapInBlockquoteCommand,
  wrapInBulletListCommand,
  wrapInHeadingCommand,
  wrapInOrderedListCommand,
} from '@milkdown/kit/preset/commonmark';
import { gfm } from '@milkdown/kit/preset/gfm';
import { TextSelection } from '@milkdown/kit/prose/state';
import { callCommand, getMarkdown, markdownToSlice, replaceAll } from '@milkdown/kit/utils';

import { math } from './math';

/** Commandes de mise en forme de la barre d'outils (ADR 0004). */
export type FormatCommand =
  | 'heading2'
  | 'heading3'
  | 'strong'
  | 'emphasis'
  | 'code'
  | 'bulletList'
  | 'orderedList'
  | 'blockquote';

/** Éditeur visuel créé : lecture et écriture du Markdown, commandes, insertion. */
export interface MarkdownEditor {
  /** Markdown courant, tel que le réécrit remark. */
  markdown(): string;
  /** Remplace tout le contenu (Markdown modifié dans l'onglet Markdown). */
  setMarkdown(markdown: string): void;
  format(command: FormatCommand): void;
  /** Lien sur la sélection ; sans adresse, retire le lien. */
  link(href: string): void;
  /**
   * Insère des blocs de Markdown (image, bloc de code, formule, diagramme, tableau) après le bloc
   * du curseur, ou à sa place si c'est un paragraphe vide ; le curseur va à la fin du dernier
   * bloc inséré (dans un bloc de code vide : prêt pour la saisie).
   */
  insert(markdown: string): void;
  focus(): void;
  destroy(): Promise<void>;
}

export interface MarkdownEditorOptions {
  /** Nom accessible de la zone d'édition. */
  label: string;
  /** Identifiant de l'élément qui décrit la zone (aide, erreur), s'il y en a un. */
  describedBy?: string;
  /** Appelée à chaque modification du document, avec son Markdown. */
  onChange?: (markdown: string) => void;
}

/**
 * Options d'écriture de remark (ADR 0004, normalisations admises) : puces `-`, emphase `*`,
 * gras `**`, blocs de code clôturés par ``` ; les gestionnaires de Milkdown sont gardés.
 */
const STRINGIFY = {
  bullet: '-',
  emphasis: '*',
  strong: '*',
  fence: '`',
  fences: true,
  rule: '-',
} as const;

/**
 * Dollars littéraux (ADR 0004) : le moteur public ne voit pas de formule dans un `$` seul suivi
 * d'une espace ou d'une fin de ligne (« 5 $ et 6 $ », ADR 0003), alors que `remark-math` y verrait
 * `$ et 6 $`. Ces `$` sont écrits `\$` avant l'analyse, hors du code : le rendu public ne change
 * pas, la formule fantôme n'apparaît pas. Les `$$` et les `$` déjà échappés restent tels quels.
 */
export function protectLiteralDollars(markdown: string): string {
  const fence = /^(\s{0,3})(`{3,}|~{3,})/;
  let inFence: string | null = null;
  return markdown
    .split('\n')
    .map((line) => {
      const opening = fence.exec(line);
      if (inFence !== null) {
        if (opening && opening[2].startsWith(inFence)) {
          inFence = null;
        }
        return line;
      }
      if (opening) {
        inFence = opening[2];
        return line;
      }
      if (/^( {4}|\t)/.test(line)) {
        return line;
      }
      // Hors des portions de code en ligne (`…`)
      return line
        .split(/(`+[^`]*`+)/)
        .map((part, index) => (index % 2 === 1 ? part : protectInline(part)))
        .join('');
    })
    .join('\n');
}

/**
 * Règle des formules en ligne du moteur public (ADR 0003) : un `$` ouvre une formule s'il n'est
 * suivi ni d'un `$` ni d'une espace, et le `$` non échappé suivant la ferme s'il ne suit pas une
 * espace et ne précède pas un chiffre. Tout autre `$` seul est littéral, donc écrit `\$`.
 */
function protectInline(text: string): string {
  let out = '';
  let index = 0;
  while (index < text.length) {
    const char = text[index];
    if (char === '\\') {
      out += text.slice(index, index + 2);
      index += 2;
      continue;
    }
    if (char !== '$') {
      out += char;
      index++;
      continue;
    }
    if (text[index + 1] === '$') {
      out += '$$';
      index += 2;
      continue;
    }
    const end = formulaEnd(text, index);
    if (end !== -1) {
      out += text.slice(index, end + 1);
      index = end + 1;
      continue;
    }
    out += '\\$';
    index++;
  }
  return out;
}

/** Fin de la formule ouverte par le `$` en `start`, ou -1 s'il n'en ouvre pas. */
function formulaEnd(text: string, start: number): number {
  if (/\s/.test(text[start + 1] ?? ' ')) {
    return -1;
  }
  let end = start + 1;
  while ((end = text.indexOf('$', end)) !== -1 && text[end - 1] === '\\') {
    end++;
  }
  if (end === -1 || /\s/.test(text[end - 1]) || /\d/.test(text[end + 1] ?? '')) {
    return -1;
  }
  return end;
}

/**
 * Crée l'éditeur visuel (Milkdown Kit, ADR 0004) dans `root`, rempli avec `markdown`. Le Markdown
 * reste la source : l'éditeur le lit, le modifie et le réécrit, jamais de HTML conservé.
 */
export async function createMarkdownEditor(
  root: HTMLElement,
  markdown: string,
  options: MarkdownEditorOptions,
): Promise<MarkdownEditor> {
  const editor = await Editor.make()
    .config((ctx) => {
      ctx.set(rootCtx, root);
      ctx.set(defaultValueCtx, protectLiteralDollars(markdown));
      ctx.update(remarkStringifyOptionsCtx, (current) => ({ ...current, ...STRINGIFY }));
      ctx.update(editorViewOptionsCtx, (current) => ({
        ...current,
        attributes: {
          role: 'textbox',
          'aria-multiline': 'true',
          'aria-label': options.label,
          ...(options.describedBy ? { 'aria-describedby': options.describedBy } : {}),
          class: 'prose editor-surface',
          spellcheck: 'true',
          lang: 'fr',
        },
      }));
      const onChange = options.onChange;
      if (onChange) {
        ctx.get(listenerCtx).markdownUpdated((_, next, previous) => {
          if (next !== previous) {
            onChange(next);
          }
        });
      }
    })
    .use(commonmark)
    .use(gfm)
    .use(math)
    .use(history)
    .use(listener)
    .create();

  const run = <T>(key: CmdKey<T>, payload?: T) => {
    editor.action(callCommand(key, payload));
  };

  return {
    markdown: () => editor.action(getMarkdown()),
    setMarkdown: (next) => editor.action(replaceAll(protectLiteralDollars(next), true)),
    format: (command) => {
      switch (command) {
        case 'heading2':
          return run(wrapInHeadingCommand.key, 2);
        case 'heading3':
          return run(wrapInHeadingCommand.key, 3);
        case 'strong':
          return run(toggleStrongCommand.key);
        case 'emphasis':
          return run(toggleEmphasisCommand.key);
        case 'code':
          return run(toggleInlineCodeCommand.key);
        case 'bulletList':
          return run(wrapInBulletListCommand.key);
        case 'orderedList':
          return run(wrapInOrderedListCommand.key);
        case 'blockquote':
          return run(wrapInBlockquoteCommand.key);
      }
    },
    link: (href) => run(toggleLinkCommand.key, { href }),
    insert: (fragment) =>
      editor.action((ctx) => {
        const view = ctx.get(editorViewCtx);
        const { content } = markdownToSlice(fragment)(ctx);
        const { state } = view;
        const $from = state.selection.$from;
        const block = $from.depth > 0 ? $from.node(1) : null;
        const empty = block !== null && block.type.name === 'paragraph' && block.content.size === 0;
        const from = block === null ? state.doc.content.size : $from.before(1);
        const to = block === null ? from : $from.after(1);
        const tr = empty ? state.tr.replaceWith(from, to, content) : state.tr.insert(to, content);
        const end = (empty ? from : to) + content.size;
        tr.setSelection(TextSelection.near(tr.doc.resolve(Math.max(end - 1, 0)), -1));
        view.dispatch(tr.scrollIntoView());
      }),
    focus: () => editor.action((ctx) => ctx.get(editorViewCtx).focus()),
    destroy: async () => {
      await editor.destroy();
    },
  };
}
