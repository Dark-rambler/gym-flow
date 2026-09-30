import { MembershipResponse } from '../../api/models';
import { BadgeTone } from '../../shared/ui/badge/badge.component';

export type MembershipStatus = MembershipResponse['status'];

export const STATUS_LABEL: Record<MembershipStatus, string> = {
  ACTIVE: 'Activa',
  SCHEDULED: 'Programada',
  FROZEN: 'Congelada',
  EXPIRED: 'Vencida',
  CANCELLED: 'Cancelada',
};

export const STATUS_TONE: Record<MembershipStatus, BadgeTone> = {
  ACTIVE: 'green',
  SCHEDULED: 'blue',
  FROZEN: 'amber',
  EXPIRED: 'red',
  CANCELLED: 'neutral',
};

/** Texto corto de vigencia para listados. */
export function remainingLabel(m: MembershipResponse): string {
  switch (m.status) {
    case 'ACTIVE':
      return m.daysLeft <= 1 ? 'Último día' : `${m.daysLeft} días restantes`;
    case 'SCHEDULED':
      return `Empieza el ${formatIso(m.startDate)}`;
    case 'FROZEN':
      return `Congelada desde el ${formatIso(m.frozenSince ?? m.startDate)}`;
    case 'EXPIRED':
      return `Venció el ${formatIso(m.endDate)}`;
    case 'CANCELLED':
      return 'Cancelada';
  }
}

export function expiresSoon(m: MembershipResponse, withinDays = 7): boolean {
  return m.status === 'ACTIVE' && m.daysLeft <= withinDays;
}

/**
 * Misma regla que el backend (Memberships.nextStartDate): hoy, o el día siguiente al último vencimiento
 * no cancelado si todavía no pasó. Solo para previsualizar; la fecha real la calcula el servidor.
 */
export function nextStartDate(memberships: MembershipResponse[], todayIso: string): string {
  const lastEnd = memberships
    .filter((m) => m.status !== 'CANCELLED')
    .map((m) => m.endDate)
    .sort()
    .at(-1);
  if (!lastEnd) return todayIso;
  const next = addDays(lastEnd, 1);
  return next > todayIso ? next : todayIso;
}

/** Suma días a una fecha ISO (yyyy-MM-dd) sin pasar por zonas horarias. */
export function addDays(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number);
  const date = new Date(Date.UTC(y, m - 1, d + days));
  return date.toISOString().slice(0, 10);
}

export function todayIso(now = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

function formatIso(iso: string): string {
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
}
