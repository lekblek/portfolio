/**
 * Diagrammes Mermaid d'un contenu rendu, dans le navigateur seulement : chaque
 * `figure[data-diagram]` (source affichée par le rendu serveur) est dessinée quand elle approche
 * de la fenêtre ; Mermaid n'est chargé qu'à ce moment, une fois par page. Niveau de sécurité
 * `strict` (étiquettes échappées, aucun clic ni script ; non modifiable par une directive du
 * diagramme). Couleurs et police lues dans les tokens. Le source reste disponible dans un
 * `<details>` ; un diagramme invalide garde son source et l'indique.
 */
type Mermaid = typeof import('mermaid').default;

let mermaidLoading: Promise<Mermaid> | null = null;
let diagramCount = 0;

/** Observe les diagrammes non encore dessinés de `container` ; retourne de quoi arrêter. */
export function observeDiagrams(container: HTMLElement): () => void {
  const figures = Array.from(
    container.querySelectorAll<HTMLElement>('figure[data-diagram]:not([data-diagram-state])'),
  );
  if (figures.length === 0 || typeof IntersectionObserver === 'undefined') {
    return () => undefined;
  }
  const observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          observer.unobserve(entry.target);
          void draw(entry.target as HTMLElement);
        }
      }
    },
    { rootMargin: '200px 0px' },
  );
  figures.forEach((figure) => {
    figure.dataset['diagramState'] = 'waiting';
    observer.observe(figure);
  });
  return () => observer.disconnect();
}

async function draw(figure: HTMLElement): Promise<void> {
  const source = figure.querySelector('pre.diagram-source');
  const definition = source?.textContent ?? '';
  try {
    const mermaid = await loadMermaid();
    const { svg } = await mermaid.render(`diagramme-${++diagramCount}`, definition);
    const drawing = figure.ownerDocument.createElement('div');
    drawing.className = 'diagram-drawing';
    // SVG produit par Mermaid en niveau `strict` (étiquettes assainies par Mermaid)
    drawing.innerHTML = svg;
    const details = figure.ownerDocument.createElement('details');
    details.className = 'diagram-details';
    details.innerHTML = '<summary>Source du diagramme</summary>';
    if (source) {
      details.appendChild(source);
    }
    figure.replaceChildren(drawing, details);
    figure.dataset['diagramState'] = 'drawn';
  } catch {
    const note = figure.ownerDocument.createElement('figcaption');
    note.className = 'diagram-error';
    note.textContent = 'Le diagramme n’a pas pu être dessiné ; sa source est affichée.';
    figure.appendChild(note);
    figure.dataset['diagramState'] = 'failed';
  }
}

function loadMermaid(): Promise<Mermaid> {
  mermaidLoading ??= import('mermaid').then(({ default: mermaid }) => {
    const tokens = getComputedStyle(document.documentElement);
    const token = (name: string) => tokens.getPropertyValue(name).trim();
    mermaid.initialize({
      startOnLoad: false,
      securityLevel: 'strict',
      theme: 'base',
      fontFamily: token('--font-display'),
      themeVariables: {
        background: token('--color-paper'),
        primaryColor: token('--color-paper-sunken'),
        primaryTextColor: token('--color-ink'),
        primaryBorderColor: token('--color-ink'),
        secondaryColor: token('--color-paper'),
        tertiaryColor: token('--color-paper'),
        lineColor: token('--color-ink-muted'),
        textColor: token('--color-ink'),
        fontFamily: token('--font-display'),
      },
    });
    return mermaid;
  });
  return mermaidLoading;
}
