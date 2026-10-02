const HOME = '/admin';
const LOGIN = '/admin/login';
// Origine fictive : sert seulement à reconnaître une adresse qui sortirait du site
const PROBE = 'http://interne.invalid';

/**
 * Adresse de retour après la connexion, limitée aux pages de l'administration : toute autre valeur
 * (adresse externe, `//hôte`, chemin public, chemin qui remonte hors de `/admin`, page de connexion)
 * donne l'accueil de l'administration. Aucune redirection ouverte.
 */
export function adminReturnUrl(value: string | null | undefined): string {
  if (!value?.startsWith('/') || value.startsWith('//') || value.includes('\\')) {
    return HOME;
  }
  const url = new URL(value, PROBE);
  const inAdmin = url.pathname === HOME || url.pathname.startsWith(`${HOME}/`);
  if (url.origin !== PROBE || !inAdmin || url.pathname === LOGIN) {
    return HOME;
  }
  return `${url.pathname}${url.search}${url.hash}`;
}
