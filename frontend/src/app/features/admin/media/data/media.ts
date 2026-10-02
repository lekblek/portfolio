import { HttpClient, HttpEvent, httpResource } from '@angular/common/http';
import { firstValueFrom, Observable } from 'rxjs';

import { AdminMedia } from '../../../../core/api/api-types';
import { Page, PageRequest, toHttpParams } from '../../../../core/api/page';

const API = '/api/admin/media';
const MEBI = 1024 * 1024;

/** Tailles maximales par format (D-BR, `01` §12, « Mo » lu comme mébioctet). */
export const IMAGE_MAX_BYTES = 5 * MEBI;
export const PDF_MAX_BYTES = 10 * MEBI;

/** Formats acceptés, et leurs extensions quand le système n'annonce pas de type. */
const ACCEPTED: Record<string, readonly string[]> = {
  'image/png': ['png'],
  'image/jpeg': ['jpg', 'jpeg'],
  'image/webp': ['webp'],
  'application/pdf': ['pdf'],
};

/** Filtre du sélecteur de fichiers (`accept`). */
export const ACCEPT = Object.keys(ACCEPTED).join(',');

/** Page de la médiathèque, les plus récents d'abord (20 par page, D-CT). Contexte d'injection. */
export function mediaPageResource(request: () => PageRequest) {
  return httpResource<Page<AdminMedia>>(() => ({ url: API, params: toHttpParams(request()) }));
}

/** Un média (`GET /{id}`) ; 404 s'il n'existe pas. Contexte d'injection. */
export function mediaResource(id: () => string | undefined) {
  return httpResource<AdminMedia>(() => {
    const current = id();
    return current === undefined ? undefined : `${API}/${current}`;
  });
}

/**
 * Envoi d'un fichier (`multipart/form-data`, partie `file`), avec les événements de progression.
 * Refus du serveur : 413 `MEDIA_TOO_LARGE`, 415 `UNSUPPORTED_MEDIA_FORMAT` (signature du contenu).
 */
export function uploadMedia(http: HttpClient, file: File): Observable<HttpEvent<AdminMedia>> {
  const body = new FormData();
  body.append('file', file, file.name);
  return http.post<AdminMedia>(API, body, { observe: 'events', reportProgress: true });
}

/** Texte alternatif : seule donnée modifiable d'un média (D-CT) ; vide → aucun. */
export function updateAltText(http: HttpClient, id: number, altText: string): Promise<AdminMedia> {
  return firstValueFrom(http.patch<AdminMedia>(`${API}/${id}`, { altText }));
}

/** Suppression (204) ; 409 `MEDIA_STILL_REFERENCED` si un contenu l'utilise (D-BV). */
export async function deleteMedia(http: HttpClient, id: number): Promise<void> {
  await firstValueFrom(http.delete<void>(`${API}/${id}`));
}

/** Vrai pour une image, faux pour un PDF. */
export function isImage(media: Pick<AdminMedia, 'format'>): boolean {
  return media.format !== 'PDF';
}

/**
 * Vérification avant l'envoi, pour répondre tout de suite : format accepté (type annoncé, sinon
 * extension) et taille de son format. Le serveur reste l'arbitre (signature du contenu).
 * Retourne la raison du refus, ou `null`.
 */
export function uploadProblem(file: File): string | null {
  const extension = file.name.split('.').pop()?.toLowerCase() ?? '';
  const type =
    file.type in ACCEPTED
      ? file.type
      : Object.keys(ACCEPTED).find((candidate) => ACCEPTED[candidate].includes(extension));
  if (type === undefined) {
    return 'Format refusé\u202f: PNG, JPEG, WebP ou PDF seulement.';
  }
  if (file.size === 0) {
    return 'Fichier vide.';
  }
  const pdf = type === 'application/pdf';
  if (file.size > (pdf ? PDF_MAX_BYTES : IMAGE_MAX_BYTES)) {
    return pdf
      ? 'Fichier trop lourd\u202f: 10\u00a0Mo au plus pour un PDF.'
      : 'Fichier trop lourd\u202f: 5\u00a0Mo au plus pour une image.';
  }
  return null;
}
