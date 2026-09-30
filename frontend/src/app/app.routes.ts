import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Iniciar sesión · gymFlow',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'registro',
    title: 'Registra tu gimnasio · gymFlow',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register.page').then((m) => m.RegisterPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./core/layout/shell.component').then((m) => m.ShellComponent),
    children: [
      {
        path: '',
        title: 'Dashboard · gymFlow',
        loadComponent: () => import('./features/dashboard/dashboard.page').then((m) => m.DashboardPage),
      },
      {
        path: 'socios',
        title: 'Socios · gymFlow',
        loadComponent: () => import('./features/members/members.page').then((m) => m.MembersPage),
      },
      {
        path: 'socios/:id',
        title: 'Socio · gymFlow',
        loadComponent: () => import('./features/members/member-detail.page').then((m) => m.MemberDetailPage),
      },
      {
        path: 'planes',
        title: 'Planes · gymFlow',
        loadComponent: () => import('./features/plans/plans.page').then((m) => m.PlansPage),
      },
      {
        path: 'staff',
        title: 'Staff · gymFlow',
        canActivate: [roleGuard('OWNER', 'ADMIN')],
        loadComponent: () => import('./features/staff/staff.page').then((m) => m.StaffPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
