import { ChangeDetectionStrategy, Component, computed, inject, resource, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api } from '../../api/api';
import { assignMembership, getCurrentCash, listPlans } from '../../api/functions';
import { MembershipResponse } from '../../api/models';
import { apiErrorMessage } from '../../core/http/api-error';
import { ButtonComponent } from '../../shared/ui/button/button.component';
import { FormFieldComponent } from '../../shared/ui/form-field/form-field.component';
import { METHOD_LABEL, PAYMENT_METHODS, PaymentMethod, needsReference } from '../cash/payment-method';
import { addDays, nextStartDate, todayIso } from './membership-status';

export interface AssignMembershipData {
  memberId: number;
  memberName: string;
  memberships: MembershipResponse[];
  canSetPrice: boolean;
}

/** Venta = membresía + cobro completo en la caja abierta. Cierra con la membresía creada (incluye el pago). */
@Component({
  selector: 'gf-assign-membership-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe, FormFieldComponent, ButtonComponent],
  host: { class: 'block max-h-[90vh] w-[calc(100vw-2rem)] max-w-md overflow-y-auto rounded-2xl bg-white p-6 shadow-xl' },
  template: `
    <h2 class="text-lg font-semibold text-neutral-900">{{ isRenewal ? 'Renovar membresía' : 'Vender membresía' }}</h2>
    <p class="mt-1 text-sm text-neutral-500">{{ data.memberName }}</p>

    @if (error()) {
      <p class="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">{{ error() }}</p>
    }

    @if (plans.isLoading() || cash.isLoading()) {
      <p class="mt-5 text-sm text-neutral-500">Cargando…</p>
    } @else if (cashClosed()) {
      <div class="mt-5 rounded-lg bg-amber-50 p-4 text-sm text-amber-900">
        La caja está cerrada. Ábrela para poder cobrar.
        <a routerLink="/caja" class="mt-2 block font-semibold underline" (click)="ref.close()">Ir a abrir caja →</a>
      </div>
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
      <div class="mt-5 space-y-5">
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

        <fieldset>
          <legend class="mb-1.5 text-sm font-medium text-neutral-700">Método de pago</legend>
          <div class="grid grid-cols-4 gap-2">
            @for (m of methods; track m) {
              <label class="flex cursor-pointer items-center justify-center rounded-lg px-2 py-2.5 text-sm font-medium ring-1 ring-neutral-200 has-[:checked]:bg-brand-50 has-[:checked]:ring-2 has-[:checked]:ring-brand-500">
                <input type="radio" name="method" class="sr-only" [value]="m" [formControl]="method" />
                {{ methodLabel[m] }}
              </label>
            }
          </div>
        </fieldset>

        @if (showReference()) {
          <gf-form-field label="N.º de operación" for="payRef" hint="Opcional, para ubicar el pago después">
            <input id="payRef" class="gf-input" maxlength="40" [formControl]="reference" />
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
      @if (!cashClosed()) {
        <button gfButton type="button" [loading]="saving()" [disabled]="!selectedPlan() || price.invalid" (click)="submit()">
          Cobrar {{ total() | currency }}
        </button>
      }
    </div>
  `,
})
export class AssignMembershipDialog {
  private readonly api = inject(Api);
  protected readonly ref = inject<DialogRef<MembershipResponse>>(DialogRef);
  protected readonly data = inject<AssignMembershipData>(DIALOG_DATA);

  /** Una clave por intento de venta: si la red reintenta o hay doble clic, el backend no cobra dos veces. */
  private readonly idempotencyKey = crypto.randomUUID();
  protected readonly today = todayIso();
  protected readonly isRenewal = this.data.memberships.some((m) => m.status === 'ACTIVE' || m.status === 'SCHEDULED');
  protected readonly methods = PAYMENT_METHODS;
  protected readonly methodLabel = METHOD_LABEL;

  protected readonly plans = resource({ loader: () => this.api.invoke(listPlans, { includeInactive: false }) });
  protected readonly cash = resource({ loader: () => this.api.invoke(getCurrentCash) });
  protected readonly cashClosed = computed(() => this.cash.hasValue() && !this.cash.value()?.current);

  protected readonly planId = new FormControl<number | null>(null, Validators.required);
  protected readonly price = new FormControl<number | null>(null, Validators.min(0));
  protected readonly method = new FormControl<PaymentMethod>('CASH', { nonNullable: true });
  protected readonly reference = new FormControl('', { nonNullable: true, validators: Validators.maxLength(40) });
  private readonly planIdValue = toSignal(this.planId.valueChanges, { initialValue: null });
  private readonly priceValue = toSignal(this.price.valueChanges, { initialValue: null });
  private readonly methodValue = toSignal(this.method.valueChanges, { initialValue: 'CASH' as PaymentMethod });

  protected readonly selectedPlan = computed(() => {
    const id = this.planIdValue();
    return this.plans.value()?.find((p) => p.id === Number(id)) ?? null;
  });
  protected readonly total = computed(() => {
    const custom = this.priceValue();
    return this.data.canSetPrice && custom !== null && `${custom}` !== '' ? Number(custom) : (this.selectedPlan()?.price ?? 0);
  });
  protected readonly showReference = computed(() => needsReference(this.methodValue()));
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
    if (!plan || this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    const customPrice = this.data.canSetPrice && this.total() !== plan.price;
    try {
      const created = await this.api.invoke(assignMembership, {
        memberId: this.data.memberId,
        body: {
          planId: plan.id,
          price: customPrice ? this.total() : undefined,
          paymentMethod: this.method.value,
          paymentReference: this.showReference() && this.reference.value ? this.reference.value : undefined,
          idempotencyKey: this.idempotencyKey,
        },
      });
      this.ref.close(created);
    } catch (err) {
      this.error.set(apiErrorMessage(err));
    } finally {
      this.saving.set(false);
    }
  }
}
