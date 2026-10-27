import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { FranjaHoraria, Recurso } from '../../core/api/modelos';
import { HorarioRecurso } from './horario-recurso';

const MARTA: Recurso = {
  id: 'r1',
  nombre: 'Marta',
  tipo: 'PERSONA',
  activo: true,
  servicioIds: [],
};

describe('HorarioRecurso', () => {
  let fixture: ComponentFixture<HorarioRecurso>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  async function abrir(franjas: FranjaHoraria[]): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [HorarioRecurso],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(HorarioRecurso);
    fixture.componentRef.setInput('id', 'r1');
    http = TestBed.inject(HttpTestingController);
    elemento = fixture.nativeElement;
    await fixture.whenStable();

    http.expectOne('/api/panel/recursos/r1/horario').flush({ franjas });
    http.expectOne('/api/panel/recursos').flush([MARTA]);
    await fixture.whenStable();
  }

  afterEach(() => http.verify());

  function seccion(dia: string): HTMLElement {
    return elemento.querySelector<HTMLElement>(`section[aria-label="${dia}"]`)!;
  }

  function horas(dia: string): string[] {
    return [...seccion(dia).querySelectorAll<HTMLInputElement>('input[type=time]')].map(
      (input) => input.value,
    );
  }

  function botonGuardar(): HTMLButtonElement {
    return elemento.querySelector<HTMLButtonElement>('button[type=submit]')!;
  }

  it('muestra el nombre del recurso y las franjas de cada día; los días sin franjas, cerrados', async () => {
    await abrir([
      { diaSemana: 1, horaInicio: '09:00', horaFin: '14:00' },
      { diaSemana: 1, horaInicio: '16:00', horaFin: '20:00' },
    ]);

    expect(elemento.querySelector('h1')?.textContent).toContain('Marta');
    expect(horas('Lunes')).toEqual(['09:00', '14:00', '16:00', '20:00']);
    expect(seccion('Martes').textContent).toContain('Cerrado');
  });

  it('no deja guardar hasta que hay cambios', async () => {
    await abrir([]);

    expect(botonGuardar().disabled).toBe(true);
  });

  it('copia el lunes de martes a viernes y guarda la semana como lista plana', async () => {
    await abrir([{ diaSemana: 1, horaInicio: '09:00', horaFin: '14:00' }]);

    const copiar = [...elemento.querySelectorAll<HTMLButtonElement>('button')].find((b) =>
      b.textContent?.includes('Copiar el lunes'),
    )!;
    copiar.click();
    await fixture.whenStable();

    expect(horas('Viernes')).toEqual(['09:00', '14:00']);
    expect(seccion('Sábado').textContent).toContain('Cerrado');

    botonGuardar().click();
    const peticion = http.expectOne('/api/panel/recursos/r1/horario');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body.franjas.map((f: FranjaHoraria) => f.diaSemana)).toEqual([
      1, 2, 3, 4, 5,
    ]);
    peticion.flush({ franjas: peticion.request.body.franjas });
  });

  it('marca el solape y no deja guardar', async () => {
    await abrir([{ diaSemana: 1, horaInicio: '09:00', horaFin: '14:00' }]);

    // Segunda franja del lunes que empieza dentro de la primera
    const anadir = seccion('Lunes').querySelector<HTMLButtonElement>(':scope > button')!;
    anadir.click();
    await fixture.whenStable();
    const inputs = seccion('Lunes').querySelectorAll<HTMLInputElement>('input[type=time]');
    inputs[2].value = '13:00';
    inputs[2].dispatchEvent(new Event('input'));
    await fixture.whenStable();

    expect(seccion('Lunes').textContent).toContain('Hay franjas que se solapan');
    expect(botonGuardar().disabled).toBe(true);
  });

  it('muestra el error del backend si el guardado falla', async () => {
    await abrir([]);
    seccion('Martes').querySelector<HTMLButtonElement>(':scope > button')!.click();
    await fixture.whenStable();

    botonGuardar().click();
    http
      .expectOne('/api/panel/recursos/r1/horario')
      .flush(
        { status: 409, detail: 'Hay franjas del horario que se solapan' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(elemento.querySelector('[role=alert]')?.textContent).toContain('se solapan');
  });
});
