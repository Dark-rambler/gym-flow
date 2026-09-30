import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api } from '../../api/api';
import { createPlan, updatePlan } from '../../api/functions';
import { PlanResponse } from '../../api/models';
import { apiErrorMessage, apiFieldErrors } from '../../core/http/api-error';
import { fieldError } from '../../shared/forms/field-error';
import { ButtonComponent } from '../../shared/ui/button/button.component';
import { FormFieldComponent } from '../../shared/ui/form-field/form-field.component';

const DURATION_PRESETS = [
  { label: '1 mes', days: 30 },
  { label: '3 meses', days: 90 },
  { label: '6 meses', days: 180 },
  { label: '1 año', days: 365 },
];

/** Alta (data.plan vacío) o edición de un plan. Cierra con el plan guardado. */
@Component({
  selector: 'gf-plan-form-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, FormFieldComponent, ButtonComponent],
  host: { class: 'block w-[calc(100vw-2rem)] max-w-md rounded-2xl bg-white p-6 shadow-xl' },
  template: `
    <h2 class="text-lg font-semibold text-neutral-900">{{ data.plan ? 'Editar plan' : 'Nuevo plan' }}</h2>
    @if (data.plan) {
      <p class="mt-1 text-sm text-neutral-500">Los cambios de precio no afectan a membresías ya vendidas.</p>
    }
    @if (error()) {
      <p class="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">{{ error() }}</p>
    }
    <form [formGroup]="form" (ngSubmit)="submit()" class="mt-5 space-y-4" novalidate>
      <gf-form-field label="Nombre" for="planName" [error]="fieldError(form.controls.name, serverErrors()['name'])">
        <input id="planName" class="gf-input" formControlName="name" placeholder="Ej. Mensual" />
      </gf-form-field>
      <gf-form-field label="Duración (días)" for="durationDays"
                     [error]="fieldError(form.controls.durationDays, serverErrors()['durationDays'])">
        <input id="durationDays" type="number" min="1" max="730" class="gf-input" formControlName="durationDays" />
        <div class="mt-2 flex flex-wrap gap-2">
          @for (preset of presets; track preset.days) {
            <button type="button" (click)="form.controls.durationDays.setValue(preset.days)"
                    class="rounded-full px-3 py-1 text-xs font-medium ring-1 ring-neutral-300 hover:bg-neutral-50"
                    [class.bg-brand-50]="form.controls.durationDays.value === preset.days"
                    [class.ring-brand-500]="form.controls.durationDays.value === preset.days">
              {{ preset.label }}
            </button>
          }
        </div>
      </gf-form-field>
      <gf-form-field label="Precio (S/)" for="price" [error]="fieldError(form.controls.price, serverErrors()['price'])">
        <input id="price" type="number" min="0" step="0.01" class="gf-input" formControlName="price" />
      </gf-form-field>
      <div class="flex justify-end gap-2 pt-2">
        <button gfButton type="button" variant="secondary" (click)="ref.close()">Cancelar</button>
        <button gfButton type="submit" [loading]="saving()">Guardar</button>
      </div>
    </form>
  `,
})
export class PlanFormDialog {
  private readonly api = inject(Api);
  protected readonly ref = inject<DialogRef<PlanResponse>>(DialogRef);
  protected readonly data = inject<{ plan?: PlanResponse }>(DIALOG_DATA);
  protected readonly presets = DURATION_PRESETS;

  protected readonly form = inject(NonNullableFormBuilder).group({
    name: [this.data.plan?.name ?? '', [Validators.required, Validators.maxLength(80)]],
    durationDays: [this.data.plan?.durationDays ?? 30, [Validators.required, Validators.min(1), Validators.max(730)]],
    price: [this.data.plan?.price ?? 0, [Validators.required, Validators.min(0)]],
  });
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverErrors = signal<Record<string, string>>({});
  protected readonly fieldError = fieldError;

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.serverErrors.set({});
    try {
      const body = this.form.getRawValue();
      const plan = this.data.plan
        ? await this.api.invoke(updatePlan, { id: this.data.plan.id, body: { ...body, active: this.data.plan.active } })
        : await this.api.invoke(createPlan, { body });
      this.ref.close(plan);
    } catch (err) {
      this.serverErrors.set(apiFieldErrors(err));
      this.error.set(apiErrorMessage(err));
    } finally {
      this.saving.set(false);
    }
  }
}
