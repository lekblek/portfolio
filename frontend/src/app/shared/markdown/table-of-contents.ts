import { Component, computed, input } from '@angular/core';

import { TocEntry } from './toc';

interface TocNode {
  entry: TocEntry;
  children: TocEntry[];
}

/**
 * Sommaire d'un contenu Markdown : titres de premier et de second niveau (`<h2>`, `<h3>` après
 * décalage), en liens vers leurs ancres sur la page courante. Liste imbriquée dans un
 * `<nav aria-label>`.
 */
@Component({
  selector: 'app-table-of-contents',
  template: `
    <nav [attr.aria-label]="label()">
      <ol class="toc">
        @for (node of tree(); track node.entry.id) {
          <li>
            <a class="toc-link" [href]="path() + '#' + node.entry.id">{{ node.entry.text }}</a>
            @if (node.children.length > 0) {
              <ol class="toc-sub">
                @for (child of node.children; track child.id) {
                  <li>
                    <a class="toc-link" [href]="path() + '#' + child.id">{{ child.text }}</a>
                  </li>
                }
              </ol>
            }
          </li>
        }
      </ol>
    </nav>
  `,
  styles: `
    .toc {
      font-size: var(--text-sm);
      line-height: var(--leading-snug);
    }

    .toc > li + li {
      margin-block-start: calc(var(--spacing) * 1);
    }

    .toc-sub {
      padding-inline-start: calc(var(--spacing) * 3);
      border-inline-start: var(--border-rule) solid var(--color-rule);
      margin-block: calc(var(--spacing) * 1);
    }

    .toc-link {
      display: block;
      padding-block: calc(var(--spacing) * 1);
      color: var(--color-ink);
      text-decoration-line: none;
    }

    .toc-sub .toc-link {
      color: var(--color-ink-muted);
    }

    .toc-link:hover,
    .toc-link:focus-visible {
      color: var(--color-accent-strong);
      text-decoration-line: underline;
    }
  `,
})
export class TableOfContents {
  readonly entries = input.required<readonly TocEntry[]>();
  /** Chemin de la page : les ancres y sont rattachées (`<base href="/">`). */
  readonly path = input.required<string>();
  readonly label = input('Sommaire');

  /** Titres de niveau 2 et leurs sous-titres de niveau 3 ; les niveaux plus bas sont omis. */
  protected readonly tree = computed(() => {
    const top = Math.min(...this.entries().map((entry) => entry.level));
    const nodes: TocNode[] = [];
    for (const entry of this.entries()) {
      if (entry.level === top) {
        nodes.push({ entry, children: [] });
      } else if (entry.level === top + 1 && nodes.length > 0) {
        nodes[nodes.length - 1].children.push(entry);
      }
    }
    return nodes;
  });
}
