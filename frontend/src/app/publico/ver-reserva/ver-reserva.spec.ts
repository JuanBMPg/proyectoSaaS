import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeEs from '@angular/common/locales/es';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ReservaPublica } from '../../core/api/modelos';
import { VerReserva } from './ver-reserva';

registerLocaleData(localeEs);

const RESERVA: ReservaPublica = {
  estado: 'CONFIRMADA',
  inicio: '2026-06-02T08:15:00Z',
  fin: '2026-06-02T08:45:00Z',
  clienteNombre: 'Juan',
  cancelable: true,
  // 24 h antes: lunes 1 de junio a las 10:15 en Madrid
  cancelableHasta: '2026-06-01T08:15:00Z',
  servicio: { nombre: 'Corte de pelo', duracionMin: 30, precioCentimos: 1500 },
  recurso: { nombre: 'Marta', etiqueta: 'Profesional' },
  negocio: {
    nombre: 'Peluquería Lola',
    slug: 'peluqueria-lola',
    zonaHoraria: 'Europe/Madrid',
    direccion: 'C/ Mayor 12',
    telefono: null,
  },
};

describe('VerReserva', () => {
  let fixture: ComponentFixture<VerReserva>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VerReserva],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: LOCALE_ID, useValue: 'es-ES' },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(VerReserva);
    fixture.componentRef.setInput('token', 'token-secreto');
    http = TestBed.inject(HttpTestingController);
    elemento = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  async function responder(reserva: ReservaPublica): Promise<void> {
    http.expectOne('/api/publico/reservas/token-secreto').flush(reserva);
    await fixture.whenStable();
  }

  it('muestra la cita en la hora del negocio con todos sus datos', async () => {
    await responder(RESERVA);

    const texto = elemento.textContent ?? '';
    expect(elemento.querySelector('[role=status]')?.textContent).toContain('Cita confirmada');
    expect(elemento.querySelector('h1')?.textContent).toContain('Corte de pelo');
    expect(texto).toContain('martes, 2 de junio');
    expect(texto).toContain('10:15 – 10:45');
    expect(texto).toContain('Profesional');
    expect(texto).toContain('Marta');
    expect(texto).toMatch(/15,00\s€/);
    expect(texto).toContain('C/ Mayor 12');
  });

  it('indica cuando la reserva espera la confirmación del negocio', async () => {
    await responder({ ...RESERVA, estado: 'PENDIENTE' });

    expect(elemento.querySelector('[role=status]')?.textContent).toContain(
      'Pendiente de confirmar por el negocio',
    );
  });

  it('enlaza de vuelta a la página del negocio', async () => {
    await responder(RESERVA);

    expect(elemento.querySelector('a')?.getAttribute('href')).toBe('/peluqueria-lola');
  });

  function boton(texto: string): HTMLButtonElement | undefined {
    return [...elemento.querySelectorAll<HTMLButtonElement>('button')].find((b) =>
      b.textContent?.includes(texto),
    );
  }

  it('se puede cancelar tras confirmarlo, y la cita pasa a cancelada', async () => {
    await responder(RESERVA);
    expect(elemento.textContent).toContain('hasta el lunes, 1 de junio a las 10:15');

    boton('Cancelar la cita')!.click();
    await fixture.whenStable();
    expect(elemento.textContent).toContain('¿Seguro que quieres cancelar la cita?');

    boton('Sí, cancelar la cita')!.click();
    const peticion = http.expectOne('/api/publico/reservas/token-secreto/cancelar');
    expect(peticion.request.method).toBe('POST');
    peticion.flush({ ...RESERVA, estado: 'CANCELADA', cancelable: false, cancelableHasta: null });
    await fixture.whenStable();

    expect(elemento.querySelector('[role=status]')?.textContent).toContain('Cita cancelada');
    expect(elemento.textContent).toContain('Tu cita se ha cancelado');
    expect(boton('Cancelar la cita')).toBeUndefined();
  });

  it('si el servidor rechaza la cancelación muestra su mensaje', async () => {
    await responder(RESERVA);
    boton('Cancelar la cita')!.click();
    await fixture.whenStable();

    boton('Sí, cancelar la cita')!.click();
    http
      .expectOne('/api/publico/reservas/token-secreto/cancelar')
      .flush(
        { detail: 'Las citas se pueden cancelar online hasta 24 horas antes.' },
        { status: 422, statusText: 'Unprocessable Content' },
      );
    await fixture.whenStable();

    expect(elemento.querySelector('[role=alert]')?.textContent).toContain('hasta 24 horas antes');
  });

  it('fuera de plazo no ofrece cancelar y explica a quién llamar', async () => {
    await responder({
      ...RESERVA,
      cancelable: false,
      negocio: { ...RESERVA.negocio, telefono: '600123123' },
    });

    expect(boton('Cancelar la cita')).toBeUndefined();
    expect(elemento.textContent).toContain(
      'El plazo para cancelar online terminó el lunes, 1 de junio',
    );
    expect(elemento.textContent).toContain('llama a Peluquería Lola al 600123123');
  });

  it('un enlace que no corresponde a ninguna reserva lo dice claramente', async () => {
    http
      .expectOne('/api/publico/reservas/token-secreto')
      .flush({ status: 404 }, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(elemento.textContent).toContain('Esta reserva no existe');
  });
});
