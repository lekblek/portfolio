import { isSafeUrl, renderMarkdown } from './markdown-renderer';
import { slugify } from './toc';

const render = (source: string) => renderMarkdown(source, { path: '/articles/exemple' });

describe('renderMarkdown', () => {
  describe('safety', () => {
    // Corpus de l'ADR 0003 : aucune sortie ne doit ouvrir une balise dangereuse
    const attacks = {
      'balise script': '<script>alert(1)</script>',
      'attribut événement': '<img src=x onerror=alert(1)>',
      'bloc HTML': '<div onclick="alert(1)">clic</div>',
      'lien javascript:': '[clic](javascript:alert(1))',
      'lien masqué par casse et entité': '[clic](JaVa&#x09;ScRiPt:alert(1))',
      'lien vbscript:': '[clic](vbscript:msgbox(1))',
      'lien data:text/html': '[clic](data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==)',
      'lien automatique javascript:': '<javascript:alert(1)>',
      'image javascript:': '![x](javascript:alert(1))',
      'titre de lien piégé': '[clic](https://ok.example "a\\" onmouseover=\\"alert(1))',
      'lien de référence javascript:': '[clic][r]\n\n[r]: javascript:alert(1)',
    };
    const dangerous = /<script|<[a-z][^>]*\son\w+=|(href|src)="\s*(javascript|vbscript|data):/i;

    for (const [label, source] of Object.entries(attacks)) {
      it(`neutralises ${label}`, () => {
        expect(render(source).html).not.toMatch(dangerous);
      });
    }

    it('shows raw html as text', () => {
      expect(render('<script>alert(1)</script>').html).toContain(
        '&lt;script&gt;alert(1)&lt;/script&gt;',
      );
    });

    it('keeps the text of a refused link', () => {
      const html = render('[clic](javascript:alert(1))').html;

      expect(html).not.toContain('<a');
      expect(html).toContain('clic');
    });
  });

  describe('links', () => {
    it('accepts web, mail and relative addresses only', () => {
      expect(isSafeUrl('https://example.org')).toBe(true);
      expect(isSafeUrl('mailto:contact@example.org')).toBe(true);
      expect(isSafeUrl('/projects/portfolio')).toBe(true);
      expect(isSafeUrl('#section')).toBe(true);
      expect(isSafeUrl('javascript:alert(1)')).toBe(false);
      expect(isSafeUrl(' JaVaScRiPt:alert(1)')).toBe(false);
      expect(isSafeUrl('file:///etc/passwd')).toBe(false);
    });

    it('announces external links to assistive technologies', () => {
      const html = render('[Angular](https://angular.dev)').html;

      expect(html).toContain('class="external"');
      expect(html).toContain('<span class="sr-only"> (site externe)</span></a>');
    });

    it('leaves internal links unmarked', () => {
      const html = render('[projets](/projects)').html;

      expect(html).toBe('<p><a href="/projects">projets</a></p>\n');
    });

    it('prefixes anchors with the page path', () => {
      expect(render('[plus bas](#conclusion)').html).toContain(
        'href="/articles/exemple#conclusion"',
      );
    });
  });

  describe('headings', () => {
    it('places the highest heading of the content right under the page h1', () => {
      const { html, headings } = render('## Premier\n\n### Détail\n\n## Second');

      expect(html).toContain('<h2 id="premier">Premier</h2>');
      expect(html).toContain('<h3 id="detail">Détail</h3>');
      expect(headings.map((h) => h.level)).toEqual([2, 3, 2]);
    });

    it('turns a level 1 heading of the content into a level 2 heading', () => {
      expect(render('# Titre').html).toContain('<h2 id="titre">Titre</h2>');
    });

    it('gives stable ids without accents and keeps them unique', () => {
      const { headings } = render(
        '## Où vit le cœur ?\n\n## Où vit le cœur ?\n\n## `code` et texte',
      );

      expect(headings).toEqual([
        { id: 'ou-vit-le-coeur', text: 'Où vit le cœur ?', level: 2 },
        { id: 'ou-vit-le-coeur-2', text: 'Où vit le cœur ?', level: 2 },
        { id: 'code-et-texte', text: 'code et texte', level: 2 },
      ]);
    });
  });

  describe('code', () => {
    it('highlights a known language with token classes', () => {
      const html = render('```java\npublic record Page(int size) {}\n```').html;

      expect(html).toContain('<pre class="code" tabindex="0"><code class="hljs language-java">');
      expect(html).toContain('<span class="hljs-keyword">public</span>');
    });

    it('escapes code in an unknown language without highlighting it', () => {
      const html = render('```inconnu\n<b>gras</b>\n```').html;

      expect(html).toContain('<pre class="code" tabindex="0"><code>&lt;b&gt;gras&lt;/b&gt;');
      expect(html).not.toContain('hljs');
    });

    it('keeps a 200 column line on a single line', () => {
      const line = 'x'.repeat(200);

      expect(render(`\`\`\`\n${line}\n\`\`\``).html).toContain(`<code>${line}\n</code>`);
    });
  });

  describe('github flavoured markdown', () => {
    it('renders tables inside a scrolling container', () => {
      const html = render('| Code | Statut |\n|---|---|\n| 404 | introuvable |').html;

      expect(html).toContain('<div class="table-scroll" tabindex="0"><table>');
      expect(html).toContain('<th>Code</th>');
      expect(html).toContain('<td>introuvable</td>');
    });

    it('renders task lists with their state spoken as text', () => {
      const html = render('- [x] Écrire les tests\n- [ ] Publier').html;

      expect(html).toContain('<li class="task-list-item">');
      expect(html).toContain('<span class="sr-only">Fait');
      expect(html).toContain('<span class="sr-only">À faire');
      expect(html).not.toContain('[x]');
    });

    it('renders strikethrough and bare links', () => {
      const html = render('~~ancien~~ https://example.org').html;

      expect(html).toContain('<s>ancien</s>');
      expect(html).toContain('<a href="https://example.org" class="external">');
    });

    it('loads images lazily', () => {
      expect(render('![Schéma](/api/public/media/cle)').html).toContain(
        '<img src="/api/public/media/cle" alt="Schéma" loading="lazy" decoding="async">',
      );
    });
  });
});

describe('slugify', () => {
  it('falls back to a generic id when nothing remains', () => {
    expect(slugify('???')).toBe('section');
  });
});
