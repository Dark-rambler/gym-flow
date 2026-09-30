import { Role } from '../../core/auth/auth.store';

export const ROLE_LABEL: Record<Role, string> = {
  OWNER: 'Dueño',
  ADMIN: 'Administrador',
  RECEPTIONIST: 'Recepción',
};

/** Espejo de las reglas del backend (UpdateStaffUseCase/CreateStaffUseCase) para no ofrecer acciones prohibidas. */
export function assignableRoles(actor: Role | null): Role[] {
  if (actor === 'OWNER') return ['OWNER', 'ADMIN', 'RECEPTIONIST'];
  if (actor === 'ADMIN') return ['RECEPTIONIST'];
  return [];
}

export function canManage(actor: Role | null, target: Role): boolean {
  return actor === 'OWNER' || (actor === 'ADMIN' && target === 'RECEPTIONIST');
}
