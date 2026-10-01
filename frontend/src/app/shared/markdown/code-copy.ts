import { Component, DestroyRef, DOCUMENT, inject, signal } from '@angular/core';

const RESET_DELAY_MS = 2000;

/**
 * Active les boutons « Copier » des blocs de code rendus avec `codeToolbar` (délégation d'un seul
 * écouteur sur le conteneur). Le texte du bloc part dans le presse-papiers ; le bouton affiche
 * « Copié » deux secondes et une région `aria-live` l'annonce. Sans presse-papiers (contexte non
 * sûr, refus), l'échec est annoncé. Rendu serveur : les boutons existent, inactifs avant
 * l'hydratation.
 */
@Component({
  selector: 'app-code-copy',
  host: { '(click)': 'copy($event)' },
  template: `
    <ng-content />
    <p class="sr-only" aria-live="polite">{{ announcement() }}</p>
  `,
})
export class CodeCopy {
  protected readonly announcement = signal('');

  private readonly document = inject(DOCUMENT);
  private timer: ReturnType<typeof setTimeout> | undefined;
  private lastButton: HTMLButtonElement | undefined;

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.timer));
  }

  protected async copy(event: Event): Promise<void> {
    const button = (event.target as Element | null)?.closest<HTMLButtonElement>(
      'button[data-code-copy]',
    );
    const code = button?.closest('.code-block')?.querySelector('pre code');
    if (!button || !code) {
      return;
    }
    const clipboard = this.document.defaultView?.navigator.clipboard;
    try {
      if (!clipboard) {
        throw new Error('presse-papiers indisponible');
      }
      // Sans la fin de ligne finale du bloc
      await clipboard.writeText((code.textContent ?? '').replace(/\n$/, ''));
      this.show(button, 'Copié', 'Code copié dans le presse-papiers.');
    } catch {
      this.show(button, 'Copier', 'La copie a échoué : sélectionnez le code pour le copier.');
    }
  }

  private show(button: HTMLButtonElement, label: string, announcement: string): void {
    clearTimeout(this.timer);
    if (this.lastButton && this.lastButton !== button) {
      setLabel(this.lastButton, 'Copier');
    }
    this.lastButton = button;
    setLabel(button, label);
    this.announcement.set(announcement);
    this.timer = setTimeout(() => {
      setLabel(button, 'Copier');
      this.announcement.set('');
    }, RESET_DELAY_MS);
  }
}

/** Libellé visible seulement : le nom accessible garde « le code » (texte masqué qui suit). */
function setLabel(button: HTMLButtonElement, label: string): void {
  if (button.firstChild) {
    button.firstChild.textContent = label;
  }
}
