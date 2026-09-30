import { ChangeDetectionStrategy, Component, computed, inject, resource, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { Dialog } from '@angular/cdk/dialog';
import { Api } from '../../api/api';
import { listPlans, updatePlan } from '../../api/functions';
import { PlanResponse } from '../../api/models';
import { AuthStore } from '../../core/auth/auth.store';
import { apiErrorMessage } from '../../core/http/api-error';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';
import { ButtonComponent } from '../../shared/ui/button/button.component';
import { IconComponent } from '../../shared/ui/icon/icon.component';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { PlanFormDialog } from './plan-form.dialog';

@Component({
  selector: 'gf-plans-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, BadgeComponent, ButtonComponent, IconComponent],
  template: `
    <div class="mx-auto max-w-5xl">
      <div class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h2 class="text-2xl font-semibold tracking-tight text-neutral-900">Planes</h2>
          <p class="mt-1 text-sm text-neutral-600">Lo que vende tu gimnasio: duración y precio de cada membresía.</p>
        </div>
        @if (canManage()) {
          <button gfButton type="button" (click)="openForm()"><gf-icon name="plus" [size]="18" /> Nuevo plan</button>
        }
      </div>

      @if (plans.isLoading() && !plans.hasValue()) {
        <p class="mt-6 text-sm text-neutral-500">Cargando…</p>
      } @else if (plans.error()) {
        <p class="mt-6 text-sm text-red-700">{{ loadError() }}
          <button type="button" class="ml-2 font-semibold underline" (click)="plans.reload()">Reintentar</button></p>
      } @else {
        <div class="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          @for (plan of plans.value(); track plan.id) {
            <div class="flex flex-col rounded-xl bg-white p-5 ring-1 ring-neutral-200" [class.opacity-60]="!plan.active">
              <div class="flex items-start justify-between gap-2">
                <p class="font-semibold text-neutral-900">{{ plan.name }}</p>
                @if (!plan.active) {
                  <gf-badge tone="neutral">Inactivo</gf-badge>
                }
              </div>
              <p class="mt-3 text-2xl font-semibold tracking-tight text-neutral-900">{{ plan.price | currency }}</p>
              <p class="text-sm text-neutral-500">{{ plan.durationDays }} {{ plan.durationDays === 1 ? 'día' : 'días' }}</p>
              @if (canManage()) {
                <div class="mt-4 flex gap-2 border-t border-neutral-100 pt-4">
                  <button gfButton type="button" variant="secondary" (click)="openForm(plan)">Editar</button>
                  <button gfButton type="button" variant="ghost" [loading]="busyId() === plan.id" (click)="toggleActive(plan)">
                    {{ plan.active ? 'Desactivar' : 'Activar' }}
                  </button>
                </div>
              }
            </div>
          } @empty {
            <div class="rounded-xl border-2 border-dashed border-neutral-200 p-8 text-center sm:col-span-2 lg:col-span-3">
              <p class="font-medium text-neutral-900">Aún no hay planes</p>
              <p class="mt-1 text-sm text-neutral-500">
                {{ canManage() ? 'Crea el primero, por ejemplo "Mensual" de 30 días.' : 'Pide al dueño o administrador que cree los planes.' }}
              </p>
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class PlansPage {
  private readonly api = inject(Api);
  private readonly store = inject(AuthStore);
  private readonly dialog = inject(Dialog);
  private readonly toast = inject(ToastService);

  protected readonly canManage = computed(() => this.store.hasRole('OWNER', 'ADMIN'));
  // quien gestiona ve también los inactivos para poder reactivarlos
  protected readonly plans = resource({
    params: () => ({ includeInactive: this.canManage() }),
    loader: ({ params }) => this.api.invoke(listPlans, params),
  });
  protected readonly busyId = signal<number | null>(null);
  protected readonly loadError = computed(() => apiErrorMessage(this.plans.error(), 'No se pudieron cargar los planes'));

  protected openForm(plan?: PlanResponse): void {
    const ref = this.dialog.open<PlanResponse>(PlanFormDialog, { data: { plan }, ariaLabel: plan ? 'Editar plan' : 'Nuevo plan' });
    ref.closed.subscribe((saved) => {
      if (saved) {
        this.toast.success(plan ? 'Plan actualizado' : `Plan "${saved.name}" creado`);
        this.plans.reload();
      }
    });
  }

  protected async toggleActive(plan: PlanResponse): Promise<void> {
    this.busyId.set(plan.id);
    try {
      await this.api.invoke(updatePlan, {
        id: plan.id,
        body: { name: plan.name, durationDays: plan.durationDays, price: plan.price, active: !plan.active },
      });
      this.toast.success(plan.active ? 'Plan desactivado: ya no se puede vender' : 'Plan activado');
      this.plans.reload();
    } catch (err) {
      this.toast.error(apiErrorMessage(err));
    } finally {
      this.busyId.set(null);
    }
  }
}
