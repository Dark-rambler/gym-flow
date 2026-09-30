import { ChangeDetectionStrategy, Component, computed, inject, resource } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../../api/api';
import { listPlans, searchMembers } from '../../api/functions';
import { AuthStore } from '../../core/auth/auth.store';
import { IconComponent, IconName } from '../../shared/ui/icon/icon.component';

interface Step {
  title: string;
  description: string;
  link: string;
  done: boolean;
}

interface Upcoming {
  title: string;
  description: string;
  icon: IconName;
}

const UPCOMING: Upcoming[] = [
  { title: 'Caja', description: 'Pagos en efectivo, Yape, Plin o tarjeta y cierre diario.', icon: 'cash' },
  { title: 'Check-in', description: 'Control de acceso por QR o DNI desde recepción.', icon: 'qr' },
];

// Semana 2: checklist de primeros pasos. Las métricas (activos, vencimientos, ingresos) llegan con caja y check-in.
@Component({
  selector: 'gf-dashboard-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, IconComponent],
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

      <div class="mt-6 grid gap-4 sm:grid-cols-2">
        <a routerLink="/socios" class="rounded-xl bg-white p-5 ring-1 ring-neutral-200 hover:ring-brand-500">
          <p class="text-sm text-neutral-500">Socios registrados</p>
          <p class="mt-1 text-3xl font-semibold tracking-tight text-neutral-900">{{ memberCount() ?? '—' }}</p>
        </a>
        <a routerLink="/planes" class="rounded-xl bg-white p-5 ring-1 ring-neutral-200 hover:ring-brand-500">
          <p class="text-sm text-neutral-500">Planes activos</p>
          <p class="mt-1 text-3xl font-semibold tracking-tight text-neutral-900">{{ plans.value()?.length ?? '—' }}</p>
        </a>
      </div>

      <h3 class="mt-8 text-sm font-semibold uppercase tracking-wide text-neutral-500">Próximamente</h3>
      <div class="mt-3 grid gap-4 sm:grid-cols-2">
        @for (item of upcoming; track item.title) {
          <div class="flex gap-4 rounded-xl bg-white p-5 ring-1 ring-neutral-200">
            <span class="flex size-10 items-center justify-center rounded-lg bg-neutral-100 text-neutral-600">
              <gf-icon [name]="item.icon" />
            </span>
            <div>
              <p class="font-semibold text-neutral-900">{{ item.title }}</p>
              <p class="mt-0.5 text-sm text-neutral-600">{{ item.description }}</p>
            </div>
          </div>
        }
      </div>
    </div>
  `,
})
export class DashboardPage {
  private readonly api = inject(Api);
  private readonly store = inject(AuthStore);

  protected readonly user = this.store.user;
  protected readonly firstName = computed(() => this.store.user()?.fullName.split(' ')[0] ?? '');
  protected readonly upcoming = UPCOMING;

  protected readonly plans = resource({ loader: () => this.api.invoke(listPlans, { includeInactive: false }) });
  private readonly members = resource({ loader: () => this.api.invoke(searchMembers, { page: 0, size: 1 }) });
  protected readonly memberCount = computed(() => this.members.value()?.totalItems);

  protected readonly steps = computed<Step[] | null>(() => {
    if (!this.plans.hasValue() || !this.members.hasValue()) return null;
    const isManager = this.store.hasRole('OWNER', 'ADMIN');
    const steps: Step[] = [];
    if (isManager) {
      steps.push({ title: 'Crea tus planes', description: 'Por ejemplo Mensual (30 días) y Trimestral (90 días).', link: '/planes', done: (this.plans.value()?.length ?? 0) > 0 });
    }
    steps.push({ title: 'Registra a tus socios', description: 'Con nombre y DNI; luego asígnales una membresía.', link: '/socios', done: (this.memberCount() ?? 0) > 0 });
    if (isManager) {
      steps.push({ title: 'Agrega a tu equipo', description: 'Cuentas para administradores y recepción.', link: '/staff', done: false });
    }
    return steps;
  });
  protected readonly pending = computed(() => this.steps()?.filter((s) => !s.done).length ?? 0);
}
