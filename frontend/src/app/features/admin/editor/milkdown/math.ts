import { $nodeSchema, $remark } from '@milkdown/kit/utils';
import remarkMath from 'remark-math';

/**
 * Formules dans l'éditeur visuel (ADR 0004) : `remark-math` lit `$…$` et `$$…$$` (nœuds mdast
 * `inlineMath` et `math`) ; deux nœuds ProseMirror les gardent comme source éditable, sans
 * rendu (l'Aperçu rend avec KaTeX, comme le site), et les réécrivent à l'identique. Sans eux,
 * une formule deviendrait du texte, et remark échapperait ses `\`, `_` et `$`.
 */
export const remarkMathPlugin = $remark('remarkMath', () => remarkMath);

/** Formule dans le texte : `$x^2$`. */
export const mathInlineSchema = $nodeSchema('math_inline', () => ({
  group: 'inline',
  inline: true,
  content: 'text*',
  marks: '',
  code: true,
  parseDOM: [{ tag: 'code[data-math-inline]', preserveWhitespace: 'full' }],
  toDOM: () => ['code', { 'data-math-inline': '', class: 'editor-math-inline' }, 0],
  parseMarkdown: {
    match: ({ type }) => type === 'inlineMath',
    runner: (state, node, type) => {
      state.openNode(type);
      const value = String(node['value'] ?? '');
      if (value) {
        state.addText(value);
      }
      state.closeNode();
    },
  },
  toMarkdown: {
    match: (node) => node.type.name === 'math_inline',
    runner: (state, node) => {
      state.addNode('inlineMath', undefined, node.textContent);
    },
  },
}));

/** Formule centrée : `$$` seul sur sa ligne, la formule, `$$`. */
export const mathBlockSchema = $nodeSchema('math_block', () => ({
  group: 'block',
  content: 'text*',
  marks: '',
  code: true,
  defining: true,
  parseDOM: [{ tag: 'pre[data-math-block]', preserveWhitespace: 'full', priority: 60 }],
  toDOM: () => ['pre', { 'data-math-block': '', class: 'editor-math-block' }, ['code', 0]],
  parseMarkdown: {
    match: ({ type }) => type === 'math',
    runner: (state, node, type) => {
      state.openNode(type);
      const value = String(node['value'] ?? '');
      if (value) {
        state.addText(value);
      }
      state.closeNode();
    },
  },
  toMarkdown: {
    match: (node) => node.type.name === 'math_block',
    runner: (state, node) => {
      state.addNode('math', undefined, node.textContent);
    },
  },
}));

/** Greffons des formules, dans l'ordre d'enregistrement. */
export const math = [remarkMathPlugin, mathInlineSchema, mathBlockSchema].flat();
