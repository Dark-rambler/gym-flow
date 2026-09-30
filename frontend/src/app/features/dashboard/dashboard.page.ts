import { ChangeDetectionStrategy, Component, computed, inject, resource } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Api } from '../../api/api';
import { getCurrentCash, getIncomeReport, listPlans, searchMembers } from '../../api/functions';
import { AuthStore } from '../../core/auth/auth.store';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { todayIso } from '../members/membership-status';

interface Step {
  title: string;
  description: string;
  link: string;
  done: boolean;
}

// Semana 3: primeros pasos + caja + ingresos. Asistencias y vencimientos llegan con el check-in (semana 4).
@Component({
  selector: 'gf-dashboard-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, CurrencyPipe, DatePipe, IconComponent],
  template: `
    <div class="mx-auto max-w-5xl">
      <h2 class="text-2xl font-semibold tracking-tight text-neutral-900">Hola, {{ firstName() }} 👋</h2>
      <p class="mt-1 text-neutral-600">Este es el panel de <strong class="font-semibold">{{ user()?.gymName }}</strong>.</p>

      @if (steps(); as list) {
        @if (pending() > 0) {
          <section class="mt-6 rounded-xl bg-white p-5 ring-1 ring-neutral-200" aria-labelledby="steps-title">
            <h3 id="steps-title" class="font-semibold text-neutral-900">Primeros pasos</h3>
            <ol class="mt-3 space-y-2">
              @for (step of list; track step.title; let i = $index) {
                <li>
                  <a [routerLink]="step.link" class="flex items-center gap-3 rounded-lg p-2 hover:bg-neutral-50">
                    <span class="flex size-7 shrink-0 items-center justify-center rounded-full text-sm font-semibold"
                          [class]="step.done ? 'bg-brand-600 text-white' : 'bg-neutral-100 text-neutral-600'">
                      {{ step.done ? '✓' : i + 1 }}
                    </span>
                    <span class="flex-1">
                      <span class="block text-sm font-medium" [class]="step.done ? 'text-neutral-400 line-through' : 'text-neutral-900'">{{ step.title }}</span>
                      <span class="block text-xs text-neutral-500">{{ step.description }}</span>
                    </span>
                  </a>
                </li>
              }
            </ol>
          </section>
        }
      }

      <div class="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <a routerLink="/caja" class="rounded-xl p-5 ring-1 hover:ring-brand-500"
           [class]="cashOpen() ? 'bg-white ring-neutral-200' : 'bg-amber-50 ring-amber-200'">
          <p class="flex items-center gap-2 text-sm text-neutral-500"><gf-icon name="cash" [size]="16" /> Caja</p>
          @if (cash.value(); as c) {
            @if (c.current; as cur) {
              <p class="mt-1 text-xl font-semibold text-neutral-900">Abierta</p>
              <p class="text-xs text-neutral-500">desde las {{ cur.session.openedAt | date: 'HH:mm' }} · {{ cur.payments.length }} cobros</p>
            } @else {
              <p class="mt-1 text-xl font-semibold text-amber-900">Cerrada</p>
              <p class="text-xs text-amber-800">Ábrela para poder cobrar</p>
            }
          } @else {
            <p class="mt-1 text-xl font-semibold text-neutral-300">—</p>
          }
        </a>
        @if (isManager()) {
          <div class="rounded-xl bg-white p-5 ring-1 ring-neutral-200">
            <p class="text-sm text-neutral-500">Ingresos de hoy</p>
            <p class="mt-1 text-2xl font-semibold tracking-tight text-neutral-900">{{ incomeToday() ?? 0 | currency }}</p>
          </div>
          <div class="rounded-xl bg-white p-5 ring-1 ring-neutral-200">
            <p class="text-sm text-neutral-500">Ingresos del mes</p>
            <p class="mt-1 text-2xl font-semibold tracking-tight text-neutral-900">{{ income.value()?.totals?.total ?? 0 | currency }}</p>
            <p class="text-xs text-neutral-500">{{ income.value()?.totals?.count ?? 0 }} ventas</p>
          </div>
        }
        <a routerLink="/socios" class="rounded-xl bg-white p-5 ring-1 ring-neutral-200 hover:ring-brand-500">
          <p class="text-sm text-neutral-500">Socios registrados</p>
          <p class="mt-1 text-2xl font-semibold tracking-tight text-neutral-900">{{ memberCount() ?? '—' }}</p>
        </a>
      </div>

      <div class="mt-8 flex gap-4 rounded-xl bg-white p-5 ring-1 ring-neutral-200">
        <span class="flex size-10 items-center justify-center rounded-lg bg-neutral-100 text-neutral-600"><gf-icon name="qr" /></span>
        <div>
          <p class="font-semibold text-neutral-900">Próximamente: Check-in</p>
          <p class="mt-0.5 text-sm text-neutral-600">Control de acceso por QR o DNI, asistencias del día y socios por vencer.</p>
        </div>
      </div>
    </div>
  `,
})
export class DashboardPage {
  private readonly api = inject(Api);
  private readonly store = inject(AuthStore);

  protected readonly user = this.store.user;
  protected readonly firstName = computed(() => this.store.user()?.fullName.split(' ')[0] ?? '');
  protected readonly isManager = computed(() => this.store.hasRole('OWNER', 'ADMIN'));

  private readonly today = todayIso();
  protected readonly plans = resource({ loader: () => this.api.invoke(listPlans, { includeInactive: false }) });
  private readonly members = resource({ loader: () => this.api.invoke(searchMembers, { page: 0, size: 1 }) });
  protected readonly cash = resource({ loader: () => this.api.invoke(getCurrentCash) });
  /** Ingresos del mes en curso (solo OWNER/ADMIN; recepción no llama al reporte). */
  protected readonly income = resource({
    params: () => (this.isManager() ? { from: this.today.slice(0, 8) + '01', to: this.today } : undefined),
    loader: ({ params }) => this.api.invoke(getIncomeReport, params),
  });

  protected readonly memberCount = computed(() => this.members.value()?.totalItems);
  protected readonly cashOpen = computed(() => !!this.cash.value()?.current);
  protected readonly incomeToday = computed(() => this.income.value()?.days.find((d) => d.date === this.today)?.total);

  protected readonly steps = computed<Step[] | null>(() => {
    if (!this.plans.hasValue() || !this.members.hasValue()) return null;
    const steps: Step[] = [];
    if (this.isManager()) {
      steps.push({ title: 'Crea tus planes', description: 'Por ejemplo Mensual (30 días) y Trimestral (90 días).', link: '/planes', done: (this.plans.value()?.length ?? 0) > 0 });
    }
    steps.push({ title: 'Abre la caja', description: 'Necesaria para cobrar las membresías.', link: '/caja', done: this.cashOpen() });
    steps.push({ title: 'Registra a tus socios', description: 'Con nombre y DNI; luego véndeles una membresía.', link: '/socios', done: (this.memberCount() ?? 0) > 0 });
    if (this.isManager()) {
      steps.push({ title: 'Agrega a tu equipo', description: 'Cuentas para administradores y recepción.', link: '/staff', done: false });
    }
    return steps;
  });
  protected readonly pending = computed(() => this.steps()?.filter((s) => !s.done).length ?? 0);
}
