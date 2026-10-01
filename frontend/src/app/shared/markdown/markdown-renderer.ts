import hljs from 'highlight.js/lib/core';
import bash from 'highlight.js/lib/languages/bash';
import css from 'highlight.js/lib/languages/css';
import java from 'highlight.js/lib/languages/java';
import javascript from 'highlight.js/lib/languages/javascript';
import json from 'highlight.js/lib/languages/json';
import python from 'highlight.js/lib/languages/python';
import sql from 'highlight.js/lib/languages/sql';
import typescript from 'highlight.js/lib/languages/typescript';
import xml from 'highlight.js/lib/languages/xml';
import yaml from 'highlight.js/lib/languages/yaml';
import MarkdownIt, { type Env, type Token } from 'markdown-it';
import footnote from 'markdown-it-footnote';

import { TocEntry, uniqueSlugger } from './toc';

/*
 * Rendu Markdown sûr par construction (docs/decisions/0003-rendu-markdown.md) :
 * HTML brut échappé (`html: false`), liens et images limités à http(s), mailto et adresses
 * relatives, balises et attributs produits par les seules règles de ce fichier.
 * Tout ajout (greffon, règle) doit préserver ces invariants et étendre le corpus des tests.
 */

for (const [name, language] of Object.entries({
  bash,
  css,
  java,
  javascript,
  json,
  python,
  sql,
  typescript,
  xml,
  yaml,
})) {
  hljs.registerLanguage(name, language);
}

export interface RenderOptions {
  /** Chemin de la page (`/articles/exemple`) : préfixe des liens internes `#…`. */
  path: string;
  /**
   * Niveau du plus haut titre du contenu : 2 (défaut) directement sous le `<h1>` de la page,
   * 3 quand le contenu est placé sous un titre de section `<h2>`.
   */
  topLevel?: 2 | 3;
  /**
   * Barre des blocs de code : langage et bouton « Copier » (`data-code-copy`), actif seulement
   * dans un conteneur `app-code-copy`.
   */
  codeToolbar?: boolean;
}

export interface RenderedMarkdown {
  html: string;
  /** Titres du contenu, après décalage sous le `<h1>` de la page. */
  headings: TocEntry[];
}

interface RenderEnv extends Env {
  path: string;
  topLevel: number;
  codeToolbar: boolean;
  headings: TocEntry[];
  externalLinks: boolean[];
}

const SCHEME = /^([a-z][a-z0-9+.-]*):/i;
const ALLOWED_SCHEMES = new Set(['http', 'https', 'mailto']);

/** Adresse acceptée : http(s), mailto, ou relative. Les caractères de contrôle ne masquent rien. */
export function isSafeUrl(url: string): boolean {
  // Les navigateurs ignorent tabulations et contrôles dans un protocole (« java\tscript: »)
  // eslint-disable-next-line no-control-regex
  const compact = url.replace(/[\x00-\x20]/g, '');
  const scheme = SCHEME.exec(compact)?.[1].toLowerCase();
  return scheme === undefined || ALLOWED_SCHEMES.has(scheme);
}

/** `env` de markdown-it : état propre à un rendu, créé par renderMarkdown. */
const renderEnv = (env: unknown) => env as RenderEnv;

const markdown = new MarkdownIt({ html: false, linkify: true, typographer: false });
markdown.validateLink = isSafeUrl;
// Notes (`texte[^1]` … `[^1]: note`) : syntaxe du greffon, rendu du projet plus bas (ADR 0003)
markdown.use(footnote);

const escapeHtml = markdown.utils.escapeHtml;

// Titres : le plus haut niveau du contenu devient h2 (le h1 est celui de la page), ou h3 sous une
// section ; identifiants uniques
markdown.core.ruler.push('project_headings', (state) => {
  const env = renderEnv(state.env);
  const openings = state.tokens.filter((token) => token.type === 'heading_open');
  if (openings.length === 0) {
    return;
  }
  const shift = env.topLevel - Math.min(...openings.map((token) => Number(token.tag.slice(1))));
  const slug = uniqueSlugger();
  state.tokens.forEach((token, index) => {
    if (token.type !== 'heading_open' && token.type !== 'heading_close') {
      return;
    }
    const level = Math.min(6, Number(token.tag.slice(1)) + shift);
    token.tag = `h${level}`;
    if (token.type === 'heading_open') {
      const inline = state.tokens[index + 1];
      const text = (inline.children ?? [])
        .filter((child) => child.type === 'text' || child.type === 'code_inline')
        .map((child) => child.content)
        .join('');
      const id = slug(text);
      token.attrSet('id', id);
      env.headings.push({ id, text, level });
    }
  });
});

// Listes de tâches (GFM) : « [ ] » / « [x] » en tête d'élément, état annoncé en texte
markdown.core.ruler.push('project_task_lists', (state) => {
  state.tokens.forEach((token, index) => {
    if (token.type !== 'inline' || state.tokens[index - 2]?.type !== 'list_item_open') {
      return;
    }
    const first = token.children?.[0];
    const match = first?.type === 'text' ? /^\[([ xX])\] /.exec(first.content) : null;
    if (!first || !match) {
      return;
    }
    const done = match[1] !== ' ';
    first.content = first.content.slice(match[0].length);
    const marker = new state.Token('task_marker', '', 0);
    marker.meta = { done };
    token.children?.unshift(marker);
    state.tokens[index - 2].attrJoin('class', 'task-list-item');
  });
});

markdown.renderer.rules['task_marker'] = (tokens, index) => {
  const { done } = tokens[index].meta as { done: boolean };
  return (
    `<span class="task-state" aria-hidden="true">${done ? '☑' : '☐'}</span>` +
    `<span class="sr-only">${done ? 'Fait' : 'À faire'}\u202f: </span>`
  );
};

// Code : coloration par classes (couleurs des tokens), bloc défilant atteignable au clavier ;
// barre facultative : langage reconnu et bouton « Copier »
function renderCode(content: string, info: string, toolbar: boolean): string {
  const language = info.trim().split(/\s+/)[0]?.toLowerCase() ?? '';
  const known = language !== '' && hljs.getLanguage(language) !== undefined;
  const body = known
    ? hljs.highlight(content, { language, ignoreIllegals: true }).value
    : escapeHtml(content);
  const codeClass = known ? ` class="hljs language-${language}"` : '';
  const pre = `<pre class="code" tabindex="0"><code${codeClass}>${body}</code></pre>`;
  if (!toolbar) {
    return `${pre}\n`;
  }
  const label = known ? `<span class="code-language" translate="no">${language}</span>` : '';
  return (
    `<div class="code-block"><div class="code-bar">${label}` +
    '<button type="button" class="code-copy" data-code-copy>' +
    'Copier<span class="sr-only"> le code</span></button></div>' +
    `${pre}</div>\n`
  );
}

markdown.renderer.rules['fence'] = (tokens, index, _options, env) =>
  renderCode(tokens[index].content, tokens[index].info, renderEnv(env).codeToolbar);
markdown.renderer.rules['code_block'] = (tokens, index, _options, env) =>
  renderCode(tokens[index].content, '', renderEnv(env).codeToolbar);

// Notes : chacune suit le bloc (paragraphe, liste…) qui l'appelle en premier, au lieu d'une liste
// en fin de document ; en marge sur grand écran, dans le flux en dessous (02-design-system §4.5).
// L'ordre du document (paragraphe, puis note) est l'ordre de lecture.
markdown.core.ruler.push('project_notes', (state) => {
  const tokens = state.tokens;
  const start = tokens.findIndex((token) => token.type === 'footnote_block_open');
  if (start < 0) {
    return;
  }
  const end = tokens.findIndex((token) => token.type === 'footnote_block_close');
  const notes = new Map<number, Token[]>();
  let current: Token[] = [];
  for (const token of tokens.slice(start + 1, end)) {
    if (token.type === 'footnote_open') {
      current = [];
      notes.set((token.meta as { id: number }).id, current);
    }
    current.push(token);
  }
  tokens.splice(start, end - start + 1);

  const placed = new Set<number>();
  const result: Token[] = [];
  let pending: number[] = [];
  for (const token of tokens) {
    result.push(token);
    for (const child of token.children ?? []) {
      const id = (child.meta as { id?: number } | null)?.id;
      if (child.type === 'footnote_ref' && id !== undefined && !placed.has(id)) {
        placed.add(id);
        pending.push(id);
      }
    }
    // Fin d'un bloc de premier niveau : ses notes le suivent
    if (token.level === 0 && token.nesting === -1 && pending.length > 0) {
      pending.forEach((id) => result.push(...(notes.get(id) ?? [])));
      pending = [];
    }
  }
  state.tokens = result;
});

const noteNumber = (token: Token) => (token.meta as { id: number }).id + 1;
const refSuffix = (token: Token) => {
  const subId = (token.meta as { subId?: number }).subId ?? 0;
  return subId > 0 ? `-${subId + 1}` : '';
};

markdown.renderer.rules['footnote_ref'] = (tokens, index, _options, env) => {
  const n = noteNumber(tokens[index]);
  return (
    `<sup class="note-ref"><a href="${renderEnv(env).path}#note-${n}" ` +
    `id="appel-note-${n}${refSuffix(tokens[index])}"><span class="sr-only">Note </span>${n}</a></sup>`
  );
};
markdown.renderer.rules['footnote_open'] = (tokens, index) => {
  const n = noteNumber(tokens[index]);
  return (
    `<div class="note" role="note" id="note-${n}">` +
    `<span class="note-number"><span class="sr-only">Note </span>${n}</span>`
  );
};
markdown.renderer.rules['footnote_close'] = () => '</div>\n';
markdown.renderer.rules['footnote_anchor'] = (tokens, index, _options, env) => {
  const n = noteNumber(tokens[index]);
  return (
    ` <a class="note-back" href="${renderEnv(env).path}#appel-note-${n}${refSuffix(tokens[index])}">` +
    `<span aria-hidden="true">\u21a9\ufe0e</span>` +
    `<span class="sr-only">Retour à l’appel de la note ${n}</span></a>`
  );
};

// Tableaux : défilement dans leur conteneur, jamais dans la page
markdown.renderer.rules['table_open'] = () => '<div class="table-scroll" tabindex="0"><table>\n';
markdown.renderer.rules['table_close'] = () => '</table></div>\n';

// Liens : internes (#…) préfixés par le chemin de la page, externes annoncés
markdown.renderer.rules['link_open'] = (tokens, index, options, env, self) => {
  const token = tokens[index];
  const href = String(token.attrGet('href') ?? '');
  if (href.startsWith('#')) {
    token.attrSet('href', `${renderEnv(env).path}${href}`);
  }
  const external = /^https?:/i.test(href);
  if (external) {
    token.attrJoin('class', 'external');
  }
  renderEnv(env).externalLinks.push(external);
  return self.renderToken(tokens, index, options);
};

markdown.renderer.rules['link_close'] = (tokens, index, options, env, self) => {
  const external = renderEnv(env).externalLinks.pop() ?? false;
  const hint = external ? '<span class="sr-only"> (site externe)</span>' : '';
  return hint + self.renderToken(tokens, index, options);
};

// Images : chargement différé ; dimensions inconnues en Markdown, contenues par la mise en page
const defaultImage = markdown.renderer.rules['image'];
markdown.renderer.rules['image'] = (tokens, index, options, env, self) => {
  tokens[index].attrSet('loading', 'lazy');
  tokens[index].attrSet('decoding', 'async');
  return defaultImage
    ? defaultImage(tokens, index, options, env, self)
    : self.renderToken(tokens, index, options);
};

/** Markdown → HTML sûr et titres du contenu. Synchrone : même résultat au serveur et au navigateur. */
export function renderMarkdown(source: string, options: RenderOptions): RenderedMarkdown {
  const env: RenderEnv = {
    path: options.path,
    topLevel: options.topLevel ?? 2,
    codeToolbar: options.codeToolbar ?? false,
    headings: [],
    externalLinks: [],
  };
  const html = markdown.render(source, env);
  return { html, headings: env.headings };
}
