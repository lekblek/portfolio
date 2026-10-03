import { HttpClient, httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { AdminProfile, SaveProfileRequest } from '../../../../core/api/api-types';

const API = '/api/admin/profile';

/** Profil tel qu'il est saisi (D-CY) ; 404 tant qu'aucun n'est enregistré. Contexte d'injection. */
export function adminProfileResource() {
  return httpResource<AdminProfile>(() => API);
}

/** Crée ou remplace tout le profil, collections comprises, dans l'ordre des listes (D-CY). */
export function saveProfile(http: HttpClient, request: SaveProfileRequest): Promise<AdminProfile> {
  return firstValueFrom(http.put<AdminProfile>(API, request));
}
