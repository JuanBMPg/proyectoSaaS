import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { Servicio } from '../../core/api/modelos';
import { ServicioDialogo } from './servicio-dialogo';

const CORTE: Servicio = {
  id: 'servicio-1',
  nombre: 'Corte',
  descripcion: null,
  duracionMin: 30,
  margenMin: 10,
  precioCentimos: 1500,
  activo: true,
};

describe('ServicioDialogo', () => {
  let fixture: ComponentFixture<ServicioDialogo>;
  let http: HttpTestingController;
  let cerrar: ReturnType<typeof vi.fn>;

  async function abrir(servicio: Servicio | null): Promise<HTMLElement> {
    cerrar = vi.fn();
    await TestBed.configureTestingModule({
      imports: [ServicioDialogo],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MAT_DIALOG_DATA, useValue: servicio },
        { provide: MatDialogRef, useValue: { close: cerrar } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ServicioDialogo);
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
    return fixture.nativeElement;
  }

  afterEach(() => http.verify());

  function escribir(elemento: HTMLElement, control: string, valor: string): void {
    const input = elemento.querySelector<HTMLInputElement>(`[formControlName=${control}]`)!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  }

  async function guardar(elemento: HTMLElement): Promise<void> {
    elemento.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    await fixture.whenStable();
  }

  it('crea un servicio enviando el precio en céntimos y se cierra con el resultado', async () => {
    const elemento = await abrir(null);
    escribir(elemento, 'nombre', 'Tinte');
    escribir(elemento, 'duracionMin', '60');
    escribir(elemento, 'precioEuros', '19.99');

    await guardar(elemento);
    const peticion = http.expectOne('/api/panel/servicios');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({
      nombre: 'Tinte',
      descripcion: null,
      duracionMin: 60,
      margenMin: 0,
      precioCentimos: 1999,
    });
    peticion.flush({ ...CORTE, id: 'servicio-2', nombre: 'Tinte' });

    expect(cerrar).toHaveBeenCalledWith(expect.objectContaining({ nombre: 'Tinte' }));
  });

  it('al editar muestra el precio en euros y hace PUT al servicio', async () => {
    const elemento = await abrir(CORTE);
    const precio = elemento.querySelector<HTMLInputElement>('[formControlName=precioEuros]')!;
    expect(precio.value).toBe('15');

    await guardar(elemento);
    const peticion = http.expectOne('/api/panel/servicios/servicio-1');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body.precioCentimos).toBe(1500);
    peticion.flush(CORTE);
  });

  it('no envía nada si la duración no es válida', async () => {
    const elemento = await abrir(null);
    escribir(elemento, 'nombre', 'Tinte');
    escribir(elemento, 'duracionMin', '0');

    await guardar(elemento);

    http.expectNone('/api/panel/servicios');
    expect(cerrar).not.toHaveBeenCalled();
  });

  it('muestra el error del backend y no se cierra', async () => {
    const elemento = await abrir(null);
    escribir(elemento, 'nombre', 'Tinte');

    await guardar(elemento);
    http
      .expectOne('/api/panel/servicios')
      .flush(
        { status: 400, detail: 'La petición contiene campos no válidos' },
        { status: 400, statusText: 'Bad Request' },
      );
    await fixture.whenStable();

    expect(elemento.querySelector('[role=alert]')?.textContent).toContain('campos no válidos');
    expect(cerrar).not.toHaveBeenCalled();
  });
});
