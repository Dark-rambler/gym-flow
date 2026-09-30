import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { AuthStore } from './auth.store';

/** Rutas de auth y públicas (carnet): sin Bearer y sin reintento (evita bucles de refresh). */
const isAuthEndpoint = (req: HttpRequest<unknown>) => req.url.includes('/api/auth/') || req.url.includes('/api/public/');

const withToken = (req: HttpRequest<unknown>, token: string | null) =>
  token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

/**
 * Añade el access token y, ante un 401, refresca la sesión una vez y reintenta la petición.
 * Si el refresh falla, cierra la sesión y lleva al login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (isAuthEndpoint(req)) {
    return next(req);
  }
  const store = inject(AuthStore);
  const auth = inject(AuthService);
  const token = store.accessToken();

  return next(withToken(req, token)).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse) || err.status !== 401 || !token) {
        return throwError(() => err);
      }
      return from(auth.refresh(token)).pipe(
        switchMap((ok) => {
          if (!ok) {
            auth.sessionExpired();
            return throwError(() => err);
          }
          return next(withToken(req, store.accessToken()));
        }),
      );
    }),
  );
};
