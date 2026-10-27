import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { respuestaToken } from '../../../testing/jwt-falso';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let cliente: HttpClient;
  let http: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    cliente = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => http.verify());

  function iniciarSesion(token: string): void {
    auth.login({ email: 'lola@example.com', password: 'secreta123' }).subscribe();
    http.expectOne('/api/auth/login').flush(respuestaToken(token));
  }

  const noAutorizado = { status: 401, statusText: 'Unauthorized' };

  it('añade el access token a las peticiones del panel', () => {
    iniciarSesion('token-1');

    cliente.get('/api/panel/negocio').subscribe();

    const peticion = http.expectOne('/api/panel/negocio');
    expect(peticion.request.headers.get('Authorization')).toBe('Bearer token-1');
    peticion.flush({});
  });

  it('no envía el token a las rutas públicas', () => {
    iniciarSesion('token-1');

    cliente.get('/api/publico/lola').subscribe();

    const peticion = http.expectOne('/api/publico/lola');
    expect(peticion.request.headers.has('Authorization')).toBe(false);
    peticion.flush({});
  });

  it('ante un 401 renueva la sesión y repite la petición con el token nuevo', () => {
    iniciarSesion('token-caducado');
    let respuesta: unknown;

    cliente.get('/api/panel/negocio').subscribe((r) => (respuesta = r));
    http.expectOne('/api/panel/negocio').flush(null, noAutorizado);
    http.expectOne('/api/auth/refresh').flush(respuestaToken('token-nuevo'));

    const repetida = http.expectOne('/api/panel/negocio');
    expect(repetida.request.headers.get('Authorization')).toBe('Bearer token-nuevo');
    repetida.flush({ nombre: 'Lola' });
    expect(respuesta).toEqual({ nombre: 'Lola' });
  });

  it('si fallan dos peticiones a la vez, solo renueva la sesión una vez', () => {
    iniciarSesion('token-caducado');

    cliente.get('/api/panel/negocio').subscribe();
    cliente.get('/api/panel/servicios').subscribe();
    http.expectOne('/api/panel/negocio').flush(null, noAutorizado);
    http.expectOne('/api/panel/servicios').flush(null, noAutorizado);

    http.expectOne('/api/auth/refresh').flush(respuestaToken('token-nuevo'));
    http.expectOne('/api/panel/negocio').flush({});
    http.expectOne('/api/panel/servicios').flush([]);
  });

  it('si la renovación falla, cierra la sesión y vuelve al login', () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    iniciarSesion('token-caducado');
    let error: unknown;

    cliente.get('/api/panel/negocio').subscribe({ error: (e) => (error = e) });
    http.expectOne('/api/panel/negocio').flush(null, noAutorizado);
    http.expectOne('/api/auth/refresh').flush(null, noAutorizado);

    expect(error).toBeTruthy();
    expect(auth.autenticado()).toBe(false);
    expect(navegar).toHaveBeenCalledWith(['/login']);
  });

  it('un 403 no intenta renovar la sesión', () => {
    iniciarSesion('token-empleado');
    let estado: number | undefined;

    cliente.get('/api/panel/negocio').subscribe({ error: (e) => (estado = e.status) });
    http.expectOne('/api/panel/negocio').flush(null, { status: 403, statusText: 'Forbidden' });

    expect(estado).toBe(403);
    http.expectNone('/api/auth/refresh');
  });
});
