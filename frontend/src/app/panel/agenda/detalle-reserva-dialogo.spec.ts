import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeEs from '@angular/common/locales/es';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { ReservaPanel } from '../../core/api/modelos';
import { DetalleReservaDialogo } from './detalle-reserva-dialogo';

registerLocaleData(localeEs);

const RESERVA: ReservaPanel = {
  id: 'r1',
  estado: 'CONFIRMADA',
  inicio: '2026-11-09T09:00:00Z',
  fin: '2026-11-09T09:30:00Z',
  finBloqueo: '2026-11-09T09:40:00Z',
  notas: 'Trae foto del corte',
  servicio: { id: 's1', nombre: 'Corte', precioCentimos: 1500 },
  recurso: { id: 'm', nombre: 'Marta' },
  cliente: { nombre: 'Ana', email: 'ana@example.com', telefono: '600111222' },
};

describe('DetalleReservaDialogo', () => {
  let fixture: ComponentFixture<DetalleReservaDialogo>;
  let http: HttpTestingController;
  let elemento: HTMLElement;
  let cerrar: ReturnType<typeof vi.fn>;

  async function abrir(reserva: ReservaPanel): Promise<void> {
    cerrar = vi.fn();
    await TestBed.configureTestingModule({
      imports: [DetalleReservaDialogo],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LOCALE_ID, useValue: 'es-ES' },
        { provide: MAT_DIALOG_DATA, useValue: { reserva, zona: 'Europe/Madrid' } },
        { provide: MatDialogRef, useValue: { close: cerrar } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(DetalleReservaDialogo);
    http = TestBed.inject(HttpTestingController);
    elemento = fixture.nativeElement;
    await fixture.whenStable();
  }

  afterEach(() => http.verify());

  function acciones(): string[] {
    return [...elemento.querySelectorAll('[data-accion]')].map((b) =>
      b.getAttribute('data-accion')!,
    );
  }

  it('muestra la cita en la hora del negocio con los datos de contacto del cliente', async () => {
    await abrir(RESERVA);

    const texto = elemento.textContent ?? '';
    expect(texto).toContain('lunes, 9 de noviembre, 10:00 – 10:30');
    expect(texto).toContain('ana@example.com');
    expect(texto).toContain('600111222');
    expect(texto).toContain('Trae foto del corte');
    expect(texto).toMatch(/15,00\s€/);
  });

  it('ofrece solo los cambios que admite su estado', async () => {
    await abrir(RESERVA);
    expect(acciones()).toEqual(['COMPLETADA', 'NO_PRESENTADO', 'CANCELADA']);
  });

  it('una reserva cancelada no ofrece cambios', async () => {
    await abrir({ ...RESERVA, estado: 'CANCELADA' });
    expect(acciones()).toEqual([]);
  });

  it('cambia el estado y se cierra con la reserva actualizada', async () => {
    await abrir(RESERVA);

    elemento.querySelector<HTMLButtonElement>('[data-accion=COMPLETADA]')!.click();
    const peticion = http.expectOne('/api/panel/reservas/r1/estado');
    expect(peticion.request.method).toBe('PATCH');
    expect(peticion.request.body).toEqual({ estado: 'COMPLETADA' });
    peticion.flush({ ...RESERVA, estado: 'COMPLETADA' });

    expect(cerrar).toHaveBeenCalledWith(expect.objectContaining({ estado: 'COMPLETADA' }));
  });

  it('si el backend rechaza el cambio muestra el motivo y no se cierra', async () => {
    await abrir(RESERVA);

    elemento.querySelector<HTMLButtonElement>('[data-accion=CANCELADA]')!.click();
    http
      .expectOne('/api/panel/reservas/r1/estado')
      .flush(
        { status: 422, detail: 'Una reserva completada no puede pasar a cancelada' },
        { status: 422, statusText: 'Unprocessable Content' },
      );
    await fixture.whenStable();

    expect(elemento.querySelector('[role=alert]')?.textContent).toContain(
      'no puede pasar a cancelada',
    );
    expect(cerrar).not.toHaveBeenCalled();
  });
});
