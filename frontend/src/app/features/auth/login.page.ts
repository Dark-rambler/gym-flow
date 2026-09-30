import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage } from '../../core/http/api-error';
import { fieldError } from '../../shared/forms/field-error';
import { ButtonComponent } from '../../shared/ui/button/button.component';
import { FormFieldComponent } from '../../shared/ui/form-field/form-field.component';
import { AuthCardComponent } from './auth-card.component';

@Component({
  selector: 'gf-login-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, AuthCardComponent, FormFieldComponent, ButtonComponent],
  template: `
    <gf-auth-card title="Inicia sesión" subtitle="Accede al panel de tu gimnasio">
      @if (expirada()) {
        <p class="mb-4 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-800">Tu sesión expiró. Vuelve a iniciar sesión.</p>
      }
      @if (error()) {
        <p class="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">{{ error() }}</p>
      }
      <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-4" novalidate>
        <gf-form-field label="Email" for="email" [error]="fieldError(form.controls.email)">
          <input id="email" type="email" autocomplete="email" class="gf-input" formControlName="email" />
        </gf-form-field>
        <gf-form-field label="Contraseña" for="password" [error]="fieldError(form.controls.password)">
          <input id="password" type="password" autocomplete="current-password" class="gf-input" formControlName="password" />
        </gf-form-field>
        <button gfButton type="submit" [block]="true" [loading]="loading()">Entrar</button>
      </form>
      <p footer>¿Tu gimnasio aún no usa gymFlow? <a routerLink="/registro" class="font-semibold text-brand-700 hover:underline">Regístralo gratis</a></p>
    </gf-auth-card>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Query params (withComponentInputBinding) */
  readonly expirada = input<string>();
  readonly volver = input<string>();

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly fieldError = fieldError;

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    try {
      await this.auth.login(this.form.getRawValue());
      // solo rutas internas: evita open redirect con ?volver=https://...
      const target = this.volver()?.startsWith('/') && !this.volver()?.startsWith('//') ? this.volver()! : '/';
      await this.router.navigateByUrl(target);
    } catch (err) {
      this.error.set(apiErrorMessage(err));
    } finally {
      this.loading.set(false);
    }
  }
}
