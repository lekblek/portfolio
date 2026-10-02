import { httpResource } from '@angular/common/http';

import { Page } from '../../../../core/api/page';

/**
 * Compteur du tableau de bord : nombre total d'éléments d'une liste d'administration existante
 * (`totalElements`), lu sur une page d'un seul élément. Aucune route d'API dédiée.
 * À appeler dans un contexte d'injection.
 */
export function adminCountResource(url: string, filters: Record<string, string> = {}) {
  return httpResource<Page<unknown>>(() => ({ url, params: { ...filters, size: 1 } }));
}
