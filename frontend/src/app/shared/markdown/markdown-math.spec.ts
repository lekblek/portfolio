import { DOCUMENT, TransferState } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { renderMath } from './katex-math';
import { MarkdownMath, mathStateKey, SERVER_MATH_RENDERER } from './markdown-math';
import { MathRenderer, renderMarkdown } from './markdown-renderer';

const render = (source: string, math?: MathRenderer) =>
  renderMarkdown(source, { path: '/articles/exemple', math });

const fake: MathRenderer = (tex, display) => `<m d="${display}">${tex}</m>`;

describe('renderMarkdown math', () => {
  it('finds formulas in the text and on their own lines', () => {
    const result = render('Aire $A = \\pi r^2$.\n\n$$\nE = mc^2\n$$\n\nFin.', fake);

    expect(result.html).toContain('<m d="false">A = \\pi r^2</m>');
    expect(result.html).toContain(
      '<div class="math-display" tabindex="0"><m d="true">E = mc^2</m></div>',
    );
    expect(result.hasMath).toBe(true);
    expect(result.hasDiagrams).toBe(false);
  });

  it('accepts a centred formula written on one line', () => {
    expect(render('$$x^2$$', fake).html).toContain('<m d="true">x^2</m>');
  });

  it('leaves amounts, spaced dollars and escaped dollars as text', () => {
    const result = render('Prix : 5 $ et 6 $. Coût \\$4 et $ 3 $.', fake);

    expect(result.html).not.toContain('<m');
    expect(result.hasMath).toBe(false);
  });

  it('shows the source of a formula when no renderer is given, safely', () => {
    const html = render('Formule $a<b$.').html;

    expect(html).toContain('<code class="math-pending">a&lt;b</code>');
  });

  it('keeps formulas inside code as code', () => {
    expect(render('`$x$` et\n\n```\n$$y$$\n```', fake).html).not.toContain('<m');
  });
});

describe('renderMarkdown diagrams', () => {
  it('shows the source of a mermaid block, escaped, for the browser to draw', () => {
    const result = render('```mermaid\nflowchart LR\n  A[<b>] --> B\n```');

    expect(result.html).toContain('<figure class="diagram" data-diagram>');
    expect(result.html).toContain('A[&lt;b&gt;] --&gt; B');
    expect(result.html).not.toContain('<b>');
    expect(result.hasDiagrams).toBe(true);
  });
});

describe('renderMath (katex)', () => {
  it('writes MathML for assistive technologies', () => {
    const html = renderMath('x^2', false);

    expect(html).toContain('<math');
    expect(html).toContain('class="katex"');
  });

  it('renders an invalid formula without throwing', () => {
    expect(() => renderMath('\\frac{1}{', true)).not.toThrow();
    expect(renderMath('\\frac{1}{', true)).toContain('katex-error');
  });

  it('refuses links and attributes from the formula', () => {
    const html = renderMath('\\href{javascript:alert(1)}{x}', false);

    expect(html).not.toContain('href="javascript');
  });
});

describe('MarkdownMath', () => {
  afterEach(() => TestBed.inject(DOCUMENT).getElementById('katex-stylesheet')?.remove());

  it('renders with KaTeX on the server and hands each formula to the browser', () => {
    TestBed.configureTestingModule({
      providers: [{ provide: SERVER_MATH_RENDERER, useValue: fake }],
    });
    const math = TestBed.inject(MarkdownMath);

    expect(math.renderer()('x', false)).toBe('<m d="false">x</m>');
    expect(TestBed.inject(TransferState).get(mathStateKey('x', false), null)).toBe(
      '<m d="false">x</m>',
    );
  });

  it('reuses the formula rendered by the server, without KaTeX in the browser', () => {
    TestBed.inject(TransferState).set(mathStateKey('y', false), '<m>transmise</m>');
    const math = TestBed.inject(MarkdownMath);

    const rendered = renderMarkdown('$y$ et $z$', { path: '/', math: math.renderer() });

    expect(rendered.html).toContain('<m>transmise</m>');
    // Formule non transmise : source affichée en attendant le chargement de KaTeX
    expect(rendered.html).toContain('<code class="math-pending">z</code>');
  });

  it('adds the KaTeX stylesheet once', () => {
    const math = TestBed.inject(MarkdownMath);

    math.ensureStylesheet();
    math.ensureStylesheet();

    expect(
      TestBed.inject(DOCUMENT).head.querySelectorAll('link[href="/katex/katex.min.css"]'),
    ).toHaveLength(1);
  });
});
