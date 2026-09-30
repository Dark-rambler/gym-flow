import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage, apiFieldErrors } from '../../core/http/api-error';
import { fieldError } from '../../shared/forms/field-error';
import { ButtonComponent } from '../../shared/ui/button/button.component';
import { FormFieldComponent } from '../../shared/ui/form-field/form-field.component';
import { AuthCardComponent } from './auth-card.component';

@Component({
  selector: 'gf-register-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, AuthCardComponent, FormFieldComponent, ButtonComponent],
  template: `
    <gf-auth-card title="Registra tu gimnasio" subtitle="Crea tu cuenta de dueño en menos de un minuto">
      @if (error()) {
        <p class="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">{{ error() }}</p>
      }
      <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-4" novalidate>
        <gf-form-field label="Nombre del gimnasio" for="gymName" [error]="fieldError(form.controls.gymName, serverErrors()['gymName'])">
          <input id="gymName" class="gf-input" formControlName="gymName" placeholder="Ej. Gym Fuerza" />
        </gf-form-field>
        <gf-form-field label="Tu nombre" for="ownerName" [error]="fieldError(form.controls.ownerName, serverErrors()['ownerName'])">
          <input id="ownerName" autocomplete="name" class="gf-input" formControlName="ownerName" />
        </gf-form-field>
        <gf-form-field label="Email" for="email" [error]="fieldError(form.controls.email, serverErrors()['email'])">
          <input id="email" type="email" autocomplete="email" class="gf-input" formControlName="email" />
        </gf-form-field>
        <gf-form-field label="Contraseña" for="password" hint="Mínimo 8 caracteres"
                       [error]="fieldError(form.controls.password, serverErrors()['password'])">
          <input id="password" type="password" autocomplete="new-password" class="gf-input" formControlName="password" />
        </gf-form-field>
        <button gfButton type="submit" [block]="true" [loading]="loading()">Crear gimnasio</button>
      </form>
      <p footer>¿Ya tienes cuenta? <a routerLink="/login" class="font-semibold text-brand-700 hover:underline">Inicia sesión</a></p>
    </gf-auth-card>
  `,
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = inject(NonNullableFormBuilder).group({
    gymName: ['', [Validators.required, Validators.maxLength(120)]],
    ownerName: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly serverErrors = signal<Record<string, string>>({});
  protected readonly fieldError = fieldError;

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.serverErrors.set({});
    try {
      await this.auth.registerGym(this.form.getRawValue());
      await this.router.navigateByUrl('/');
    } catch (err) {
      this.serverErrors.set(apiFieldErrors(err));
      this.error.set(apiErrorMessage(err));
    } finally {
      this.loading.set(false);
    }
  }
}
