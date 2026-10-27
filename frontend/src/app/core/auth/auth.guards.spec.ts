import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';

import { Rol } from '../api/modelos';
import { jwtFalso, respuestaToken } from '../../../testing/jwt-falso';
import { authGuard, invitadoGuard, propietarioGuard } from './auth.guards';
import { AuthService } from './auth.service';

describe('guards de autenticación', () => {
  const ruta = {} as ActivatedRouteSnapshot;
  const estado = { url: '/panel/agenda' } as RouterStateSnapshot;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
  });

  function iniciarSesion(rol: Rol = 'PROPIETARIO'): void {
    TestBed.inject(AuthService)
      .login({ email: 'lola@example.com', password: 'secreta123' })
      .subscribe();
    TestBed.inject(HttpTestingController)
      .expectOne('/api/auth/login')
      .flush(respuestaToken(jwtFalso(rol)));
  }

  function ejecutar(guard: typeof authGuard) {
    return TestBed.runInInjectionContext(() => guard(ruta, estado));
  }

  it('authGuard sin sesión redirige al login recordando la página pedida', () => {
    const resultado = ejecutar(authGuard) as UrlTree;

    expect(TestBed.inject(Router).serializeUrl(resultado)).toBe(
      '/login?redirect=%2Fpanel%2Fagenda',
    );
  });

  it('authGuard con sesión deja pasar', () => {
    iniciarSesion();

    expect(ejecutar(authGuard)).toBe(true);
  });

  it('invitadoGuard con sesión manda al panel', () => {
    iniciarSesion();

    const resultado = ejecutar(invitadoGuard) as UrlTree;

    expect(TestBed.inject(Router).serializeUrl(resultado)).toBe('/panel');
  });

  it('invitadoGuard sin sesión deja pasar', () => {
    expect(ejecutar(invitadoGuard)).toBe(true);
  });

  it('propietarioGuard deja pasar al propietario', () => {
    iniciarSesion('PROPIETARIO');

    expect(ejecutar(propietarioGuard)).toBe(true);
  });

  it('propietarioGuard devuelve al empleado al inicio del panel', () => {
    iniciarSesion('EMPLEADO');

    const resultado = ejecutar(propietarioGuard) as UrlTree;

    expect(TestBed.inject(Router).serializeUrl(resultado)).toBe('/panel');
  });
});
