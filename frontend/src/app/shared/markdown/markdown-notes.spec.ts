import { renderMarkdown } from './markdown-renderer';

const render = (source: string, codeToolbar = false) =>
  renderMarkdown(source, { path: '/articles/exemple', codeToolbar }).html;

describe('renderMarkdown notes', () => {
  const source = [
    'Premier paragraphe avec une note[^cache].',
    '',
    '- élément de liste[^liste]',
    '- autre élément',
    '',
    'Dernier paragraphe, qui rappelle la première note[^cache].',
    '',
    '[^cache]: Le cache est **vidé** à chaque déploiement.',
    '[^liste]: Note appelée depuis une liste.',
  ].join('\n');

  it('places each note right after the block that first calls it, not at the end', () => {
    const html = render(source);

    const firstParagraph = html.indexOf('Premier paragraphe');
    const firstNote = html.indexOf('id="note-1"');
    const list = html.indexOf('<ul>');
    const secondNote = html.indexOf('id="note-2"');
    const lastParagraph = html.indexOf('Dernier paragraphe');
    expect(firstParagraph).toBeLessThan(firstNote);
    expect(firstNote).toBeLessThan(list);
    expect(html.indexOf('</ul>')).toBeLessThan(secondNote);
    expect(secondNote).toBeLessThan(lastParagraph);
    expect(html).not.toContain('footnotes');
    expect(html.match(/role="note"/g)).toHaveLength(2);
  });

  it('links calls and notes both ways, on the current page', () => {
    const html = render(source);

    expect(html).toContain(
      '<sup class="note-ref"><a href="/articles/exemple#note-1" id="appel-note-1"><span class="sr-only">Note </span>1</a></sup>',
    );
    expect(html).toContain('id="appel-note-1-2"');
    expect(html).toContain('href="/articles/exemple#appel-note-1"');
    expect(html).toContain('Retour à l’appel de la note 1');
    expect(html).toContain('Le cache est <strong>vidé</strong>');
  });

  it('keeps the note text as safe as the rest of the content', () => {
    const html = render(
      'Texte[^x].\n\n[^x]: <script>alert(1)</script> [clic](javascript:alert(1))',
    );

    expect(html).not.toContain('<script>');
    expect(html).not.toContain('href="javascript:');
  });
});

describe('renderMarkdown code toolbar', () => {
  it('adds the language and a copy button when asked', () => {
    const html = render('```java\nclass A {}\n```', true);

    expect(html).toContain('<span class="code-language" translate="no">java</span>');
    expect(html).toContain('<button type="button" class="code-copy" data-code-copy>');
    expect(html).toContain('<pre class="code" tabindex="0">');
  });

  it('names no language it does not know, and adds nothing by default', () => {
    expect(render('```inconnu\nx\n```', true)).not.toContain('code-language');
    expect(render('```java\nclass A {}\n```')).not.toContain('code-copy');
  });
});
