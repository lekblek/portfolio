import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Service, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { AdminAccount, AdminCredentials } from '../../../core/api/api-types';

const SESSION = '/api/admin/session';

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
  private readonly closed = signal(false);

  /** Administrateur connecté ; `null` tant que la session n'est pas vérifiée ou ouverte. */
  readonly account = this.current.asReadonly();
  /** Vrai après une déconnexion demandée, jusqu'à la connexion suivante. */
  readonly closedByUser = this.closed.asReadonly();

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
    this.closed.set(false);
  }

  /** Déconnexion (204) : la session et son cookie sont supprimés par le serveur. */
  async close(): Promise<void> {
    await firstValueFrom(this.http.delete<void>(SESSION));
    this.current.set(null);
    this.closed.set(true);
  }
}
