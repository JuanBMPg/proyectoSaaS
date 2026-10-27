import { HttpErrorResponse } from '@angular/common/http';

import { ProblemDetail } from '../core/api/modelos';

/** Mensaje legible para el usuario a partir de un error HTTP con formato Problem Details. */
export function mensajeDeError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'No se puede conectar con el servidor. Inténtalo de nuevo en unos segundos.';
    }
    const problema = error.error as ProblemDetail | null;
    if (problema?.detail) {
      return problema.detail;
    }
  }
  return 'Ha ocurrido un error inesperado.';
}

/** Errores por campo de una respuesta 400 ({ "errores": { "email": "..." } }). */
export function erroresDeCampo(error: unknown): Record<string, string> {
  if (error instanceof HttpErrorResponse && error.status === 400) {
    return (error.error as ProblemDetail | null)?.errores ?? {};
  }
  return {};
}
