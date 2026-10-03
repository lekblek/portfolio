import { Routes } from '@angular/router';

/** Messages de contact (`/admin/messages`) : boîte de réception, puis chaque message. */
export const messageRoutes: Routes = [
  {
    path: '',
    title: 'Messages',
    data: { noindex: true },
    loadComponent: () => import('./pages/message-list-page').then((m) => m.MessageListPage),
  },
  {
    path: ':id',
    title: 'Message',
    data: { noindex: true },
    loadComponent: () => import('./pages/message-page').then((m) => m.MessagePage),
  },
];
