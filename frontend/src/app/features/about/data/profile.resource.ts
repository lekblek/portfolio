import { httpResource } from '@angular/common/http';

import { Profile } from '../../../core/api/api-types';

/**
 * Profil public (`GET /api/public/profile`). Lu pendant le rendu serveur puis repris par le
 * navigateur depuis le cache de transfert : aucun second appel au premier affichage.
 * À appeler dans un contexte d'injection.
 */
export function profileResource() {
  return httpResource<Profile>(() => '/api/public/profile');
}
