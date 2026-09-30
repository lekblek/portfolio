import {
  booleanAttribute,
  computed,
  DestroyRef,
  Directive,
  ElementRef,
  inject,
  input,
  Renderer2,
} from '@angular/core';

export type ButtonVariant = 'primary' | 'secondary' | 'quiet';
export type ButtonSize = 'sm' | 'md';

// Retour de pression : seul mouvement retenu (transform, 100 ms), absent sous mouvement réduit (motion-safe)
const BASE =
  'inline-flex items-center justify-center gap-2 rounded-control border-(length:--border-strong) ' +
  'text-center font-medium leading-snug no-underline select-none touch-manipulation ' +
  'transition-transform duration-(--duration-instant) ease-out ' +
  'motion-safe:not-aria-disabled:not-aria-busy:enabled:active:scale-97 aria-busy:cursor-progress ' +
  'disabled:cursor-not-allowed aria-disabled:cursor-not-allowed';

// a[appButton] n'a pas d'état :enabled : le retour de pression passe par l'élément actif seul
const ANCHOR_PRESS = 'motion-safe:not-aria-disabled:not-aria-busy:active:scale-97';

const VARIANTS: Record<ButtonVariant, string> = {
  primary:
    'border-ink bg-ink text-paper hover:border-accent-strong hover:bg-accent-strong ' +
    'disabled:border-rule disabled:bg-paper-sunken disabled:text-ink-muted ' +
    'aria-disabled:border-rule aria-disabled:bg-paper-sunken aria-disabled:text-ink-muted',
  secondary:
    'border-ink bg-paper text-ink hover:bg-paper-sunken ' +
    'disabled:border-rule disabled:text-ink-muted aria-disabled:border-rule aria-disabled:text-ink-muted',
  quiet:
    'border-transparent bg-transparent text-accent hover:bg-paper-sunken hover:text-accent-strong ' +
    'disabled:text-ink-muted aria-disabled:text-ink-muted',
};

// md : cible de 44 px (tactile) ; sm : 32 px, au-dessus du minimum de 24 px (WCAG 2.5.8)
const SIZES: Record<ButtonSize, string> = {
  md: 'min-h-11 px-4 py-2 text-base',
  sm: 'min-h-8 px-3 py-1 text-sm',
};

/**
 * Bouton du système de design (docs/frontend/02-design-system.md §18), sur un `<button>` natif
 * ou un lien `<a>` qui en a l'apparence. Désactivation : `disabled` (retiré de la tabulation)
 * ou `aria-disabled="true"` (reste focalisable et annoncé). `loading` garde le libellé, pose
 * `aria-busy` et ignore les activations, ce qui empêche une double soumission.
 */
@Directive({
  selector: 'button[appButton], a[appButton]',
  host: {
    '[class]': 'classes()',
    '[attr.aria-busy]': 'loading() || null',
  },
})
export class Button {
  readonly variant = input<ButtonVariant>('primary');
  readonly size = input<ButtonSize>('md');
  readonly loading = input(false, { transform: booleanAttribute });

  private readonly element: HTMLElement = inject(ElementRef).nativeElement;
  private readonly isAnchor = this.element.tagName === 'A';

  protected readonly classes = computed(() =>
    [BASE, this.isAnchor ? ANCHOR_PRESS : '', VARIANTS[this.variant()], SIZES[this.size()]].join(
      ' ',
    ),
  );

  constructor() {
    // Phase de capture : passe avant les écouteurs (click) posés par le gabarit qui utilise le bouton
    const stop = inject(Renderer2).listen(
      this.element,
      'click',
      (event: Event) => this.blockWhenInactive(event),
      { capture: true },
    );
    inject(DestroyRef).onDestroy(stop);
  }

  private blockWhenInactive(event: Event): void {
    if (this.loading() || this.element.getAttribute('aria-disabled') === 'true') {
      event.preventDefault();
      event.stopImmediatePropagation();
    }
  }
}
