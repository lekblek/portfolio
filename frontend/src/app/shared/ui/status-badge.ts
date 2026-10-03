import { Component, computed, input } from '@angular/core';

/** États d'un contenu : visibilité d'un projet, statut d'une publication (D-AV). */
export type ContentStatus = 'DRAFT' | 'IN_REVIEW' | 'SCHEDULED' | 'PUBLISHED' | 'ARCHIVED';

const LABELS: Record<ContentStatus, [masculine: string, feminine: string]> = {
  DRAFT: ['Brouillon', 'Brouillon'],
  IN_REVIEW: ['En relecture', 'En relecture'],
  SCHEDULED: ['Programmé', 'Programmée'],
  PUBLISHED: ['Publié', 'Publiée'],
  ARCHIVED: ['Archivé', 'Archivée'],
};

/**
 * Pastille de statut (02-design-system §18) : le mot d'abord, la couleur ensuite (jamais seule) ;
 * repère de forme pour distinguer les états sans couleur : cercle plein (publié), cercle vide
 * (brouillon), demi-cercle (relecture, programmation), carré (archivé). Accord selon le contenu :
 * « Publié » pour un projet, « Publiée » pour une publication.
 */
@Component({
  selector: 'app-status-badge',
  host: { class: 'status-badge', '[attr.data-status]': 'status()' },
  template: `<span class="status-badge-mark" aria-hidden="true"></span>{{ label() }}`,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      gap: calc(var(--spacing) * 1.5);
      padding: calc(var(--spacing) * 0.5) calc(var(--spacing) * 2);
      border: var(--border-rule) solid currentColor;
      border-radius: var(--radius-full);
      color: var(--color-ink-muted);
      font-size: var(--text-sm);
      font-weight: var(--font-weight-medium);
      line-height: var(--leading-ui);
      white-space: nowrap;
    }

    .status-badge-mark {
      width: calc(var(--spacing) * 2);
      height: calc(var(--spacing) * 2);
      border: var(--border-strong) solid currentColor;
      border-radius: var(--radius-full);
    }

    :host([data-status='PUBLISHED']) {
      color: var(--color-success);
    }

    :host([data-status='PUBLISHED']) .status-badge-mark {
      background: currentColor;
    }

    :host([data-status='IN_REVIEW']) {
      color: var(--color-accent);
    }

    :host([data-status='SCHEDULED']) {
      color: var(--color-warning);
    }

    :host([data-status='IN_REVIEW']) .status-badge-mark,
    :host([data-status='SCHEDULED']) .status-badge-mark {
      background: linear-gradient(90deg, currentColor 50%, transparent 50%);
    }

    :host([data-status='ARCHIVED']) {
      background: var(--color-paper-sunken);
    }

    :host([data-status='ARCHIVED']) .status-badge-mark {
      border-radius: 0;
      background: currentColor;
    }
  `,
})
export class StatusBadge {
  readonly status = input.required<ContentStatus>();
  /** Accord au féminin (une publication) ; masculin par défaut (un projet). */
  readonly feminine = input(false);

  protected readonly label = computed(() => LABELS[this.status()][this.feminine() ? 1 : 0]);
}
