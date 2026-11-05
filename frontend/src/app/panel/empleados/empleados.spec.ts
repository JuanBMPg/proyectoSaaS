import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Empleado, Recurso } from '../../core/api/modelos';
import { Empleados } from './empleados';

function recurso(id: string, nombre: string, activo = true): Recurso {
  return { id, nombre, tipo: 'PERSONA', activo, servicioIds: [] };
}

const MARTA_EMPLEADA: Empleado = {
  id: 'u1',
  nombre: 'Marta López',
  email: 'marta@example.com',
  activo: true,
  recurso: { id: 'marta', nombre: 'Marta' },
};

describe('Empleados', () => {
  let fixture: ComponentFixture<Empleados>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  async function abrir(empleados: Empleado[], recursos: Recurso[]): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [Empleados],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(Empleados);
    http = TestBed.inject(HttpTestingController);
    elemento = fixture.nativeElement;
    http.expectOne('/api/panel/empleados').flush(empleados);
    http.expectOne('/api/panel/recursos').flush(recursos);
    await fixture.whenStable();
  }

  afterEach(() => http.verify());

  function botonNuevo(): HTMLButtonElement {
    return [...elemento.querySelectorAll<HTMLButtonElement>('button')].find((b) =>
      b.textContent?.includes('Nuevo empleado'),
    )!;
  }

  it('lista los empleados con el recurso cuya agenda ven', async () => {
    await abrir([MARTA_EMPLEADA], [recurso('marta', 'Marta'), recurso('pedro', 'Pedro')]);

    const fila = elemento.querySelector('tr[mat-row]')!;
    expect(fila.textContent).toContain('Marta López');
    expect(fila.textContent).toContain('marta@example.com');
    expect(fila.textContent).toContain('Marta');
    expect(botonNuevo().disabled).toBe(false);
  });

  it('no deja dar de alta si todos los recursos activos ya tienen empleado', async () => {
    await abrir([MARTA_EMPLEADA], [recurso('marta', 'Marta'), recurso('pista', 'Pista', false)]);

    expect(botonNuevo().disabled).toBe(true);
  });

  it('sin recursos explica que primero hay que crearlos', async () => {
    await abrir([], []);

    expect(elemento.textContent).toContain('primero crea en Recursos');
  });
});
