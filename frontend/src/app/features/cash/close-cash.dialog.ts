import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api } from '../../api/api';
import { closeCash } from '../../api/functions';
import { CashSessionDetailResponse } from '../../api/models';
import { apiErrorMessage } from '../../core/http/api-error';
import { ButtonComponent } from '../../shared/ui/button/button.component';
import { FormFieldComponent } from '../../shared/ui/form-field/form-field.component';
import { DIFFERENCE_CLASS, DIFFERENCE_TEXT, differenceKind, subtractMoney } from './payment-method';

/** Arqueo: se cuenta el efectivo del cajón y se ve en vivo si cuadra. Cierra con la caja cerrada. */
@Component({
  selector: 'gf-close-cash-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, CurrencyPipe, FormFieldComponent, ButtonComponent],
  host: { class: 'block w-[calc(100vw-2rem)] max-w-md rounded-2xl bg-white p-6 shadow-xl' },
  template: `
    <h2 class="text-lg font-semibold text-neutral-900">Cerrar caja</h2>
    <p class="mt-1 text-sm text-neutral-500">Cuenta el efectivo del cajón. Yape, Plin y tarjeta no entran en el arqueo.</p>

    @if (error()) {
      <p class="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">{{ error() }}</p>
    }

    @if (expected !== null) {
      <div class="mt-5 rounded-lg bg-neutral-50 p-4 text-sm">
        <div class="flex justify-between"><span class="text-neutral-600">Efectivo esperado</span><strong>{{ expected | currency }}</strong></div>
        @if (difference() !== null) {
          <div class="mt-1 flex justify-between">
            <span class="text-neutral-600">{{ diffText() }}</span>
            <strong [class]="diffClass()">{{ absDifference() | currency }}</strong>
          </div>
        }
      </div>
    }

    <div class="mt-4 space-y-4">
      <gf-form-field label="Efectivo contado (S/)" for="counted">
        <input id="counted" type="number" min="0" step="0.10" class="gf-input text-lg" [formControl]="counted" autofocus />
      </gf-form-field>
      <gf-form-field label="Notas" for="closeNotes" hint="Opcional: explica si hay diferencia">
        <textarea id="closeNotes" rows="2" class="gf-input" [formControl]="notes"></textarea>
      </gf-form-field>
    </div>

    <div class="mt-6 flex justify-end gap-2">
      <button gfButton type="button" variant="secondary" (click)="ref.close()">Volver</button>
      <button gfButton type="button" [loading]="saving()" [disabled]="counted.invalid" (click)="submit()">Cerrar caja</button>
    </div>
  `,
})
export class CloseCashDialog {
  private readonly api = inject(Api);
  protected readonly ref = inject<DialogRef<CashSessionDetailResponse>>(DialogRef);
  /** expectedCash null = arqueo a ciegas (recepción): se cuenta sin ver cuánto debería haber. */
  private readonly data = inject<{ expectedCash: number | null }>(DIALOG_DATA);

  protected readonly expected = this.data.expectedCash;
  protected readonly counted = new FormControl<number | null>(null, [Validators.required, Validators.min(0)]);
  protected readonly notes = new FormControl('', { nonNullable: true, validators: Validators.maxLength(500) });
  private readonly countedValue = toSignal(this.counted.valueChanges, { initialValue: null });

  protected readonly difference = computed(() => {
    const v = this.countedValue();
    if (this.expected === null || v === null || v === undefined || `${v}` === '') return null;
    return subtractMoney(Number(v), this.expected);
  });
  private readonly kind = computed(() => differenceKind(this.difference() ?? 0));
  protected readonly diffText = computed(() => DIFFERENCE_TEXT[this.kind()]);
  protected readonly diffClass = computed(() => DIFFERENCE_CLASS[this.kind()]);
  protected readonly absDifference = computed(() => Math.abs(this.difference() ?? 0));
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  async submit(): Promise<void> {
    if (this.counted.invalid || this.counted.value === null) return;
    this.saving.set(true);
    this.error.set(null);
    try {
      const closed = await this.api.invoke(closeCash, {
        body: { countedCash: Number(this.counted.value), notes: this.notes.value || undefined },
      });
      this.ref.close(closed);
    } catch (err) {
      this.error.set(apiErrorMessage(err));
    } finally {
      this.saving.set(false);
    }
  }
}
