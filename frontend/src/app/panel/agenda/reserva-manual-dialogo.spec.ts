import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { NegocioPublico } from '../../core/api/modelos';
import { ReservaManualDialogo } from './reserva-manual-dialogo';

const LOLA: NegocioPublico = {
  nombre: 'Peluquería Lola',
  slug: 'peluqueria-lola',
  zonaHoraria: 'Europe/Madrid',
  direccion: null,
  telefono: null,
  etiquetaRecurso: 'Profesional',
  antelacionMaximaDias: 30,
  servicios: [
    {
      id: 'corte',
      nombre: 'Corte',
      descripcion: null,
      duracionMin: 30,
      precioCentimos: 1500,
      recursoIds: ['marta'],
    },
  ],
  recursos: [{ id: 'marta', nombre: 'Marta' }],
};

describe('ReservaManualDialogo', () => {
  let fixture: ComponentFixture<ReservaManualDialogo>;
  let http: HttpTestingController;
  let componente: ReservaManualDialogo;
  let cerrar: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    cerrar = vi.fn();
    await TestBed.configureTestingModule({
      imports: [ReservaManualDialogo],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: MAT_DIALOG_DATA,
          useValue: { slug: 'peluqueria-lola', zona: 'Europe/Madrid', fecha: '2026-11-09' },
        },
        { provide: MatDialogRef, useValue: { close: cerrar } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ReservaManualDialogo);
    componente = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
    http.expectOne('/api/publico/peluqueria-lola').flush(LOLA);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  /** Rellena el formulario como lo haría el usuario (a través del FormGroup del componente). */
  function rellenar(hora: string): void {
    const formulario = (componente as unknown as { formulario: { patchValue(v: object): void } })
      .formulario;
    formulario.patchValue({ servicioId: 'corte' });
    formulario.patchValue({
      recursoId: 'marta',
      hora,
      nombre: ' Ana ',
      email: 'ana@example.com',
    });
  }

  function guardar(): void {
    fixture.nativeElement.querySelector('button[type=submit]').click();
  }

  it('envía la reserva con la hora del negocio convertida a UTC', async () => {
    rellenar('10:00');

    guardar();

    const peticion = http.expectOne('/api/panel/reservas');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({
      servicioId: 'corte',
      recursoId: 'marta',
      inicio: '2026-11-09T09:00:00Z',
      nombre: 'Ana',
      email: 'ana@example.com',
      telefono: null,
      notas: null,
    });
    peticion.flush({ id: 'nueva' });
    expect(cerrar).toHaveBeenCalledWith({ id: 'nueva' });
  });

  it('si se solapa con otra reserva lo explica y no se cierra', async () => {
    rellenar('10:00');

    guardar();
    http
      .expectOne('/api/panel/reservas')
      .flush(
        { status: 409, detail: 'Ese hueco acaba de ocuparse' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('[role=alert]')?.textContent).toContain(
      'se solapa con otra reserva',
    );
    expect(cerrar).not.toHaveBeenCalled();
  });

  it('no envía nada si falta el cliente', async () => {
    guardar();

    http.expectNone('/api/panel/reservas');
  });
});
