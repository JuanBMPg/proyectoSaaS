import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';

import { AuthService } from './auth.service';

/**
 * Protege el panel: sin sesión redirige al login recordando a dónde se quería ir.
 * Es solo comodidad para el usuario; la protección real son los 401/403 del backend.
 */
export const authGuard: CanActivateFn = (_ruta, estado) => {
  const router = inject(Router);
  return inject(AuthService)
    .comprobarSesion()
    .pipe(
      map((haySesion) =>
        haySesion
          ? true
          : router.createUrlTree(['/login'], { queryParams: { redirect: estado.url } }),
      ),
    );
};

/** Login y registro no tienen sentido con la sesión iniciada: se va directo al panel. */
export const invitadoGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(AuthService)
    .comprobarSesion()
    .pipe(map((haySesion) => (haySesion ? router.createUrlTree(['/panel']) : true)));
};

/** Páginas solo para el propietario; un empleado vuelve al inicio del panel. */
export const propietarioGuard: CanActivateFn = () => {
  return inject(AuthService).rol() === 'PROPIETARIO'
    ? true
    : inject(Router).createUrlTree(['/panel']);
};
