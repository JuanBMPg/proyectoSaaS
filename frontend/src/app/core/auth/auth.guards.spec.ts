import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  GuardResult,
  MaybeAsync,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { Observable, firstValueFrom, isObservable, of } from 'rxjs';

import { Rol } from '../api/modelos';
import { jwtFalso, respuestaToken } from '../../../testing/jwt-falso';
import { authGuard, invitadoGuard, propietarioGuard } from './auth.guards';
import { AuthService } from './auth.service';

describe('guards de autenticación', () => {
  const ruta = {} as ActivatedRouteSnapshot;
  const estado = { url: '/panel/agenda' } as RouterStateSnapshot;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function iniciarSesion(rol: Rol = 'PROPIETARIO'): void {
    TestBed.inject(AuthService)
      .login({ email: 'lola@example.com', password: 'secreta123' })
      .subscribe();
    http.expectOne('/api/auth/login').flush(respuestaToken(jwtFalso(rol)));
  }

  /** Los guards pueden devolver un valor o un Observable: se normaliza a Observable. */
  function ejecutar(guard: typeof authGuard): Observable<GuardResult> {
    const resultado = TestBed.runInInjectionContext(() =>
      guard(ruta, estado),
    ) as MaybeAsync<GuardResult>;
    return isObservable(resultado) ? resultado : of(resultado as GuardResult);
  }

  function url(resultado: GuardResult): string {
    return TestBed.inject(Router).serializeUrl(resultado as UrlTree);
  }

  function sinCookieDeSesion(): void {
    http
      .expectOne('/api/auth/refresh')
      .flush({ status: 401 }, { status: 401, statusText: 'Unauthorized' });
  }

  it('authGuard sin sesión redirige al login recordando la página pedida', async () => {
    const resultado = firstValueFrom(ejecutar(authGuard));
    sinCookieDeSesion();

    expect(url(await resultado)).toBe('/login?redirect=%2Fpanel%2Fagenda');
  });

  it('authGuard recupera la sesión con la cookie tras recargar la página', async () => {
    const resultado = firstValueFrom(ejecutar(authGuard));
    http.expectOne('/api/auth/refresh').flush(respuestaToken(jwtFalso()));

    expect(await resultado).toBe(true);
  });

  it('authGuard con sesión en memoria deja pasar sin llamar al servidor', async () => {
    iniciarSesion();

    expect(await firstValueFrom(ejecutar(authGuard))).toBe(true);
    http.expectNone('/api/auth/refresh');
  });

  it('invitadoGuard con sesión manda al panel', async () => {
    iniciarSesion();

    expect(url(await firstValueFrom(ejecutar(invitadoGuard)))).toBe('/panel');
  });

  it('invitadoGuard sin sesión deja pasar', async () => {
    const resultado = firstValueFrom(ejecutar(invitadoGuard));
    sinCookieDeSesion();

    expect(await resultado).toBe(true);
  });

  it('propietarioGuard deja pasar al propietario', async () => {
    iniciarSesion('PROPIETARIO');

    expect(await firstValueFrom(ejecutar(propietarioGuard))).toBe(true);
  });

  it('propietarioGuard devuelve al empleado al inicio del panel', async () => {
    iniciarSesion('EMPLEADO');

    expect(url(await firstValueFrom(ejecutar(propietarioGuard)))).toBe('/panel');
  });
});
