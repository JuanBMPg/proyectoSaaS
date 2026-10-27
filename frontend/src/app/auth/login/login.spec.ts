import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { jwtFalso, respuestaToken } from '../../../testing/jwt-falso';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    http = TestBed.inject(HttpTestingController);
    elemento = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function escribir(selector: string, valor: string): void {
    const input = elemento.querySelector<HTMLInputElement>(selector)!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  }

  async function enviar(): Promise<void> {
    elemento.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    await fixture.whenStable();
  }

  it('no llama a la API si el formulario es inválido', async () => {
    await enviar();

    http.expectNone('/api/auth/login');
    expect(elemento.textContent).toContain('Escribe tu email');
  });

  it('con credenciales correctas inicia sesión y va al panel', async () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    escribir('input[type=email]', 'lola@example.com');
    escribir('input[type=password]', 'secreta123');

    await enviar();
    const peticion = http.expectOne('/api/auth/login');
    expect(peticion.request.body).toEqual({ email: 'lola@example.com', password: 'secreta123' });
    peticion.flush(respuestaToken(jwtFalso()));

    expect(navegar).toHaveBeenCalledWith('/panel');
  });

  it('muestra el mensaje del backend si las credenciales son incorrectas', async () => {
    escribir('input[type=email]', 'lola@example.com');
    escribir('input[type=password]', 'incorrecta');

    await enviar();
    http
      .expectOne('/api/auth/login')
      .flush(
        { status: 401, detail: 'Email o contraseña incorrectos' },
        { status: 401, statusText: 'Unauthorized' },
      );
    await fixture.whenStable();

    expect(elemento.querySelector('[role=alert]')?.textContent).toContain(
      'Email o contraseña incorrectos',
    );
  });
});
