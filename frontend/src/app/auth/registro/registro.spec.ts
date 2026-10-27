import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Registro } from './registro';

describe('Registro', () => {
  let fixture: ComponentFixture<Registro>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Registro],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Registro);
    http = TestBed.inject(HttpTestingController);
    elemento = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function escribir(control: string, valor: string): void {
    const input = elemento.querySelector<HTMLInputElement>(`[formControlName=${control}]`)!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  }

  function rellenarFormularioValido(): void {
    escribir('nombreNegocio', 'Peluquería Lola');
    escribir('slug', 'peluqueria-lola');
    escribir('nombre', 'Lola');
    escribir('email', 'lola@example.com');
    escribir('password', 'secreta123');
  }

  async function enviar(): Promise<void> {
    elemento.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    await fixture.whenStable();
  }

  it('rechaza un slug con mayúsculas o espacios sin llamar a la API', async () => {
    rellenarFormularioValido();
    escribir('slug', 'Peluquería Lola');

    await enviar();

    http.expectNone('/api/auth/registro');
    expect(elemento.textContent).toContain('minúsculas, números y guiones');
  });

  it('muestra arriba el conflicto si el slug ya existe', async () => {
    rellenarFormularioValido();

    await enviar();
    http
      .expectOne('/api/auth/registro')
      .flush(
        { status: 409, detail: 'Ya existe un negocio con ese slug' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(elemento.querySelector('[role=alert]')?.textContent).toContain(
      'Ya existe un negocio con ese slug',
    );
  });

  it('coloca los errores de validación del servidor bajo su campo', async () => {
    rellenarFormularioValido();

    await enviar();
    http
      .expectOne('/api/auth/registro')
      .flush(
        {
          status: 400,
          detail: 'La petición contiene campos no válidos',
          errores: { email: 'dominio no permitido' },
        },
        { status: 400, statusText: 'Bad Request' },
      );
    await fixture.whenStable();

    expect(elemento.querySelector('mat-error')?.textContent).toContain('dominio no permitido');
    expect(elemento.querySelector('[role=alert]')).toBeNull();
  });
});
