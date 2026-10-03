/**
 * Appel de données à l'API publique, que le rendu serveur doit rendre inutile au navigateur. Les
 * fichiers des médias (`/api/public/media/…`) sont des images à télécharger, pas des appels.
 */
export function isDataCall(url: string): boolean {
  return url.includes('/api/public/') && !url.includes('/api/public/media/');
}
