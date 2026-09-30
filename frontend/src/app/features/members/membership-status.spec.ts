import { MembershipResponse } from '../../api/models';
import { addDays, expiresSoon, nextStartDate, remainingLabel, todayIso } from './membership-status';

const membership = (overrides: Partial<MembershipResponse>): MembershipResponse => ({
  id: 1,
  planName: 'Mensual',
  price: 100,
  startDate: '2026-03-10',
  endDate: '2026-04-08',
  status: 'ACTIVE',
  frozenDays: 0,
  daysLeft: 30,
  ...overrides,
});

describe('membership-status', () => {
  it('addDays cruza meses y años sin desfase horario', () => {
    expect(addDays('2026-01-31', 1)).toBe('2026-02-01');
    expect(addDays('2026-12-31', 1)).toBe('2027-01-01');
    expect(addDays('2026-03-10', 29)).toBe('2026-04-08');
  });

  it('nextStartDate encadena al último vencimiento e ignora canceladas', () => {
    const list = [membership({}), membership({ id: 2, endDate: '2026-06-01', status: 'CANCELLED' })];
    expect(nextStartDate(list, '2026-03-15')).toBe('2026-04-09');
    expect(nextStartDate(list, '2026-05-01')).toBe('2026-05-01'); // ya venció: empieza hoy
    expect(nextStartDate([], '2026-03-15')).toBe('2026-03-15');
  });

  it('remainingLabel describe cada estado', () => {
    expect(remainingLabel(membership({ daysLeft: 12 }))).toBe('12 días restantes');
    expect(remainingLabel(membership({ daysLeft: 1 }))).toBe('Último día');
    expect(remainingLabel(membership({ status: 'EXPIRED', daysLeft: 0 }))).toBe('Venció el 08/04/2026');
    expect(remainingLabel(membership({ status: 'SCHEDULED' }))).toBe('Empieza el 10/03/2026');
  });

  it('expiresSoon solo para activas con pocos días', () => {
    expect(expiresSoon(membership({ daysLeft: 5 }))).toBe(true);
    expect(expiresSoon(membership({ daysLeft: 20 }))).toBe(false);
    expect(expiresSoon(membership({ status: 'SCHEDULED', daysLeft: 3 }))).toBe(false);
  });

  it('todayIso usa la fecha local', () => {
    expect(todayIso(new Date(2026, 0, 5, 23, 30))).toBe('2026-01-05');
  });
});
