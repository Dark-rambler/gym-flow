import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api } from '../../api/api';
import { createMember, updateMember } from '../../api/functions';
import { MemberDetailResponse, MemberRequest } from '../../api/models';
import { apiErrorMessage, apiFieldErrors } from '../../core/http/api-error';
import { fieldError } from '../../shared/forms/field-error';
import { ButtonComponent } from '../../shared/ui/button/button.component';
import { FormFieldComponent } from '../../shared/ui/form-field/form-field.component';

/** Alta (sin data.member) o edición de un socio. Cierra con la ficha guardada. */
@Component({
  selector: 'gf-member-form-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, FormFieldComponent, ButtonComponent],
  host: { class: 'block max-h-[90vh] w-[calc(100vw-2rem)] max-w-lg overflow-y-auto rounded-2xl bg-white p-6 shadow-xl' },
  template: `
    <h2 class="text-lg font-semibold text-neutral-900">{{ data.member ? 'Editar socio' : 'Nuevo socio' }}</h2>
    @if (error()) {
      <p class="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">{{ error() }}</p>
    }
    <form [formGroup]="form" (ngSubmit)="submit()" class="mt-5 grid gap-4 sm:grid-cols-2" novalidate>
      <gf-form-field class="sm:col-span-2" label="Nombre completo" for="mFullName"
                     [error]="fieldError(form.controls.fullName, serverErrors()['fullName'])">
        <input id="mFullName" class="gf-input" formControlName="fullName" autocomplete="off" />
      </gf-form-field>
      <gf-form-field label="DNI" for="mDni" [error]="fieldError(form.controls.dni, serverErrors()['dni'])">
        <input id="mDni" class="gf-input" formControlName="dni" inputmode="numeric" autocomplete="off" />
      </gf-form-field>
      <gf-form-field label="Teléfono" for="mPhone" [error]="fieldError(form.controls.phone, serverErrors()['phone'])">
        <input id="mPhone" type="tel" class="gf-input" formControlName="phone" placeholder="Opcional" />
      </gf-form-field>
      <gf-form-field label="Email" for="mEmail" [error]="fieldError(form.controls.email, serverErrors()['email'])">
        <input id="mEmail" type="email" class="gf-input" formControlName="email" placeholder="Opcional" />
      </gf-form-field>
      <gf-form-field label="Fecha de nacimiento" for="mBirth" [error]="fieldError(form.controls.birthDate, serverErrors()['birthDate'])">
        <input id="mBirth" type="date" class="gf-input" formControlName="birthDate" />
      </gf-form-field>
      <gf-form-field class="sm:col-span-2" label="Notas" for="mNotes" [error]="fieldError(form.controls.notes, serverErrors()['notes'])">
        <textarea id="mNotes" rows="2" class="gf-input" formControlName="notes" placeholder="Lesiones, objetivos, etc. (opcional)"></textarea>
      </gf-form-field>
      <div class="flex justify-end gap-2 pt-2 sm:col-span-2">
        <button gfButton type="button" variant="secondary" (click)="ref.close()">Cancelar</button>
        <button gfButton type="submit" [loading]="saving()">{{ data.member ? 'Guardar' : 'Registrar socio' }}</button>
      </div>
    </form>
  `,
})
export class MemberFormDialog {
  private readonly api = inject(Api);
  protected readonly ref = inject<DialogRef<MemberDetailResponse>>(DialogRef);
  protected readonly data = inject<{ member?: MemberDetailResponse }>(DIALOG_DATA);

  private readonly m = this.data.member;
  protected readonly form = inject(NonNullableFormBuilder).group({
    fullName: [this.m?.fullName ?? '', [Validators.required, Validators.maxLength(120)]],
    dni: [this.m?.dni ?? '', [Validators.required, Validators.pattern(/^\s*[A-Za-z0-9]{6,12}\s*$/)]],
    phone: [this.m?.phone ?? '', [Validators.maxLength(20), Validators.pattern(/^[0-9+()\s-]*$/)]],
    email: [this.m?.email ?? '', [Validators.email, Validators.maxLength(160)]],
    birthDate: [this.m?.birthDate ?? ''],
    notes: [this.m?.notes ?? '', Validators.maxLength(500)],
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
    const v = this.form.getRawValue();
    const body: MemberRequest = {
      fullName: v.fullName,
      dni: v.dni,
      phone: v.phone || undefined,
      email: v.email || undefined,
      birthDate: v.birthDate || undefined,
      notes: v.notes || undefined,
    };
    try {
      const saved = this.m
        ? await this.api.invoke(updateMember, { id: this.m.id, body })
        : await this.api.invoke(createMember, { body });
      this.ref.close(saved);
    } catch (err) {
      this.serverErrors.set(apiFieldErrors(err));
      this.error.set(apiErrorMessage(err));
    } finally {
      this.saving.set(false);
    }
  }
}
