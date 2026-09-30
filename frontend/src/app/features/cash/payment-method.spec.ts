import { differenceKind, needsReference, subtractMoney } from './payment-method';

describe('payment-method', () => {
  it('differenceKind clasifica el arqueo', () => {
    expect(differenceKind(0)).toBe('exact');
    expect(differenceKind(0.001)).toBe('exact');
    expect(differenceKind(5)).toBe('over');
    expect(differenceKind(-0.5)).toBe('short');
  });

  it('subtractMoney evita errores de coma flotante', () => {
    expect(subtractMoney(0.3, 0.1)).toBe(0.2);
    expect(subtractMoney(145, 150)).toBe(-5);
    expect(subtractMoney(100.1, 100.1)).toBe(0);
  });

  it('solo los métodos digitales piden referencia', () => {
    expect(needsReference('CASH')).toBe(false);
    expect(needsReference('YAPE')).toBe(true);
    expect(needsReference('CARD')).toBe(true);
  });
});
