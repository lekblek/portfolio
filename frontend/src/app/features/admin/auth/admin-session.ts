import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Service, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { AdminAccount, AdminCredentials } from '../../../core/api/api-types';

const SESSION = '/api/admin/session';

/** Fin de la dernière session : déconnexion demandée ou session expirée (401 pendant le travail). */
export type SessionEnd = 'closed' | 'expired';

/**
 * Session de l'administrateur (D-CO, 01-architecture §11), tenue par le cookie de session du
 * serveur ; ce service n'en garde que l'administrateur connecté, en mémoire (rien dans le stockage
 * du navigateur). `GET /api/admin/session` dépose aussi le jeton CSRF (`XSRF-TOKEN`), qu'Angular
 * renvoie ensuite dans `X-XSRF-TOKEN` (D-CP) : il précède donc toute connexion.
 */
@Service()
export class AdminSession {
  private readonly http = inject(HttpClient);
  private readonly current = signal<AdminAccount | null>(null);
  private readonly ended = signal<SessionEnd | null>(null);

  /** Administrateur connecté ; `null` tant que la session n'est pas vérifiée ou ouverte. */
  readonly account = this.current.asReadonly();
  /** Fin de la dernière session, jusqu'à la connexion suivante ; `null` sinon. */
  readonly end = this.ended.asReadonly();

  /**
   * Vrai si une session est ouverte : demandée au serveur une fois, puis gardée. Une 401 donne
   * `false` ; toute autre erreur (serveur injoignable) est rejetée.
   */
  async isOpen(): Promise<boolean> {
    if (this.current() !== null) {
      return true;
    }
    try {
      this.current.set(await firstValueFrom(this.http.get<AdminAccount>(SESSION)));
      return true;
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        return false;
      }
      throw error;
    }
  }

  /** Connexion : 401 `INVALID_CREDENTIALS` ou 429 `TOO_MANY_LOGIN_ATTEMPTS` rejetés tels quels. */
  async open(credentials: AdminCredentials): Promise<void> {
    this.current.set(await firstValueFrom(this.http.post<AdminAccount>(SESSION, credentials)));
    this.ended.set(null);
  }

  /** Déconnexion (204) : la session et son cookie sont supprimés par le serveur. */
  async close(): Promise<void> {
    await firstValueFrom(this.http.delete<void>(SESSION));
    this.current.set(null);
    this.ended.set('closed');
  }

  /** Session refusée par le serveur pendant le travail (401) : oubliée, à rouvrir. */
  expire(): void {
    this.current.set(null);
    this.ended.set('expired');
  }
}
