import { CheckInResponse } from '../../api/models';

export type CheckInOutcome = 'allowed' | 'duplicate' | 'denied';

export function outcomeOf(r: CheckInResponse): CheckInOutcome {
  if (r.result === 'DENIED') return 'denied';
  return r.duplicate ? 'duplicate' : 'allowed';
}

/** Minutos enteros transcurridos entre dos instantes ISO (para "ya registró hace X min"). */
export function minutesBetween(fromIso: string, to: Date = new Date()): number {
  return Math.max(0, Math.floor((to.getTime() - new Date(fromIso).getTime()) / 60000));
}

/** URL pública del carnet para el celular del socio (el token del QR es la credencial). */
export function publicCardUrl(payload: string, origin = window.location.origin): string {
  return `${origin}/carnet/${payload.replace(/^GF1:/i, '')}`;
}

export const REASON_LABEL: Record<NonNullable<CheckInResponse['reason']>, string> = {
  INVALID_CODE: 'Código no válido',
  UNKNOWN: 'No encontrado',
  MEMBER_INACTIVE: 'Socio desactivado',
  NO_MEMBERSHIP: 'Sin membresía',
  NOT_STARTED: 'Aún no empieza',
  FROZEN: 'Congelada',
  EXPIRED: 'Vencida',
};
