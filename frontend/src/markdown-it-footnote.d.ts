// markdown-it-footnote 4.0.0 n'a pas de types ; @types/markdown-it-footnote suit @types/markdown-it,
// pas les types fournis par markdown-it 15 (D-DY). Seule la fonction de greffon est utilisée.
declare module 'markdown-it-footnote' {
  import type { PluginSimple } from 'markdown-it';

  const footnote: PluginSimple;
  export default footnote;
}
