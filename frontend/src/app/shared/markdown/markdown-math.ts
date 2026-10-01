import {
  DOCUMENT,
  inject,
  InjectionToken,
  makeStateKey,
  Service,
  signal,
  TransferState,
} from '@angular/core';

import { MathRenderer } from './markdown-renderer';

/** Rendu KaTeX fourni par la seule configuration serveur : KaTeX n'entre pas dans les lots du navigateur. */
export const SERVER_MATH_RENDERER = new InjectionToken<MathRenderer>('SERVER_MATH_RENDERER');

/** Feuille de style et polices de KaTeX, copiées telles quelles depuis le paquet (angular.json). */
const KATEX_STYLESHEET = '/katex/katex.min.css';
const KATEX_STYLESHEET_ID = 'katex-stylesheet';

/**
 * Formules du Markdown sans KaTeX dans le navigateur au premier affichage :
 * - au rendu serveur, KaTeX produit chaque formule et la confie à `TransferState` ;
 * - dans le navigateur, une formule transmise est reprise telle quelle (même HTML à
 *   l'hydratation) ; une formule inconnue (navigation dans le navigateur vers un autre contenu)
 *   déclenche le chargement paresseux de KaTeX, puis un nouveau rendu (signal `loaded`).
 * La feuille de style de KaTeX n'est ajoutée qu'aux pages qui contiennent des formules.
 */
@Service()
export class MarkdownMath {
  private readonly server = inject(SERVER_MATH_RENDERER, { optional: true });
  private readonly transferState = inject(TransferState);
  private readonly document = inject(DOCUMENT);
  private readonly loaded = signal<MathRenderer | null>(null);
  private loading = false;

  /** Rendu à passer à `renderMarkdown` ; à lire dans un contexte réactif (dépend de `loaded`). */
  renderer(): MathRenderer {
    const server = this.server;
    if (server) {
      return (tex, display) => {
        const html = server(tex, display);
        this.transferState.set(mathStateKey(tex, display), html);
        return html;
      };
    }
    const loaded = this.loaded();
    return (tex, display) => {
      const transferred = this.transferState.get(mathStateKey(tex, display), null);
      if (transferred !== null) {
        return transferred;
      }
      if (loaded) {
        return loaded(tex, display);
      }
      this.load();
      return pending(tex);
    };
  }

  /** Ajoute la feuille de style de KaTeX à la page, une fois. */
  ensureStylesheet(): void {
    if (this.document.getElementById(KATEX_STYLESHEET_ID)) {
      return;
    }
    const link = this.document.createElement('link');
    link.id = KATEX_STYLESHEET_ID;
    link.rel = 'stylesheet';
    link.href = KATEX_STYLESHEET;
    this.document.head.appendChild(link);
  }

  private load(): void {
    if (this.loading) {
      return;
    }
    this.loading = true;
    void import('./katex-math').then(({ renderMath }) => this.loaded.set(renderMath));
  }
}

/** Clé de transfert d'une formule (rendu serveur → navigateur). */
export function mathStateKey(tex: string, display: boolean) {
  return makeStateKey<string>(`math:${display ? 'd' : 'i'}:${hash(tex)}`);
}

/** Empreinte courte et stable (FNV-1a 32 bits) : clé de transfert d'une formule. */
function hash(text: string): string {
  let value = 0x811c9dc5;
  for (let index = 0; index < text.length; index++) {
    value ^= text.charCodeAt(index);
    value = Math.imul(value, 0x01000193);
  }
  return (value >>> 0).toString(36) + text.length.toString(36);
}

/** Source de la formule, en attendant KaTeX (dans le cadre de bloc pour une formule centrée). */
function pending(tex: string): string {
  const source = tex.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  return `<code class="math-pending">${source}</code>`;
}
