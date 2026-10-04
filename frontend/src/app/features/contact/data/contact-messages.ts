import { HttpClient, httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { ContactMessageRequest, Profile } from '../../../core/api/api-types';

/**
 * Envoie un message du formulaire de contact (`POST /api/public/contact-messages`) : 202 sans
 * corps si le message est reçu ; 400 `VALIDATION_FAILED`, 429 `TOO_MANY_CONTACT_MESSAGES` (avec
 * `Retry-After`) sinon (D-EJ).
 */
export async function sendContactMessage(
  http: HttpClient,
  message: ContactMessageRequest,
): Promise<void> {
  await firstValueFrom(http.post<void>('/api/public/contact-messages', message));
}

/**
 * Profil public, pour le contexte de la page (liens professionnels, CV) : une requête lue au rendu
 * serveur puis reprise du cache de transfert. 404 (profil non publié) : la page n'affiche que le
 * formulaire. À appeler dans un contexte d'injection.
 */
export function contactProfileResource() {
  return httpResource<Profile>(() => '/api/public/profile');
}
