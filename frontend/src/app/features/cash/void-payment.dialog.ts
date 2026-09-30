import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api } from '../../api/api';
import { voidPayment } from '../../api/functions';
import { PaymentResponse } from '../../api/models';
import { apiErrorMessage } from '../../core/http/api-error';
import { ButtonComponent } from '../../shared/ui/button/button.component';
import { FormFieldComponent } from '../../shared/ui/form-field/form-field.component';
import { METHOD_LABEL } from './payment-method';

/** Anula un cobro (y cancela su membresía) pidiendo el motivo. Cierra con el pago anulado. */
@Component({
  selector: 'gf-void-payment-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, CurrencyPipe, FormFieldComponent, ButtonComponent],
  host: { class: 'block w-[calc(100vw-2rem)] max-w-md rounded-2xl bg-white p-6 shadow-xl' },
  template: `
    <h2 class="text-lg font-semibold text-neutral-900">Anular pago</h2>
    <p class="mt-1 text-sm text-neutral-600">
      {{ p.memberName }} · {{ p.planName }} · {{ methodLabel[p.method] }} {{ p.amount | currency }}
    </p>
    <p class="mt-3 rounded-lg bg-amber-50 p-3 text-sm text-amber-900">
      La membresía se cancelará y el monto dejará de contar en la caja. Si fue en efectivo, devuelve el dinero al socio.
    </p>
    @if (error()) {
      <p class="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">{{ error() }}</p>
    }
    <gf-form-field class="mt-4" label="Motivo" for="voidReason">
      <input id="voidReason" class="gf-input" [formControl]="reason" placeholder="Ej. se cobró al socio equivocado" />
    </gf-form-field>
    <div class="mt-6 flex justify-end gap-2">
      <button gfButton type="button" variant="secondary" (click)="ref.close()">Volver</button>
      <button gfButton type="button" variant="danger" [loading]="saving()" [disabled]="reason.invalid" (click)="submit()">Anular pago</button>
    </div>
  `,
})
export class VoidPaymentDialog {
  private readonly api = inject(Api);
  protected readonly ref = inject<DialogRef<PaymentResponse>>(DialogRef);
  protected readonly p = inject<PaymentResponse>(DIALOG_DATA);
  protected readonly methodLabel = METHOD_LABEL;
  protected readonly reason = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] });
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  async submit(): Promise<void> {
    if (this.reason.invalid) return;
    this.saving.set(true);
    this.error.set(null);
    try {
      this.ref.close(await this.api.invoke(voidPayment, { id: this.p.id, body: { reason: this.reason.value } }));
    } catch (err) {
      this.error.set(apiErrorMessage(err));
    } finally {
      this.saving.set(false);
    }
  }
}
