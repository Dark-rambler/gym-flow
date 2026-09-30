import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Api } from '../../api/api';
import { login, logout, refresh, registerGym } from '../../api/functions';
import { LoginRequest, RegisterGymRequest } from '../../api/models';
import { AuthStore } from './auth.store';

const REFRESH_LOCK = 'gymflow-refresh';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(Api);
  private readonly store = inject(AuthStore);
  private readonly router = inject(Router);

  private refreshing: Promise<boolean> | null = null;

  async login(body: LoginRequest): Promise<void> {
    this.store.setSession(await this.api.invoke(login, { body }));
  }

  async registerGym(body: RegisterGymRequest): Promise<void> {
    this.store.setSession(await this.api.invoke(registerGym, { body }));
  }

  async logout(): Promise<void> {
    const refreshToken = this.store.refreshToken();
    this.store.clear();
    await this.router.navigateByUrl('/login');
    if (refreshToken) {
      // best effort: si falla, el token caduca solo
      this.api.invoke(logout, { body: { refreshToken } }).catch(() => undefined);
    }
  }

  /** Sesión perdida (refresh inválido): limpia y manda al login avisando al usuario. */
  sessionExpired(): void {
    this.store.clear();
    void this.router.navigate(['/login'], { queryParams: { expirada: 1 } });
  }

  /**
   * Obtiene un access token nuevo. Nunca hay dos refresh a la vez: dentro de la pestaña se comparte la
   * promesa y entre pestañas se serializa con Web Locks. El backend trata un refresh token reutilizado
   * como robo y revoca todas las sesiones, así que esto es obligatorio, no una optimización.
   *
   * @param failedAccessToken el token que recibió el 401; si ya cambió, otra pestaña refrescó antes.
   */
  refresh(failedAccessToken: string | null): Promise<boolean> {
    this.refreshing ??= this.refreshWithLock(failedAccessToken).finally(() => (this.refreshing = null));
    return this.refreshing;
  }

  private refreshWithLock(failedAccessToken: string | null): Promise<boolean> {
    const run = () => this.doRefresh(failedAccessToken);
    return navigator.locks ? navigator.locks.request(REFRESH_LOCK, run) : run();
  }

  private async doRefresh(failedAccessToken: string | null): Promise<boolean> {
    this.store.syncFromStorage();
    const current = this.store.accessToken();
    if (current && current !== failedAccessToken) {
      return true;
    }
    const refreshToken = this.store.refreshToken();
    if (!refreshToken) {
      return false;
    }
    try {
      this.store.setSession(await this.api.invoke(refresh, { body: { refreshToken } }));
      return true;
    } catch {
      return false;
    }
  }
}
