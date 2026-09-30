import { PaymentResponse } from '../../api/models';

export type PaymentMethod = PaymentResponse['method'];

export const PAYMENT_METHODS: PaymentMethod[] = ['CASH', 'YAPE', 'PLIN', 'CARD'];

export const METHOD_LABEL: Record<PaymentMethod, string> = {
  CASH: 'Efectivo',
  YAPE: 'Yape',
  PLIN: 'Plin',
  CARD: 'Tarjeta',
};

/** Solo el efectivo entra en el arqueo; los demás piden número de operación opcional. */
export function needsReference(method: PaymentMethod): boolean {
  return method !== 'CASH';
}

export type DifferenceKind = 'exact' | 'over' | 'short';

/** Resultado del arqueo: contado − esperado (con tolerancia de medio céntimo por redondeo). */
export function differenceKind(difference: number): DifferenceKind {
  if (Math.abs(difference) < 0.005) return 'exact';
  return difference > 0 ? 'over' : 'short';
}

export const DIFFERENCE_TEXT: Record<DifferenceKind, string> = {
  exact: 'Cuadra',
  over: 'Sobra',
  short: 'Falta',
};

export const DIFFERENCE_CLASS: Record<DifferenceKind, string> = {
  exact: 'text-emerald-700',
  over: 'text-amber-700',
  short: 'text-red-700',
};

/** Resta en céntimos para no arrastrar errores de coma flotante (0.1 + 0.2). */
export function subtractMoney(a: number, b: number): number {
  return (Math.round(a * 100) - Math.round(b * 100)) / 100;
}
