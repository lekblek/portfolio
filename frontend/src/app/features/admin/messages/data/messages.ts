import { HttpClient, httpResource } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import {
  AdminContactMessage,
  AdminContactMessageSummary,
  ContactStatus,
} from '../../../../core/api/api-types';
import { Page, toHttpParams } from '../../../../core/api/page';

const API = '/api/admin/contact-messages';

export interface MessageListRequest {
  page: number;
  status?: ContactStatus;
}

/** Statuts dans l'ordre du cycle (D-CZ), avec leur libellé de filtre. */
export const STATUS_FILTERS: readonly { value: ContactStatus; label: string }[] = [
  { value: 'NEW', label: 'Nouveaux' },
  { value: 'READ', label: 'Lus' },
  { value: 'PROCESSED', label: 'Traités' },
  { value: 'ARCHIVED', label: 'Archivés' },
];

const ORDER: readonly ContactStatus[] = ['NEW', 'READ', 'PROCESSED', 'ARCHIVED'];

/** Action qui fait avancer un message ; jamais en arrière (invariant 29). */
export interface MessageTransition {
  target: ContactStatus;
  label: string;
}

const LABELS: Record<ContactStatus, string> = {
  NEW: 'Marquer comme nouveau',
  READ: 'Marquer comme lu',
  PROCESSED: 'Marquer comme traité',
  ARCHIVED: 'Archiver',
};

/** Statuts après le statut actuel, dans l'ordre du cycle (sauter des étapes est permis). */
export function forwardFrom(status: ContactStatus): readonly MessageTransition[] {
  return ORDER.slice(ORDER.indexOf(status) + 1).map((target) => ({
    target,
    label: LABELS[target],
  }));
}

/** Boîte de réception (20 par page), les plus récents d'abord. Contexte d'injection. */
export function messagePageResource(request: () => MessageListRequest) {
  return httpResource<Page<AdminContactMessageSummary>>(() => ({
    url: API,
    params: toHttpParams({ ...request() }),
  }));
}

/** Un message ; le lire ne change pas son statut (D-CZ). Contexte d'injection. */
export function messageResource(id: () => string) {
  return httpResource<AdminContactMessage>(() => `${API}/${id()}`);
}

/** 409 `INVALID_CONTACT_MESSAGE_TRANSITION` si le statut n'est pas après l'actuel. */
export function changeMessageStatus(
  http: HttpClient,
  id: number,
  status: ContactStatus,
): Promise<AdminContactMessage> {
  return firstValueFrom(http.post<AdminContactMessage>(`${API}/${id}/status`, { status }));
}

/** Suppression définitive (204), à la demande de la personne (F30, D-EX). */
export async function deleteMessage(http: HttpClient, id: number): Promise<void> {
  await firstValueFrom(http.delete<void>(`${API}/${id}`));
}

/** Lien « Répondre » : la messagerie de l'administrateur, sujet repris. */
export function replyLink(message: AdminContactMessage): string {
  return `mailto:${message.email}?subject=${encodeURIComponent(`Re: ${message.subject}`)}`;
}
