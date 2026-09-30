import { Profile } from '../../../core/api/api-types';

/**
 * Données structurées de la page À propos : `ProfilePage` dont l'entité principale est la
 * `Person` du profil. Seules les données publiées y figurent ; jamais l'adresse électronique
 * (D-EA). `absolute` rend absolue une adresse du site.
 */
export function profilePageJsonLd(profile: Profile, absolute: (path: string) => string): object {
  const person: Record<string, unknown> = {
    '@type': 'Person',
    name: profile.displayName,
    jobTitle: profile.professionalTitle,
    description: profile.shortBio,
  };
  if (profile.avatar) {
    person['image'] = absolute(profile.avatar.url);
  }
  if (profile.publicLocation) {
    person['homeLocation'] = { '@type': 'Place', name: profile.publicLocation };
  }
  if (profile.links.length > 0) {
    person['sameAs'] = profile.links.map((link) => link.url);
  }
  return {
    '@context': 'https://schema.org',
    '@type': 'ProfilePage',
    url: absolute('/about'),
    inLanguage: 'fr',
    mainEntity: person,
  };
}
