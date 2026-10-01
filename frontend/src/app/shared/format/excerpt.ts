/**
 * Texte brut d'un Markdown court (description d'une série) : premier paragraphe, sans balisage ni
 * code, borné à `max` caractères sur une fin de mot. Pour un résumé de liste ou une description
 * de page, jamais pour afficher le contenu lui-même (rendu par `shared/markdown`).
 */
export function markdownExcerpt(markdown: string, max: number): string {
  const paragraph =
    markdown
      .replace(/```[\s\S]*?```/g, ' ')
      .split(/\n\s*\n/)
      .map((block) => block.trim())
      .find((block) => block !== '' && !block.startsWith('#') && !block.startsWith('$$')) ?? '';
  const text = paragraph
    .replace(/!\[[^\]]*]\([^)]*\)/g, '')
    .replace(/\[([^\]]*)]\([^)]*\)/g, '$1')
    .replace(/\[\^[^\]]*]/g, '')
    .replace(/[*_`>~]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
  if (text.length <= max) {
    return text;
  }
  const cut = text.slice(0, max - 1);
  return `${cut.slice(0, Math.max(cut.lastIndexOf(' '), 1)).trimEnd()}…`;
}
