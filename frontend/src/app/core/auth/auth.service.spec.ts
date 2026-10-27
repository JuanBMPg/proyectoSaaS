import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { jwtFalso, respuestaToken } from '../../../testing/jwt-falso';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let servicio: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    servicio = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('empieza sin sesión', () => {
    expect(servicio.autenticado()).toBe(false);
    expect(servicio.rol()).toBeNull();
  });

  it('el login guarda el access token en memoria y expone el rol del token', () => {
    servicio.login({ email: 'lola@example.com', password: 'secreta123' }).subscribe();

    const peticion = http.expectOne('/api/auth/login');
    expect(peticion.request.method).toBe('POST');
    peticion.flush(respuestaToken(jwtFalso('PROPIETARIO')));

    expect(servicio.autenticado()).toBe(true);
    expect(servicio.rol()).toBe('PROPIETARIO');
  });

  it('comparte una sola llamada a /refresh aunque se pida varias veces a la vez', () => {
    const recibidos: string[] = [];
    servicio.refrescar().subscribe((token) => recibidos.push(token));
    servicio.refrescar().subscribe((token) => recibidos.push(token));

    http.expectOne('/api/auth/refresh').flush(respuestaToken('token-nuevo'));

    expect(recibidos).toEqual(['token-nuevo', 'token-nuevo']);
  });

  it('comprobarSesion recupera la sesión con la cookie del refresh token', () => {
    let haySesion: boolean | undefined;
    servicio.comprobarSesion().subscribe((resultado) => (haySesion = resultado));

    http.expectOne('/api/auth/refresh').flush(respuestaToken(jwtFalso()));

    expect(haySesion).toBe(true);
    expect(servicio.autenticado()).toBe(true);
  });

  it('comprobarSesion responde false si no hay sesión y no vuelve a intentarlo', () => {
    let haySesion: boolean | undefined;
    servicio.comprobarSesion().subscribe((resultado) => (haySesion = resultado));
    http
      .expectOne('/api/auth/refresh')
      .flush({ status: 401 }, { status: 401, statusText: 'Unauthorized' });
    expect(haySesion).toBe(false);

    // Segunda comprobación: ya se sabe que no hay sesión, no se repite la llamada
    servicio.comprobarSesion().subscribe((resultado) => (haySesion = resultado));
    http.expectNone('/api/auth/refresh');
    expect(haySesion).toBe(false);
  });

  it('comprobarSesion no llama al servidor si ya hay token en memoria', () => {
    servicio.login({ email: 'lola@example.com', password: 'secreta123' }).subscribe();
    http.expectOne('/api/auth/login').flush(respuestaToken(jwtFalso()));

    let haySesion: boolean | undefined;
    servicio.comprobarSesion().subscribe((resultado) => (haySesion = resultado));

    http.expectNone('/api/auth/refresh');
    expect(haySesion).toBe(true);
  });

  it('el logout limpia la sesión y vuelve al login aunque el servidor falle', () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    servicio.login({ email: 'lola@example.com', password: 'secreta123' }).subscribe();
    http.expectOne('/api/auth/login').flush(respuestaToken(jwtFalso()));

    servicio.logout().subscribe();
    http
      .expectOne('/api/auth/logout')
      .flush(null, { status: 500, statusText: 'Internal Server Error' });

    expect(servicio.autenticado()).toBe(false);
    expect(navegar).toHaveBeenCalledWith(['/login']);
  });
});
