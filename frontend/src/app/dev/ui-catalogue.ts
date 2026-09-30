import { afterNextRender, Component, computed, DOCUMENT, inject, signal } from '@angular/core';

import { Button, ButtonVariant } from '../shared/ui/button';
import { Icon } from '../shared/ui/icon';
import { contrastRatio, textLevel } from './contrast';

interface ColorToken {
  name: string;
  swatch: string;
  use: string;
}

interface ColorPair {
  foreground: string;
  background: string;
  textClass: string;
  backgroundClass: string;
}

/** Tokens de couleur, avec la classe qui les affiche (écrite en entier pour être repérée par Tailwind). */
const COLORS: ColorToken[] = [
  { name: 'paper', swatch: 'bg-paper', use: 'fond' },
  { name: 'paper-sunken', swatch: 'bg-paper-sunken', use: 'code, encadrés' },
  { name: 'ink', swatch: 'bg-ink', use: 'texte, titres, bordure de contrôle' },
  { name: 'ink-muted', swatch: 'bg-ink-muted', use: 'texte secondaire' },
  { name: 'rule', swatch: 'bg-rule', use: 'filets (décoratif)' },
  { name: 'accent', swatch: 'bg-accent', use: 'liens, focus, état courant' },
  { name: 'accent-strong', swatch: 'bg-accent-strong', use: 'survol d’un lien' },
  { name: 'selection', swatch: 'bg-selection', use: 'sélection de texte' },
  { name: 'danger', swatch: 'bg-danger', use: 'erreur' },
  { name: 'success', swatch: 'bg-success', use: 'confirmation' },
  { name: 'warning', swatch: 'bg-warning', use: 'avertissement' },
];

const TEXT_CLASSES: Record<string, string> = {
  ink: 'text-ink',
  'ink-muted': 'text-ink-muted',
  accent: 'text-accent',
  'accent-strong': 'text-accent-strong',
  danger: 'text-danger',
  success: 'text-success',
  warning: 'text-warning',
  'code-keyword': 'text-code-keyword',
  'code-string': 'text-code-string',
  'code-annotation': 'text-code-annotation',
};

const BACKGROUND_CLASSES: Record<string, string> = {
  paper: 'bg-paper',
  'paper-sunken': 'bg-paper-sunken',
  selection: 'bg-selection',
};

function pair(foreground: string, background: string): ColorPair {
  return {
    foreground,
    background,
    textClass: TEXT_CLASSES[foreground],
    backgroundClass: BACKGROUND_CLASSES[background],
  };
}

const PAIRS: ColorPair[] = [
  ...['ink', 'ink-muted', 'accent', 'accent-strong', 'danger', 'success', 'warning'].flatMap(
    (foreground) => [pair(foreground, 'paper'), pair(foreground, 'paper-sunken')],
  ),
  pair('ink', 'selection'),
  pair('code-keyword', 'paper-sunken'),
  pair('code-string', 'paper-sunken'),
  pair('code-annotation', 'paper-sunken'),
];

const TYPE_SCALE = [
  {
    token: 'text-4xl',
    classes: 'text-4xl leading-tight tracking-display',
    use: 'nom sur l’accueil',
  },
  {
    token: 'text-3xl',
    classes: 'text-3xl leading-tight tracking-title',
    use: 'titre de page et d’article',
  },
  {
    token: 'text-2xl',
    classes: 'text-2xl leading-snug tracking-heading',
    use: 'section d’un article',
  },
  { token: 'text-xl', classes: 'text-xl leading-snug tracking-heading', use: 'titre d’une entrée' },
  { token: 'text-lg', classes: 'text-lg leading-snug', use: 'titre de registre' },
  { token: 'text-base', classes: 'text-base', use: 'interface' },
  { token: 'text-sm', classes: 'text-sm', use: 'métadonnées, légendes' },
  { token: 'text-xs', classes: 'text-xs', use: 'notes, cartouche' },
];

const SPACING = [
  { token: '1', bar: 'w-1' },
  { token: '2', bar: 'w-2' },
  { token: '3', bar: 'w-3' },
  { token: '4', bar: 'w-4' },
  { token: '5', bar: 'w-5' },
  { token: '6', bar: 'w-6' },
  { token: '8', bar: 'w-8' },
  { token: '10', bar: 'w-10' },
  { token: '12', bar: 'w-12' },
  { token: '14', bar: 'w-14' },
  { token: '16', bar: 'w-16' },
  { token: '24', bar: 'w-24' },
  { token: 'flow', bar: 'w-flow' },
  { token: 'block', bar: 'w-block' },
  { token: 'section', bar: 'w-section' },
  { token: 'gutter', bar: 'w-gutter' },
];

const STACKS = [
  { name: 'stack-flow', use: 'éléments d’un bloc' },
  { name: 'stack-block', use: 'blocs d’une zone' },
  { name: 'stack-section', use: 'sections d’une page' },
];

const TERMS = [
  'Java',
  'Spring Boot',
  'Angular',
  'PostgreSQL',
  'Docker',
  'Testcontainers',
  'Tailwind CSS',
];

const MOTION = [
  { token: '--duration-instant', duration: 'var(--duration-instant)', ease: 'var(--ease-out)' },
  { token: '--duration-fast', duration: 'var(--duration-fast)', ease: 'var(--ease-out)' },
  { token: '--duration-base', duration: 'var(--duration-base)', ease: 'var(--ease-out)' },
  { token: '--duration-slow', duration: 'var(--duration-slow)', ease: 'var(--ease-in-out)' },
];

/**
 * Catalogue du système de design, en développement seulement (`/_ui`, `canMatch: isDevMode`).
 * Les contrastes sont calculés dans le navigateur à partir des tokens réellement appliqués.
 */
@Component({
  selector: 'app-ui-catalogue',
  imports: [Button, Icon],
  templateUrl: './ui-catalogue.html',
  styleUrl: './ui-catalogue.css',
})
export class UiCatalogue {
  protected readonly colors = COLORS;
  protected readonly typeScale = TYPE_SCALE;
  protected readonly spacing = SPACING;
  protected readonly buttonVariants: ButtonVariant[] = ['primary', 'secondary', 'quiet'];
  protected readonly stacks = STACKS;
  protected readonly terms = TERMS;
  protected readonly motion = MOTION;

  protected readonly values = signal<Record<string, string>>({});
  protected readonly moved = signal(false);

  protected readonly measuredPairs = computed(() =>
    PAIRS.map((item) => {
      const value = contrastRatio(
        this.values()[item.foreground] ?? '',
        this.values()[item.background] ?? '',
      );
      const measured = !Number.isNaN(value);
      return {
        ...item,
        ratio: measured ? `${value.toFixed(2).replace('.', ',')}:1` : '—',
        level: measured ? textLevel(value) : '',
      };
    }),
  );

  private readonly document = inject(DOCUMENT);

  constructor() {
    afterNextRender(() => {
      const styles = getComputedStyle(this.document.documentElement);
      const names = [
        ...COLORS.map((c) => c.name),
        'code-keyword',
        'code-string',
        'code-annotation',
      ];
      this.values.set(
        Object.fromEntries(
          names.map((name) => [name, styles.getPropertyValue(`--color-${name}`).trim()]),
        ),
      );
    });
  }
}
