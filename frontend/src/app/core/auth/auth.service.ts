import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, shareReplay, throwError } from 'rxjs';

import { AccessTokenResponse, LoginRequest, RegistroRequest, Rol } from '../api/modelos';

/**
 * Estado de la sesión del personal del negocio.
 *
 * - El access token se guarda solo en memoria (un signal), nunca en localStorage:
 *   no queda persistido en el navegador y caduca a los 15 minutos, así que un XSS
 *   tiene mucho menos que robar.
 * - El refresh token vive en una cookie httpOnly que gestiona el navegador; este servicio
 *   nunca lo ve. Al recargar la página la sesión se recupera llamando a /refresh.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly token = signal<string | null>(null);
  private refrescoEnCurso: Observable<string> | null = null;

  readonly accessToken = this.token.asReadonly();
  readonly autenticado = computed(() => this.token() !== null);
  /** Solo para decidir qué mostrar; quien autoriza de verdad es el backend. */
  readonly rol = computed<Rol | null>(() => {
    const token = this.token();
    return token ? leerClaims(token).rol : null;
  });

  registrar(datos: RegistroRequest): Observable<void> {
    return this.http
      .post<AccessTokenResponse>('/api/auth/registro', datos)
      .pipe(map((respuesta) => this.token.set(respuesta.accessToken)));
  }

  login(datos: LoginRequest): Observable<void> {
    return this.http
      .post<AccessTokenResponse>('/api/auth/login', datos)
      .pipe(map((respuesta) => this.token.set(respuesta.accessToken)));
  }

  /**
   * Pide un access token nuevo usando la cookie del refresh token.
   * Si varias peticiones lo piden a la vez se comparte una sola llamada: el backend rota
   * el refresh token en cada uso, así que dos llamadas simultáneas harían fallar la segunda.
   */
  refrescar(): Observable<string> {
    if (!this.refrescoEnCurso) {
      this.refrescoEnCurso = this.http.post<AccessTokenResponse>('/api/auth/refresh', null).pipe(
        map((respuesta) => {
          this.token.set(respuesta.accessToken);
          return respuesta.accessToken;
        }),
        catchError((error: unknown) => {
          this.token.set(null);
          return throwError(() => error);
        }),
        finalize(() => (this.refrescoEnCurso = null)),
        shareReplay(1),
      );
    }
    return this.refrescoEnCurso;
  }

  /** Al arrancar la aplicación: si hay una cookie de sesión válida, se recupera la sesión. */
  restaurarSesion(): Observable<void> {
    return this.refrescar().pipe(
      map(() => undefined),
      catchError(() => of(undefined)),
    );
  }

  /** Revoca el refresh token en el servidor y, pase lo que pase, limpia la sesión local. */
  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', null).pipe(
      catchError(() => of(undefined)),
      map(() => undefined),
      finalize(() => this.cerrarSesionLocal()),
    );
  }

  /** Olvida el access token y vuelve al login (p. ej. cuando la sesión ha caducado). */
  cerrarSesionLocal(): void {
    this.token.set(null);
    void this.router.navigate(['/login']);
  }
}

interface ClaimsToken {
  sub: string;
  negocioId: string;
  rol: Rol;
}

/** Lee el payload del JWT (Base64URL) sin verificar la firma: eso lo hace el backend. */
function leerClaims(token: string): ClaimsToken {
  const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
  return JSON.parse(atob(payload)) as ClaimsToken;
}
