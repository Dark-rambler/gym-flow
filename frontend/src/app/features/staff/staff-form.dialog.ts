import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api } from '../../api/api';
import { createStaff } from '../../api/functions';
import { StaffResponse } from '../../api/models';
import { Role } from '../../core/auth/auth.store';
import { apiErrorMessage, apiFieldErrors } from '../../core/http/api-error';
import { fieldError } from '../../shared/forms/field-error';
import { ButtonComponent } from '../../shared/ui/button/button.component';
import { FormFieldComponent } from '../../shared/ui/form-field/form-field.component';
import { ROLE_LABEL } from './staff-roles';

export interface StaffFormData {
  roles: Role[];
}

/** Alta de un usuario del staff. Cierra con el StaffResponse creado, o undefined si se cancela. */
@Component({
  selector: 'gf-staff-form-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, FormFieldComponent, ButtonComponent],
  host: { class: 'block w-[calc(100vw-2rem)] max-w-md rounded-2xl bg-white p-6 shadow-xl' },
  template: `
    <h2 class="text-lg font-semibold text-neutral-900">Nuevo miembro del staff</h2>
    <p class="mt-1 text-sm text-neutral-500">Comparte la contraseña inicial con la persona para que pueda entrar.</p>

    @if (error()) {
      <p class="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">{{ error() }}</p>
    }
    <form [formGroup]="form" (ngSubmit)="submit()" class="mt-5 space-y-4" novalidate>
      <gf-form-field label="Nombre completo" for="fullName" [error]="fieldError(form.controls.fullName, serverErrors()['fullName'])">
        <input id="fullName" class="gf-input" formControlName="fullName" />
      </gf-form-field>
      <gf-form-field label="Email" for="staffEmail" [error]="fieldError(form.controls.email, serverErrors()['email'])">
        <input id="staffEmail" type="email" class="gf-input" formControlName="email" autocomplete="off" />
      </gf-form-field>
      <gf-form-field label="Contraseña inicial" for="staffPassword" hint="Mínimo 8 caracteres"
                     [error]="fieldError(form.controls.password, serverErrors()['password'])">
        <input id="staffPassword" type="text" class="gf-input" formControlName="password" autocomplete="new-password" />
      </gf-form-field>
      <gf-form-field label="Rol" for="role">
        <select id="role" class="gf-input" formControlName="role">
          @for (role of data.roles; track role) {
            <option [value]="role">{{ roleLabel[role] }}</option>
          }
        </select>
      </gf-form-field>
      <div class="flex justify-end gap-2 pt-2">
        <button gfButton type="button" variant="secondary" (click)="dialogRef.close()">Cancelar</button>
        <button gfButton type="submit" [loading]="saving()">Crear</button>
      </div>
    </form>
  `,
})
export class StaffFormDialog {
  private readonly api = inject(Api);
  protected readonly dialogRef = inject<DialogRef<StaffResponse>>(DialogRef);
  protected readonly data = inject<StaffFormData>(DIALOG_DATA);

  protected readonly form = inject(NonNullableFormBuilder).group({
    fullName: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: [this.data.roles[this.data.roles.length - 1] as Role, Validators.required],
  });
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverErrors = signal<Record<string, string>>({});
  protected readonly fieldError = fieldError;
  protected readonly roleLabel = ROLE_LABEL;

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.serverErrors.set({});
    try {
      const created = await this.api.invoke(createStaff, { body: this.form.getRawValue() });
      this.dialogRef.close(created);
    } catch (err) {
      this.serverErrors.set(apiFieldErrors(err));
      this.error.set(apiErrorMessage(err));
    } finally {
      this.saving.set(false);
    }
  }
}
