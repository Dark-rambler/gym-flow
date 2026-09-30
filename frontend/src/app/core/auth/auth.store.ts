import { Injectable, computed, signal } from '@angular/core';
import { AuthResponse, MeResponse } from '../../api/models';

export type Role = MeResponse['role'];

interface StoredSession {
  accessToken: string;
  refreshToken: string;
  user: MeResponse;
}

const STORAGE_KEY = 'gymflow.session';

function readStorage(): StoredSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as StoredSession) : null;
  } catch {
    return null;
  }
}

function writeStorage(session: StoredSession | null): void {
  try {
    if (session) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  } catch {
    // almacenamiento no disponible (modo privado): la sesión vive solo en memoria
  }
}

/**
 * Sesión del usuario (tokens + perfil) en signals, persistida en localStorage.
 * Se sincroniza entre pestañas: un login/logout/refresh en otra pestaña se refleja aquí.
 */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly session = signal<StoredSession | null>(readStorage());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly role = computed<Role | null>(() => this.session()?.user.role ?? null);
  readonly isAuthenticated = computed(() => this.session() !== null);

  constructor() {
    window.addEventListener('storage', (e) => {
      if (e.key === STORAGE_KEY) {
        this.syncFromStorage();
      }
    });
  }

  accessToken(): string | null {
    return this.session()?.accessToken ?? null;
  }

  refreshToken(): string | null {
    return this.session()?.refreshToken ?? null;
  }

  hasRole(...roles: Role[]): boolean {
    const role = this.role();
    return role !== null && roles.includes(role);
  }

  setSession(res: AuthResponse): void {
    const session = { accessToken: res.accessToken, refreshToken: res.refreshToken, user: res.user };
    this.session.set(session);
    writeStorage(session);
  }

  clear(): void {
    this.session.set(null);
    writeStorage(null);
  }

  /** Relee localStorage: otra pestaña pudo haber rotado los tokens o cerrado sesión. */
  syncFromStorage(): void {
    this.session.set(readStorage());
  }
}
