import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

/**
 * Protege el panel: sin sesión redirige al login recordando a dónde se quería ir.
 * Es solo comodidad para el usuario; la protección real son los 401/403 del backend.
 */
export const authGuard: CanActivateFn = (_ruta, estado) => {
  if (inject(AuthService).autenticado()) {
    return true;
  }
  return inject(Router).createUrlTree(['/login'], { queryParams: { redirect: estado.url } });
};

/** Login y registro no tienen sentido con la sesión iniciada: se va directo al panel. */
export const invitadoGuard: CanActivateFn = () => {
  return inject(AuthService).autenticado() ? inject(Router).createUrlTree(['/panel']) : true;
};
