import { CheckInResponse } from '../../api/models';
import { minutesBetween, outcomeOf, publicCardUrl } from './check-in-code';

const response = (overrides: Partial<CheckInResponse>): CheckInResponse => ({
  result: 'ALLOWED',
  duplicate: false,
  message: 'Bienvenido/a',
  checkedAt: '2026-03-10T15:00:00Z',
  ...overrides,
});

describe('check-in-code', () => {
  it('outcomeOf distingue permitido, repetido y denegado', () => {
    expect(outcomeOf(response({}))).toBe('allowed');
    expect(outcomeOf(response({ duplicate: true }))).toBe('duplicate');
    expect(outcomeOf(response({ result: 'DENIED', reason: 'EXPIRED' }))).toBe('denied');
  });

  it('minutesBetween redondea hacia abajo y nunca es negativo', () => {
    expect(minutesBetween('2026-03-10T15:00:00Z', new Date('2026-03-10T15:42:59Z'))).toBe(42);
    expect(minutesBetween('2026-03-10T15:00:00Z', new Date('2026-03-10T14:00:00Z'))).toBe(0);
  });

  it('publicCardUrl quita el prefijo GF1:', () => {
    expect(publicCardUrl('GF1:abc-123', 'https://gym.app')).toBe('https://gym.app/carnet/abc-123');
  });
});
