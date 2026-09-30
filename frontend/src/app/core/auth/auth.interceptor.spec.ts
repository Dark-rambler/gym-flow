import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { AuthResponse } from '../../api/models';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { AuthStore } from './auth.store';

const session = (accessToken: string): AuthResponse => ({
  accessToken,
  refreshToken: 'refresh-' + accessToken,
  expiresIn: 900,
  user: { id: 1, fullName: 'Ana Pérez', email: 'ana@gym.pe', role: 'OWNER', gymId: 7, gymName: 'Gym Demo' },
});

/** Deja correr las promesas pendientes (el refresh es async). */
const flushMicrotasks = () => new Promise((resolve) => setTimeout(resolve));

describe('authInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;
  let store: AuthStore;
  const auth = { refresh: vi.fn(), sessionExpired: vi.fn() };

  beforeEach(() => {
    localStorage.clear();
    auth.refresh.mockReset();
    auth.sessionExpired.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
    store = TestBed.inject(AuthStore);
    store.setSession(session('old'));
  });

  afterEach(() => ctrl.verify());

  it('añade el access token como Bearer', async () => {
    const result = firstValueFrom(http.get('/api/me'));
    const req = ctrl.expectOne('/api/me');
    expect(req.request.headers.get('Authorization')).toBe('Bearer old');
    req.flush({ ok: true });
    await expect(result).resolves.toEqual({ ok: true });
  });

  it('no envía Bearer a las rutas de auth', () => {
    http.post('http://localhost:8080/api/auth/refresh', {}).subscribe();
    const req = ctrl.expectOne('http://localhost:8080/api/auth/refresh');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('ante un 401 refresca una vez y reintenta con el token nuevo', async () => {
    auth.refresh.mockImplementation(async () => {
      store.setSession(session('new'));
      return true;
    });
    const result = firstValueFrom(http.get('/api/staff'));

    ctrl.expectOne('/api/staff').flush({ message: 'expirado' }, { status: 401, statusText: 'Unauthorized' });
    await flushMicrotasks();

    expect(auth.refresh).toHaveBeenCalledExactlyOnceWith('old');
    const retry = ctrl.expectOne('/api/staff');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer new');
    retry.flush([{ id: 1 }]);
    await expect(result).resolves.toEqual([{ id: 1 }]);
  });

  it('si el refresh falla, cierra la sesión y propaga el 401', async () => {
    auth.refresh.mockResolvedValue(false);
    const result = firstValueFrom(http.get('/api/staff'));

    ctrl.expectOne('/api/staff').flush({ message: 'expirado' }, { status: 401, statusText: 'Unauthorized' });

    await expect(result).rejects.toMatchObject({ status: 401 });
    expect(auth.sessionExpired).toHaveBeenCalledOnce();
  });

  it('no intenta refrescar otros errores', async () => {
    const result = firstValueFrom(http.get('/api/staff'));
    ctrl.expectOne('/api/staff').flush({ message: 'prohibido' }, { status: 403, statusText: 'Forbidden' });

    await expect(result).rejects.toMatchObject({ status: 403 });
    expect(auth.refresh).not.toHaveBeenCalled();
  });
});
