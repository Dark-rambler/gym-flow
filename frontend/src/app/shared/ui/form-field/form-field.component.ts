import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Etiqueta + control proyectado + mensaje de error.
 * El error lo calcula el padre (ver fieldError en shared/forms) para que se actualice con OnPush.
 *
 * <gf-form-field label="Email" [error]="fieldError(form.controls.email)" for="email">
 *   <input id="email" class="gf-input" formControlName="email" />
 * </gf-form-field>
 */
@Component({
  selector: 'gf-form-field',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <label [attr.for]="for()" class="mb-1.5 block text-sm font-medium text-neutral-700">{{ label() }}</label>
    <ng-content />
    @if (error()) {
      <p class="mt-1.5 text-sm text-red-600" role="alert">{{ error() }}</p>
    } @else if (hint()) {
      <p class="mt-1.5 text-sm text-neutral-500">{{ hint() }}</p>
    }
  `,
})
export class FormFieldComponent {
  readonly label = input.required<string>();
  readonly for = input<string>();
  readonly error = input<string | null>(null);
  readonly hint = input<string>();
}
