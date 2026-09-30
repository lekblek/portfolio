// Refuse ce qui contourne le système de design (docs/frontend/02-design-system.md §17) :
// valeurs arbitraires de Tailwind, couleurs littérales et `!important` hors de `src/styles/tokens.css`.
// `!important` reste permis dans un bloc `prefers-reduced-motion`. Usage : npm run lint:styles
import { readdirSync, readFileSync } from 'node:fs';
import { join, relative, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('..', import.meta.url));
const source = join(root, 'src');
// tokens.css définit les valeurs ; index.html porte la couleur de l’interface du navigateur (theme-color = paper)
const allowed = new Set([join('src', 'styles', 'tokens.css'), join('src', 'index.html')]);

const rules = [
  { name: 'valeur arbitraire de Tailwind', pattern: /[\w)\]]-\[[^\]\s]+\]/g },
  {
    name: 'couleur littérale',
    pattern: /(?<![&\w])#(?:[0-9a-f]{8}|[0-9a-f]{6}|[0-9a-f]{3,4})\b/gi,
  },
  { name: 'couleur littérale', pattern: /\b(?:rgba?|hsla?|oklch|oklab|lab|lch|color)\(/gi },
  { name: '!important', pattern: /!important/g },
];

function* files(directory) {
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) {
      yield* files(path);
    } else if (/\.(html|ts|css)$/.test(entry.name) && !/\.(spec|d)\.ts$/.test(entry.name)) {
      yield path;
    }
  }
}

/** Remplace le contenu des blocs `@media (prefers-reduced-motion…)` par des espaces (numéros de ligne gardés). */
function withoutReducedMotion(css) {
  let result = css;
  for (const match of css.matchAll(/@media[^{]*prefers-reduced-motion[^{]*\{/g)) {
    let depth = 1;
    let index = match.index + match[0].length;
    while (depth > 0 && index < css.length) {
      depth += css[index] === '{' ? 1 : css[index] === '}' ? -1 : 0;
      index++;
    }
    const blank = css.slice(match.index, index).replace(/[^\n]/g, ' ');
    result = result.slice(0, match.index) + blank + result.slice(index);
  }
  return result;
}

const problems = [];
for (const file of files(source)) {
  const path = relative(root, file);
  if (allowed.has(path)) {
    continue;
  }
  const raw = readFileSync(file, 'utf8');
  const text = file.endsWith('.css') ? withoutReducedMotion(raw) : raw;
  text.split('\n').forEach((line, index) => {
    for (const rule of rules) {
      for (const match of line.matchAll(rule.pattern)) {
        problems.push(`${path.split(sep).join('/')}:${index + 1} ${rule.name} : ${match[0]}`);
      }
    }
  });
}

if (problems.length > 0) {
  console.error(problems.join('\n'));
  console.error(
    `\n${problems.length} écart(s) au système de design : ajouter un token plutôt qu’une valeur.`,
  );
  process.exit(1);
}
console.log('Styles conformes au système de design.');
