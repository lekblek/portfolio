/*
 * Préparation des tests unitaires (jsdom). jsdom n'implémente ni le dialogue modal
 * (`showModal`, `close`) ni `ResizeObserver` : versions minimales, suffisantes pour vérifier la
 * logique des composants. Le comportement réel (couche supérieure, focus contenu, `Échap`) est
 * vérifié dans un vrai navigateur par les tests de bout en bout.
 */

if (typeof HTMLDialogElement.prototype.showModal !== 'function') {
  HTMLDialogElement.prototype.showModal = function (this: HTMLDialogElement) {
    this.open = true;
  };
  HTMLDialogElement.prototype.close = function (this: HTMLDialogElement, returnValue?: string) {
    if (!this.open) {
      return;
    }
    if (returnValue !== undefined) {
      this.returnValue = returnValue;
    }
    this.open = false;
    this.dispatchEvent(new Event('close'));
  };
}

if (typeof globalThis.ResizeObserver === 'undefined') {
  globalThis.ResizeObserver = class {
    observe(): void {
      // Aucune mesure de mise en page dans jsdom
    }
    unobserve(): void {
      // idem
    }
    disconnect(): void {
      // idem
    }
  };
}
