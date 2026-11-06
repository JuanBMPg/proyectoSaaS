import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { ExcepcionCreada, Recurso } from '../../core/api/modelos';
import { DatosExcepcion, ExcepcionDialogo } from './excepcion-dialogo';

const MARTA: Recurso = {
  id: 'marta',
  nombre: 'Marta',
  tipo: 'PERSONA',
  activo: true,
  servicioIds: [],
};

describe('ExcepcionDialogo', () => {
  let fixture: ComponentFixture<ExcepcionDialogo>;
  let http: HttpTestingController;
  let cerrar: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    cerrar = vi.fn();
    await TestBed.configureTestingModule({
      imports: [ExcepcionDialogo],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MAT_DIALOG_DATA, useValue: { recursos: [MARTA] } satisfies DatosExcepcion },
        { provide: MatDialogRef, useValue: { close: cerrar } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ExcepcionDialogo);
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  /** El formulario es protected: en el test se accede a él como lo haría la plantilla. */
  function formulario() {
    return (fixture.componentInstance as unknown as { formulario: ExcepcionDialogo['formulario'] })
      .formulario;
  }

  function guardar(): void {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
  }

  it('un cierre de todo el negocio se envía sin recurso ni horas', () => {
    formulario().patchValue({
      fechaInicio: new Date(2026, 11, 24),
      fechaFin: new Date(2026, 11, 24),
      motivo: ' Nochebuena ',
    });
    guardar();

    const peticion = http.expectOne('/api/panel/excepciones');
    expect(peticion.request.body).toEqual({
      recursoId: null,
      fechaInicio: '2026-12-24',
      fechaFin: '2026-12-24',
      horaInicio: null,
      horaFin: null,
      motivo: 'Nochebuena',
    });
    const respuesta = { reservasActivas: 0 } as ExcepcionCreada;
    peticion.flush(respuesta);
    expect(cerrar).toHaveBeenCalledWith(respuesta);
  });

  it('un horario especial de un recurso lleva sus horas', () => {
    formulario().patchValue({
      recursoId: 'marta',
      fechaInicio: new Date(2026, 7, 3),
      fechaFin: new Date(2026, 7, 5),
      tipo: 'especial',
      horaInicio: '10:00',
      horaFin: '14:00',
      motivo: 'Media jornada',
    });
    guardar();

    const peticion = http.expectOne('/api/panel/excepciones');
    expect(peticion.request.body).toMatchObject({
      recursoId: 'marta',
      fechaInicio: '2026-08-03',
      fechaFin: '2026-08-05',
      horaInicio: '10:00',
      horaFin: '14:00',
    });
    peticion.flush({ reservasActivas: 0 });
  });

  it('no envía un horario especial sin horas o con las horas invertidas', () => {
    formulario().patchValue({
      fechaInicio: new Date(2026, 7, 3),
      fechaFin: new Date(2026, 7, 3),
      tipo: 'especial',
      motivo: 'Feria',
    });
    guardar();
    expect(formulario().hasError('horasIncompletas')).toBe(true);

    formulario().patchValue({ horaInicio: '14:00', horaFin: '10:00' });
    guardar();
    expect(formulario().hasError('horasInvertidas')).toBe(true);

    http.expectNone('/api/panel/excepciones');
  });
});
