import { renderMarkdown } from '../../../../shared/markdown/markdown-renderer';
import angular from '../fixtures/article-angular.md';
import java from '../fixtures/article-java.md';
import vision from '../fixtures/article-vision.md';
import special from '../fixtures/caracteres-speciaux.md';
import lists from '../fixtures/listes-et-citations.md';
import normalisations from '../fixtures/normalisations.md';
import { codeBlock, MATH_TEMPLATE, MERMAID_TEMPLATE } from '../snippets';
import { createMarkdownEditor, MarkdownEditor } from './markdown-editor';

/**
 * Aller-retour Markdown → éditeur visuel → Markdown sur le corpus (ADR 0004) : le Markdown réécrit
 * peut différer (normalisations admises), mais le site doit en tirer exactement le même HTML ;
 * un second passage ne change plus rien.
 */
const CORPUS: Record<string, string> = {
  'article Java': java,
  'article Angular': angular,
  'article vision (image, formules, Mermaid)': vision,
  'listes et citations': lists,
  'caractères spéciaux': special,
  normalisations,
};

const html = (source: string) => renderMarkdown(source, { path: '/articles/essai' }).html;

let editors: MarkdownEditor[] = [];

async function roundTrip(source: string): Promise<string> {
  const root = document.createElement('div');
  document.body.append(root);
  const editor = await createMarkdownEditor(root, source, { label: 'Contenu' });
  editors.push(editor);
  return editor.markdown();
}

describe('visual editor round trip', () => {
  afterEach(async () => {
    for (const editor of editors) {
      await editor.destroy();
    }
    editors = [];
    document.body.innerHTML = '';
  });

  for (const [name, source] of Object.entries(CORPUS)) {
    it(`keeps the rendering of the ${name}`, async () => {
      const once = await roundTrip(source);

      expect(html(once)).toBe(html(source));
      expect(await roundTrip(once)).toBe(once);
    });
  }

  it('writes formulas back as they were typed', async () => {
    const once = await roundTrip('Aire $A = \\pi r^2$.\n\n$$\n\\frac{a_1}{b_2}\n$$\n');

    expect(once).toContain('$A = \\pi r^2$');
    expect(once).toContain('$$\n\\frac{a_1}{b_2}\n$$');
  });

  it('keeps the language of code blocks and the caption of a figure', async () => {
    const once = await roundTrip(
      '```mermaid\nflowchart LR\n  A --> B\n```\n\n![Alt](/api/public/media/a.webp "Légende")\n',
    );

    expect(once).toContain('```mermaid\nflowchart LR\n  A --> B\n```');
    expect(once).toContain('![Alt](/api/public/media/a.webp "Légende")');
  });

  it('leaves literal dollars alone, outside code', async () => {
    const source = 'Prix : 5 $ et 6 $, puis $x$ et `a $ b`.\n';
    const once = await roundTrip(source);

    expect(html(once)).toBe(html(source));
    expect(once).toContain('$x$');
    expect(once).toContain('`a $ b`');
  });

  it('inserts blocks one after another, after the block of the cursor', async () => {
    const root = document.createElement('div');
    document.body.append(root);
    const editor = await createMarkdownEditor(root, 'Un paragraphe.\n', { label: 'Contenu' });
    editors.push(editor);

    editor.insert(codeBlock('java'));
    editor.insert(MATH_TEMPLATE);
    editor.insert(MERMAID_TEMPLATE);
    const once = editor.markdown();

    expect(once).toContain('Un paragraphe.');
    expect(once).toContain('```java');
    expect(once).toContain('$$\nE = mc^2\n$$');
    expect(once.indexOf('```java')).toBeLessThan(once.indexOf('$$'));
    expect(once.indexOf('$$')).toBeLessThan(once.indexOf('```mermaid'));
  });

  it('applies the documented normalisations only', async () => {
    const once = await roundTrip(normalisations);

    expect(once).toContain('# Titre souligné');
    expect(once).toContain('## Sous-titre souligné');
    expect(once).toContain('- puce étoile');
    // L'emphase garde son marqueur d'origine
    expect(once).toContain('_italique_');
    expect(once).toContain('__gras__');
    expect(once).toContain('```\ncode indenté\nsur deux lignes\n```');
  });
});
