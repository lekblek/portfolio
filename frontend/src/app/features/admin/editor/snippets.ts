/**
 * Fragments de Markdown insérés par la barre d'outils de l'éditeur (F31). Ils suivent le guide de
 * rédaction (02-design-system §20) : langages du moteur public, diagramme avec `accTitle` et
 * `accDescr`, figure = image seule avec un titre (D-EV).
 */

/** Langages colorés par le moteur public (ADR 0003), dans l'ordre du guide de rédaction. */
export const CODE_LANGUAGES = [
  'java',
  'typescript',
  'javascript',
  'python',
  'sql',
  'bash',
  'json',
  'yaml',
  'css',
  'xml',
] as const;

/** Bloc de code vide du langage choisi. */
export function codeBlock(language: string): string {
  return `\`\`\`${language}\n\n\`\`\``;
}

/** Formule centrée, à remplacer. */
export const MATH_TEMPLATE = '$$\nE = mc^2\n$$';

/** Diagramme Mermaid avec son titre et sa description accessibles. */
export const MERMAID_TEMPLATE = [
  '```mermaid',
  'flowchart LR',
  '  accTitle: Titre court du diagramme',
  '  accDescr: Ce que montre le diagramme, en une phrase.',
  '  A[Début] --> B[Fin]',
  '```',
].join('\n');

/** Tableau de deux colonnes et une ligne. */
export const TABLE_TEMPLATE = [
  '| Colonne | Colonne |',
  '| --- | --- |',
  '| Valeur | Valeur |',
].join('\n');

/**
 * Image de la médiathèque : `![texte alternatif](adresse)`, ou `![…](adresse "Légende")` pour une
 * figure légendée. Les crochets du texte alternatif et les guillemets de la légende sont échappés.
 */
export function imageMarkdown(url: string, altText: string, caption: string): string {
  const alt = altText.replace(/([[\]\\])/g, '\\$1');
  const title = caption ? ` "${caption.replace(/(["\\])/g, '\\$1')}"` : '';
  return `![${alt}](${url}${title})`;
}
