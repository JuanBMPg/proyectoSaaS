import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeEs from '@angular/common/locales/es';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { NegocioPublico } from '../../core/api/modelos';
import { fechaIso } from '../../shared/fechas';
import { Reservar } from './reservar';

registerLocaleData(localeEs);

const LOLA: NegocioPublico = {
  nombre: 'Peluquería Lola',
  slug: 'peluqueria-lola',
  zonaHoraria: 'Europe/Madrid',
  direccion: 'C/ Mayor 12',
  telefono: null,
  etiquetaRecurso: 'Profesional',
  antelacionMaximaDias: 30,
  servicios: [
    {
      id: 'corte',
      nombre: 'Corte de pelo',
      descripcion: null,
      duracionMin: 30,
      precioCentimos: 1500,
      recursoIds: ['marta', 'pedro'],
    },
    {
      id: 'tinte',
      nombre: 'Tinte',
      descripcion: null,
      duracionMin: 60,
      precioCentimos: 3500,
      recursoIds: ['marta'],
    },
  ],
  recursos: [
    { id: 'marta', nombre: 'Marta' },
    { id: 'pedro', nombre: 'Pedro' },
  ],
};

describe('Reservar', () => {
  let fixture: ComponentFixture<Reservar>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Reservar],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: LOCALE_ID, useValue: 'es-ES' },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Reservar);
    fixture.componentRef.setInput('slug', 'peluqueria-lola');
    http = TestBed.inject(HttpTestingController);
    elemento = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  async function cargarNegocio(): Promise<void> {
    http.expectOne('/api/publico/peluqueria-lola').flush(LOLA);
    await fixture.whenStable();
  }

  function boton(texto: string): HTMLButtonElement {
    const encontrado = [...elemento.querySelectorAll<HTMLButtonElement>('button')].find((b) =>
      b.textContent?.includes(texto),
    );
    if (!encontrado) {
      throw new Error(`No hay ningún botón con el texto "${texto}"`);
    }
    return encontrado;
  }

  async function pulsar(texto: string): Promise<void> {
    boton(texto).click();
    await fixture.whenStable();
  }

  /** Pulsa el primer día disponible del calendario (hoy) y devuelve la petición de huecos. */
  async function elegirPrimerDia() {
    elemento
      .querySelector<HTMLButtonElement>('.mat-calendar-body-cell:not(.mat-calendar-body-disabled)')!
      .click();
    await fixture.whenStable();
    return http.expectOne((peticion) => peticion.url.endsWith('/disponibilidad'));
  }

  it('muestra el negocio y sus servicios con el precio en euros', async () => {
    await cargarNegocio();

    expect(elemento.querySelector('h1')?.textContent).toContain('Peluquería Lola');
    expect(boton('Corte de pelo').textContent).toMatch(/15,00\s€/);
    expect(boton('Tinte').textContent).toContain('60 min');
  });

  it('si el negocio no existe lo dice claramente', async () => {
    http
      .expectOne('/api/publico/peluqueria-lola')
      .flush({ status: 404 }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(elemento.textContent).toContain('Este negocio no existe');
  });

  it('al elegir un servicio ofrece solo los profesionales que lo prestan, con "Sin preferencia" marcado', async () => {
    await cargarNegocio();

    await pulsar('Tinte');

    expect(boton('Sin preferencia').getAttribute('aria-pressed')).toBe('true');
    expect(boton('Marta')).toBeTruthy();
    expect(elemento.textContent).not.toContain('Pedro');
  });

  it('pide los huecos del día elegido sin desfase de fecha y sin recurso si no hay preferencia', async () => {
    await cargarNegocio();
    await pulsar('Corte de pelo');

    const peticion = await elegirPrimerDia();

    expect(peticion.request.params.get('servicioId')).toBe('corte');
    expect(peticion.request.params.get('fecha')).toBe(fechaIso(new Date()));
    expect(peticion.request.params.has('recursoId')).toBe(false);
    peticion.flush({ fecha: fechaIso(new Date()), zonaHoraria: 'Europe/Madrid', huecos: [] });
  });

  it('muestra las horas en la zona del negocio, sin repetir, y resume la cita elegida', async () => {
    await cargarNegocio();
    await pulsar('Corte de pelo');
    const peticion = await elegirPrimerDia();

    peticion.flush({
      fecha: '2026-06-02',
      zonaHoraria: 'Europe/Madrid',
      huecos: [
        { recursoId: 'marta', inicio: '2026-06-02T08:00:00Z', fin: '2026-06-02T08:30:00Z' },
        { recursoId: 'pedro', inicio: '2026-06-02T08:00:00Z', fin: '2026-06-02T08:30:00Z' },
        { recursoId: 'pedro', inicio: '2026-06-02T14:30:00Z', fin: '2026-06-02T15:00:00Z' },
      ],
    });
    await fixture.whenStable();

    const horas = [...elemento.querySelectorAll('.hora')].map((h) => h.textContent?.trim());
    expect(horas).toEqual(['10:00', '16:30']);

    await pulsar('16:30');
    const resumen = elemento.querySelector('[aria-label="Resumen de tu cita"]')?.textContent;
    expect(resumen).toContain('Corte de pelo');
    expect(resumen).toContain('Pedro');
    expect(resumen).toContain('martes, 2 de junio');
    expect(resumen).toContain('16:30');
  });

  it('elegir un profesional vuelve a pedir los huecos solo de ese profesional', async () => {
    await cargarNegocio();
    await pulsar('Corte de pelo');
    (await elegirPrimerDia()).flush({ fecha: '', zonaHoraria: 'Europe/Madrid', huecos: [] });
    await fixture.whenStable();

    await pulsar('Marta');

    const peticion = http.expectOne((p) => p.url.endsWith('/disponibilidad'));
    expect(peticion.request.params.get('recursoId')).toBe('marta');
    peticion.flush({ fecha: '', zonaHoraria: 'Europe/Madrid', huecos: [] });
    await fixture.whenStable();
    expect(elemento.textContent).toContain('No quedan horas libres este día');
  });

  // --- Confirmar la reserva ---

  const HUECO_MARTA = {
    recursoId: 'marta',
    inicio: '2026-06-02T08:00:00Z',
    fin: '2026-06-02T08:30:00Z',
  };

  /** Servicio, día y la hora de las 10:00 elegidos; formulario abierto. */
  async function hastaElFormulario(): Promise<void> {
    await cargarNegocio();
    await pulsar('Corte de pelo');
    (await elegirPrimerDia()).flush({
      fecha: '2026-06-02',
      zonaHoraria: 'Europe/Madrid',
      huecos: [HUECO_MARTA],
    });
    await fixture.whenStable();
    await pulsar('10:00');
    await pulsar('Continuar');
  }

  function escribir(control: string, valor: string): void {
    const campo = elemento.querySelector<HTMLInputElement>(`[formControlName=${control}]`)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
  }

  it('confirma la reserva con el profesional de la hora elegida y lleva a su página', async () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await hastaElFormulario();
    escribir('nombre', '  Juan ');
    escribir('email', 'juan@example.com');

    await pulsar('Confirmar reserva');

    const peticion = http.expectOne('/api/publico/peluqueria-lola/reservas');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({
      servicioId: 'corte',
      recursoId: 'marta',
      inicio: '2026-06-02T08:00:00Z',
      nombre: 'Juan',
      email: 'juan@example.com',
      telefono: null,
      notas: null,
    });
    peticion.flush({ token: 'token-secreto', reserva: {} });

    expect(navegar).toHaveBeenCalledWith(['/reserva', 'token-secreto']);
  });

  it('no envía nada si faltan el nombre o el email', async () => {
    await hastaElFormulario();

    await pulsar('Confirmar reserva');

    http.expectNone('/api/publico/peluqueria-lola/reservas');
    expect(elemento.textContent).toContain('Escribe tu nombre');
  });

  it('si el hueco se acaba de ocupar avisa, recarga las horas y conserva los datos', async () => {
    await hastaElFormulario();
    escribir('nombre', 'Juan');
    escribir('email', 'juan@example.com');

    await pulsar('Confirmar reserva');
    http
      .expectOne('/api/publico/peluqueria-lola/reservas')
      .flush(
        { status: 409, detail: 'Ese hueco acaba de ocuparse' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(elemento.querySelector('[role=alert]')?.textContent).toContain(
      'Ese hueco acaba de ocuparse. Elige otra hora.',
    );
    // Las horas se vuelven a pedir y hay que elegir una nueva para poder confirmar
    http
      .expectOne((p) => p.url.endsWith('/disponibilidad'))
      .flush({ fecha: '2026-06-02', zonaHoraria: 'Europe/Madrid', huecos: [] });
    await fixture.whenStable();
    expect(boton('Confirmar reserva').disabled).toBe(true);
    expect(elemento.querySelector<HTMLInputElement>('[formControlName=nombre]')!.value).toBe(
      'Juan',
    );
  });
});
