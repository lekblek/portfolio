/** Rapport de contraste WCAG 2 entre deux couleurs `#rgb` ou `#rrggbb` ; `NaN` si l'une est illisible. */
export function contrastRatio(foreground: string, background: string): number {
  const a = relativeLuminance(foreground);
  const b = relativeLuminance(background);
  return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
}

/** Niveau atteint par un texte de taille courante : `AAA` (≥ 7), `AA` (≥ 4,5) ou `insuffisant`. */
export function textLevel(ratio: number): 'AAA' | 'AA' | 'insuffisant' {
  if (ratio >= 7) return 'AAA';
  if (ratio >= 4.5) return 'AA';
  return 'insuffisant';
}

function relativeLuminance(color: string): number {
  const channels = parseHex(color.trim());
  if (channels === null) {
    return Number.NaN;
  }
  const [r, g, b] = channels.map((channel) => {
    const value = channel / 255;
    return value <= 0.03928 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function parseHex(color: string): [number, number, number] | null {
  const match = /^#([0-9a-f]{3}|[0-9a-f]{6})$/i.exec(color);
  if (match === null) {
    return null;
  }
  const digits = match[1].length === 3 ? [...match[1]].map((d) => d + d).join('') : match[1];
  return [0, 2, 4].map((start) => parseInt(digits.slice(start, start + 2), 16)) as [
    number,
    number,
    number,
  ];
}
