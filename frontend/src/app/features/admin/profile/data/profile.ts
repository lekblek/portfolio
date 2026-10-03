import { HttpClient, httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { AdminMedia, AdminProfile, SaveProfileRequest } from '../../../../core/api/api-types';
import { Page, toHttpParams } from '../../../../core/api/page';

const API = '/api/admin/profile';

/** Profil tel qu'il est saisi (D-CY) ; 404 tant qu'aucun n'est enregistré. Contexte d'injection. */
export function adminProfileResource() {
  return httpResource<AdminProfile>(() => API);
}

/** Crée ou remplace tout le profil, collections comprises, dans l'ordre des listes (D-CY). */
export function saveProfile(http: HttpClient, request: SaveProfileRequest): Promise<AdminProfile> {
  return firstValueFrom(http.put<AdminProfile>(API, request));
}

/** Un média choisi pour le profil (avatar, CV) : nom et aperçu. Contexte d'injection. */
export function chosenMediaResource(id: () => number | null) {
  return httpResource<AdminMedia>(() => {
    const current = id();
    return current === null ? undefined : `/api/admin/media/${current}`;
  });
}

/**
 * Page de la médiathèque montrée par le sélecteur (`page` en base 1), les plus récents d'abord ;
 * aucune requête tant que le sélecteur n'est pas ouvert (`page` nul). Contexte d'injection.
 */
export function pickerPageResource(page: () => number | null) {
  return httpResource<Page<AdminMedia>>(() => {
    const current = page();
    return current === null
      ? undefined
      : { url: '/api/admin/media', params: toHttpParams({ page: current }) };
  });
}
