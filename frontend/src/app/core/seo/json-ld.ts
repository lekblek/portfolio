/**
 * Données structurées JSON-LD sérialisées pour un `<script type="application/ld+json">`.
 * `<`, `>` et `&` sont échappés : un texte saisi (titre d'article contenant `</script>`) ne peut
 * pas fermer la balise ni injecter du HTML. U+2028 et U+2029 le sont aussi (fins de ligne JS).
 */
export function serializeJsonLd(data: object): string {
  return JSON.stringify(data)
    .replace(/</g, '\\u003c')
    .replace(/>/g, '\\u003e')
    .replace(/&/g, '\\u0026')
    .replace(/\u2028/g, '\\u2028')
    .replace(/\u2029/g, '\\u2029');
}
