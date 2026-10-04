// Frontières entre les dossiers de src/app (docs/frontend/01-architecture.md §5).
// no-restricted-imports ne compare que le texte d'un import : « ../../b/x », écrit depuis
// features/a, ne nomme pas le dossier visé. Cette règle résout donc le chemin avant de juger.
const path = require('node:path');

const APP_ROOT = path.join(__dirname, '..', 'src', 'app');

/** Sous-dossiers de features/admin que les autres écrans d'administration peuvent importer. */
const ADMIN_SHARED = ['auth', 'editor'];

/** @type {Record<string, string>} */
const AREA_LABELS = {
  feature: 'une fonctionnalité',
  admin: 'l’administration',
};

/**
 * Zone d'un fichier de src/app, ou null hors de src/app.
 * @param {string} file chemin absolu
 * @returns {{ area: string, feature?: string } | null}
 */
function zoneOf(file) {
  const relative = path.relative(APP_ROOT, file);
  if (relative.startsWith('..') || path.isAbsolute(relative)) {
    return null;
  }
  const [top, second, third] = relative.split(path.sep);
  if (top === 'features') {
    if (second === 'admin') {
      // features/admin/admin.routes.ts assemble les sous-fonctionnalités : zone « admin » sans nom
      return third?.includes('.') ? { area: 'admin' } : { area: 'admin', feature: third };
    }
    return { area: 'feature', feature: second };
  }
  if (['core', 'shared', 'layout', 'dev'].includes(top)) {
    return { area: top };
  }
  return { area: 'root' };
}

/**
 * Motif de refus, ou null si l'import est permis.
 * @param {{ area: string, feature?: string }} from
 * @param {{ area: string, feature?: string }} to
 * @returns {string | null}
 */
function violation(from, to) {
  if (to.area === 'root') {
    return from.area === 'root' ? null : 'la racine de l’application';
  }
  switch (from.area) {
    case 'core':
      return to.area === 'core' ? null : to.area;
    case 'shared':
      return ['core', 'shared'].includes(to.area) ? null : to.area;
    case 'layout':
    case 'dev':
      return ['core', 'shared', from.area].includes(to.area) ? null : to.area;
    case 'feature':
      if (['core', 'shared', 'layout'].includes(to.area)) return null;
      return to.area === 'feature' && to.feature === from.feature
        ? null
        : 'une autre fonctionnalité';
    case 'admin':
      if (['core', 'shared', 'layout'].includes(to.area)) return null;
      if (to.area === 'feature') return 'le site public';
      if (to.area !== 'admin') return to.area;
      // auth (session) et editor (éditeur de contenu, F31) servent toute l'administration
      if (
        from.feature === undefined ||
        to.feature === from.feature ||
        ADMIN_SHARED.includes(to.feature ?? '')
      ) {
        return null;
      }
      return 'une autre fonctionnalité d’administration';
    default:
      return null;
  }
}

/** @type {import('eslint').Rule.RuleModule} */
const rule = {
  meta: {
    type: 'problem',
    docs: { description: 'Frontières entre core, shared, layout et features (01-architecture §5)' },
    schema: [],
    messages: {
      forbidden: '{{from}} ne peut pas importer {{target}} (docs/frontend/01-architecture.md §5).',
    },
  },
  create(context) {
    const from = zoneOf(context.filename);
    if (from === null) {
      return {};
    }
    /** @param {import('estree').Node & { source?: import('estree').Literal | null }} node */
    const check = (node) => {
      const specifier = node.source?.value;
      if (typeof specifier !== 'string' || !specifier.startsWith('.')) {
        return;
      }
      const to = zoneOf(path.resolve(path.dirname(context.filename), specifier));
      const refused = to && violation(from, to);
      if (refused) {
        const target = AREA_LABELS[refused] ?? refused;
        const label = from.feature ? `${from.area}/${from.feature}` : from.area;
        context.report({ node, messageId: 'forbidden', data: { from: label, target } });
      }
    };
    return {
      ImportDeclaration: check,
      ExportNamedDeclaration: check,
      ExportAllDeclaration: check,
      // import('…') des routes chargées à la demande
      ImportExpression: (node) => check(/** @type {any} */ (node)),
    };
  },
};

module.exports = { rules: { 'folder-boundaries': rule } };
