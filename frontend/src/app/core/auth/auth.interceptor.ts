import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from './auth.service';

/** Solo las rutas del panel necesitan el JWT; las públicas y las de auth van sin él. */
const RUTAS_CON_TOKEN = '/api/panel/';

/**
 * Añade el access token a las peticiones del panel. Si el backend responde 401 (token
 * caducado), renueva la sesión una vez y repite la petición. Si la renovación también
 * falla, la sesión ha terminado y se vuelve al login.
 */
export const authInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  if (!peticion.url.startsWith(RUTAS_CON_TOKEN)) {
    return siguiente(peticion);
  }

  const auth = inject(AuthService);

  return siguiente(conToken(peticion, auth.accessToken())).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }
      return auth.refrescar().pipe(
        catchError((errorRefresco: unknown) => {
          auth.cerrarSesionLocal();
          return throwError(() => errorRefresco);
        }),
        // Fuera del catchError anterior: un error de la petición repetida no vuelve a refrescar
        switchMap((tokenNuevo) => siguiente(conToken(peticion, tokenNuevo))),
      );
    }),
  );
};

function conToken<T>(peticion: HttpRequest<T>, token: string | null): HttpRequest<T> {
  return token ? peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : peticion;
}
