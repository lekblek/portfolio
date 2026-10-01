import katex from 'katex';

import { MathRenderer } from './markdown-renderer';

/**
 * Formule TeX → HTML et MathML par KaTeX (sortie `htmlAndMathml` : MathML pour les technologies
 * d'assistance, HTML masqué pour l'affichage). Une formule invalide est rendue comme son source,
 * signalé, sans interrompre la page (`throwOnError: false`). `trust: false` refuse `\href`,
 * `\url` et les autres commandes qui produiraient des liens ou des attributs.
 *
 * Chargé au rendu serveur (`app.config.server.ts`) et, dans le navigateur, seulement après une
 * navigation vers un contenu qui contient des formules (`MarkdownMath`).
 */
export const renderMath: MathRenderer = (tex, display) =>
  katex.renderToString(tex, {
    displayMode: display,
    output: 'htmlAndMathml',
    throwOnError: false,
    trust: false,
    strict: 'ignore',
    // KaTeX écrit la couleur en ligne : le token du système de design (02-design-system §4.1)
    errorColor: 'var(--color-danger)',
  });
