import { HttpErrorResponse } from '@angular/common/http';

/** Cuerpo de error del backend: { status, message, timestamp, errors? } */
interface ApiErrorBody {
  status: number;
  message: string;
  errors?: Record<string, string>;
}

function body(err: unknown): ApiErrorBody | null {
  if (err instanceof HttpErrorResponse && err.error && typeof err.error === 'object' && 'message' in err.error) {
    return err.error as ApiErrorBody;
  }
  return null;
}

/** Mensaje para mostrar al usuario a partir de cualquier error de una llamada a la API. */
export function apiErrorMessage(err: unknown, fallback = 'Ocurrió un error inesperado'): string {
  if (err instanceof HttpErrorResponse && err.status === 0) {
    return 'No se pudo conectar con el servidor';
  }
  return body(err)?.message ?? fallback;
}

/** Errores de validación por campo (400), p. ej. { email: 'Email inválido' }. */
export function apiFieldErrors(err: unknown): Record<string, string> {
  return body(err)?.errors ?? {};
}
