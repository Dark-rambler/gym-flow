import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthStore } from '../../core/auth/auth.store';
import { IconComponent, IconName } from '../../shared/ui/icon/icon.component';

interface Upcoming {
  title: string;
  description: string;
  icon: IconName;
}

const UPCOMING: Upcoming[] = [
  { title: 'Socios', description: 'Registro de socios con búsqueda por nombre o DNI.', icon: 'users' },
  { title: 'Membresías', description: 'Planes, renovaciones y vencimientos.', icon: 'card' },
  { title: 'Caja', description: 'Pagos en efectivo, Yape, Plin o tarjeta y cierre diario.', icon: 'cash' },
  { title: 'Check-in', description: 'Control de acceso por QR o DNI desde recepción.', icon: 'qr' },
];

// Placeholder de la semana 1: las métricas llegan con los módulos de socios, caja y check-in.
@Component({
  selector: 'gf-dashboard-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, IconComponent],
  template: `
    <div class="mx-auto max-w-5xl">
      <h2 class="text-2xl font-semibold tracking-tight text-neutral-900">Hola, {{ firstName() }} 👋</h2>
      <p class="mt-1 text-neutral-600">Este es el panel de <strong class="font-semibold">{{ user()?.gymName }}</strong>.</p>

      @if (canManageStaff()) {
        <div class="mt-6 flex flex-col gap-3 rounded-xl bg-brand-50 p-5 ring-1 ring-brand-100 sm:flex-row sm:items-center">
          <div class="flex-1">
            <p class="font-semibold text-brand-900">Primer paso: agrega a tu equipo</p>
            <p class="text-sm text-brand-900/80">Crea cuentas para tus administradores y recepcionistas.</p>
          </div>
          <a routerLink="/staff" class="inline-flex items-center justify-center rounded-lg bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-700">Gestionar staff</a>
        </div>
      }

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
  private readonly store = inject(AuthStore);

  protected readonly user = this.store.user;
  protected readonly firstName = computed(() => this.store.user()?.fullName.split(' ')[0] ?? '');
  protected readonly canManageStaff = computed(() => this.store.hasRole('OWNER', 'ADMIN'));
  protected readonly upcoming = UPCOMING;
}
