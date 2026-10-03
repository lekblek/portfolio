import { httpResource } from '@angular/common/http';
import { computed } from '@angular/core';

import { AdminMedia } from './api-types';
import { Page, toHttpParams } from './page';

const API = '/api/admin/media';

/**
 * Un média choisi par un écran d'administration (avatar, CV, couverture, capture) : nom et aperçu.
 * Aucune requête sans identifiant. Contexte d'injection.
 *
 * L'identifiant passe par un `computed` : lu dans un modèle de formulaire, il ne relance la requête
 * que s'il change, pas à chaque frappe dans un autre champ (requêtes annulées puis relancées).
 */
export function adminMediaResource(id: () => number | null) {
  const mediaId = computed(id);
  return httpResource<AdminMedia>(() => {
    const current = mediaId();
    return current === null ? undefined : `${API}/${current}`;
  });
}

/**
 * Page de la médiathèque montrée par le sélecteur de médias (`page` en base 1), les plus récents
 * d'abord ; aucune requête tant que le sélecteur n'est pas ouvert (`page` nul). Contexte d'injection.
 */
export function mediaPickerPageResource(page: () => number | null) {
  return httpResource<Page<AdminMedia>>(() => {
    const current = page();
    return current === null ? undefined : { url: API, params: toHttpParams({ page: current }) };
  });
}
