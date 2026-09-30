import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore, Role } from './auth.store';

/** Rutas de la app: requieren sesión. */
export const authGuard: CanActivateFn = (_route, state) => {
  const store = inject(AuthStore);
  return (
    store.isAuthenticated() ||
    inject(Router).createUrlTree(['/login'], { queryParams: { volver: state.url === '/' ? null : state.url } })
  );
};

/** Login y registro: si ya hay sesión, al dashboard. */
export const guestGuard: CanActivateFn = () =>
  !inject(AuthStore).isAuthenticated() || inject(Router).createUrlTree(['/']);

/** Restringe por rol (la autorización real la hace el backend; esto es UX). */
export const roleGuard =
  (...roles: Role[]): CanActivateFn =>
  () =>
    inject(AuthStore).hasRole(...roles) || inject(Router).createUrlTree(['/']);
