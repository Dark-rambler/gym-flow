import { AbstractControl } from '@angular/forms';

/**
 * Mensaje de error de un control (solo si ya fue tocado), priorizando el error del servidor para ese campo.
 */
export function fieldError(control: AbstractControl, serverError?: string | null): string | null {
  if (serverError) {
    return serverError;
  }
  if (!control.errors || !(control.touched || control.dirty)) {
    return null;
  }
  const e = control.errors;
  if (e['required']) return 'Este campo es obligatorio';
  if (e['email']) return 'Email inválido';
  if (e['minlength']) return `Mínimo ${e['minlength'].requiredLength} caracteres`;
  if (e['maxlength']) return `Máximo ${e['maxlength'].requiredLength} caracteres`;
  return 'Valor inválido';
}
