import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Excepcion, Recurso } from '../../core/api/modelos';
import { Excepciones } from './excepciones';

const MARTA: Recurso = {
  id: 'marta',
  nombre: 'Marta',
  tipo: 'PERSONA',
  activo: true,
  servicioIds: [],
};

const VACACIONES: Excepcion = {
  id: 'e1',
  recurso: { id: 'marta', nombre: 'Marta' },
  fechaInicio: '2026-08-03',
  fechaFin: '2026-08-14',
  horaInicio: null,
  horaFin: null,
  motivo: 'Vacaciones',
};

const NOCHEBUENA: Excepcion = {
  id: 'e2',
  recurso: null,
  fechaInicio: '2026-12-24',
  fechaFin: '2026-12-24',
  horaInicio: '09:00',
  horaFin: '14:00',
  motivo: 'Nochebuena',
};

describe('Excepciones', () => {
  let fixture: ComponentFixture<Excepciones>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  async function abrir(excepciones: Excepcion[]): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [Excepciones],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(Excepciones);
    http = TestBed.inject(HttpTestingController);
    elemento = fixture.nativeElement;
    http.expectOne('/api/panel/excepciones').flush(excepciones);
    http.expectOne('/api/panel/recursos').flush([MARTA]);
    await fixture.whenStable();
  }

  afterEach(() => http.verify());

  function filas(): HTMLElement[] {
    return [...elemento.querySelectorAll<HTMLElement>('tr[mat-row]')];
  }

  it('lista cierres y horarios especiales con a quién afectan', async () => {
    await abrir([VACACIONES, NOCHEBUENA]);

    const [vacaciones, nochebuena] = filas();
    expect(vacaciones.textContent).toContain('lun, 3 ago – vie, 14 ago');
    expect(vacaciones.textContent).toContain('Marta');
    expect(vacaciones.textContent).toContain('Cerrado');
    expect(nochebuena.textContent).toContain('Todo el negocio');
    expect(nochebuena.textContent).toContain('Solo de 09:00 a 14:00');
  });

  it('sin excepciones explica para qué sirven', async () => {
    await abrir([]);

    expect(elemento.textContent).toContain('No hay excepciones próximas');
  });

  it('al borrar una la quita de la lista', async () => {
    await abrir([VACACIONES, NOCHEBUENA]);

    elemento.querySelector<HTMLButtonElement>('button[aria-label="Borrar Vacaciones"]')!.click();
    const peticion = http.expectOne('/api/panel/excepciones/e1');
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush(null);
    await fixture.whenStable();

    expect(filas()).toHaveLength(1);
    expect(filas()[0].textContent).toContain('Nochebuena');
  });
});
