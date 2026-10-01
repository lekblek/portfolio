import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { ContactMessageRequest } from '../../../core/api/api-types';

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
