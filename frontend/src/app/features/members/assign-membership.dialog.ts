import { ChangeDetectionStrategy, Component, computed, inject, resource, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api } from '../../api/api';
import { assignMembership, listPlans } from '../../api/functions';
import { MembershipResponse } from '../../api/models';
import { apiErrorMessage } from '../../core/http/api-error';
import { ButtonComponent } from '../../shared/ui/button/button.component';
import { FormFieldComponent } from '../../shared/ui/form-field/form-field.component';
import { addDays, nextStartDate, todayIso } from './membership-status';

export interface AssignMembershipData {
  memberId: number;
  memberName: string;
  memberships: MembershipResponse[];
  canSetPrice: boolean;
}

/** Vende o renueva. Cierra con la membresía creada. */
@Component({
  selector: 'gf-assign-membership-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe, FormFieldComponent, ButtonComponent],
  host: { class: 'block w-[calc(100vw-2rem)] max-w-md rounded-2xl bg-white p-6 shadow-xl' },
  template: `
    <h2 class="text-lg font-semibold text-neutral-900">{{ isRenewal ? 'Renovar membresía' : 'Asignar membresía' }}</h2>
    <p class="mt-1 text-sm text-neutral-500">{{ data.memberName }}</p>

    @if (error()) {
      <p class="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">{{ error() }}</p>
    }

    @if (plans.isLoading()) {
      <p class="mt-5 text-sm text-neutral-500">Cargando planes…</p>
    } @else if (!plans.value()?.length) {
      <div class="mt-5 rounded-lg bg-amber-50 p-4 text-sm text-amber-900">
        No hay planes activos.
        @if (data.canSetPrice) {
          <a routerLink="/planes" class="font-semibold underline" (click)="ref.close()">Crea uno primero</a>.
        } @else {
          Pide al dueño que cree los planes.
        }
      </div>
    } @else {
      <div class="mt-5 space-y-4">
        <fieldset>
          <legend class="mb-1.5 text-sm font-medium text-neutral-700">Plan</legend>
          <div class="grid gap-2">
            @for (plan of plans.value(); track plan.id) {
              <label class="flex cursor-pointer items-center gap-3 rounded-lg p-3 ring-1 ring-neutral-200 has-[:checked]:bg-brand-50 has-[:checked]:ring-2 has-[:checked]:ring-brand-500">
                <input type="radio" name="plan" class="accent-brand-600" [value]="plan.id" [formControl]="planId" />
                <span class="flex-1 text-sm font-medium text-neutral-900">{{ plan.name }}
                  <span class="font-normal text-neutral-500">· {{ plan.durationDays }} días</span></span>
                <span class="text-sm font-semibold">{{ plan.price | currency }}</span>
              </label>
            }
          </div>
        </fieldset>

        @if (data.canSetPrice && selectedPlan()) {
          <gf-form-field label="Precio cobrado (S/)" for="assignPrice" hint="Cámbialo solo para aplicar un descuento">
            <input id="assignPrice" type="number" min="0" step="0.01" class="gf-input" [formControl]="price" />
          </gf-form-field>
        }

        @if (preview(); as p) {
          <div class="rounded-lg bg-neutral-50 p-3 text-sm text-neutral-700">
            Vigencia: <strong>{{ p.start | date: 'dd/MM/yyyy' }}</strong> al <strong>{{ p.end | date: 'dd/MM/yyyy' }}</strong>
            @if (p.start !== today) {
              <span class="block text-xs text-neutral-500">Empieza al día siguiente del vencimiento actual.</span>
            }
          </div>
        }
      </div>
    }

    <div class="mt-6 flex justify-end gap-2">
      <button gfButton type="button" variant="secondary" (click)="ref.close()">Cancelar</button>
      <button gfButton type="button" [loading]="saving()" [disabled]="!selectedPlan()" (click)="submit()">Confirmar</button>
    </div>
  `,
})
export class AssignMembershipDialog {
  private readonly api = inject(Api);
  protected readonly ref = inject<DialogRef<MembershipResponse>>(DialogRef);
  protected readonly data = inject<AssignMembershipData>(DIALOG_DATA);

  protected readonly today = todayIso();
  protected readonly isRenewal = this.data.memberships.some((m) => m.status === 'ACTIVE' || m.status === 'SCHEDULED');
  protected readonly plans = resource({ loader: () => this.api.invoke(listPlans, { includeInactive: false }) });
  protected readonly planId = new FormControl<number | null>(null, Validators.required);
  protected readonly price = new FormControl<number | null>(null, Validators.min(0));
  private readonly planIdValue = toSignal(this.planId.valueChanges, { initialValue: null });

  protected readonly selectedPlan = computed(() => {
    const id = this.planIdValue();
    return this.plans.value()?.find((p) => p.id === Number(id)) ?? null;
  });
  protected readonly preview = computed(() => {
    const plan = this.selectedPlan();
    if (!plan) return null;
    const start = nextStartDate(this.data.memberships, this.today);
    return { start, end: addDays(start, plan.durationDays - 1) };
  });
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    // al elegir plan, el precio sugerido es el del plan
    this.planId.valueChanges.subscribe(() => this.price.setValue(this.selectedPlan()?.price ?? null));
  }

  async submit(): Promise<void> {
    const plan = this.selectedPlan();
    if (!plan) return;
    this.saving.set(true);
    this.error.set(null);
    const customPrice = this.data.canSetPrice && this.price.value !== null && this.price.value !== plan.price;
    try {
      const created = await this.api.invoke(assignMembership, {
        memberId: this.data.memberId,
        body: { planId: plan.id, price: customPrice ? this.price.value! : undefined },
      });
      this.ref.close(created);
    } catch (err) {
      this.error.set(apiErrorMessage(err));
    } finally {
      this.saving.set(false);
    }
  }
}
