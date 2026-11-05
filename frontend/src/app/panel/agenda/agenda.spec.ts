import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Negocio, ReservaPanel } from '../../core/api/modelos';
import { AuthService } from '../../core/auth/auth.service';
import { Agenda } from './agenda';

const NEGOCIO = {
  id: 'n1',
  nombre: 'Peluquería Lola',
  slug: 'peluqueria-lola',
  zonaHoraria: 'Europe/Madrid',
} as Negocio;

const RESERVA: ReservaPanel = {
  id: 'r1',
  estado: 'CONFIRMADA',
  inicio: new Date().toISOString(),
  fin: new Date(Date.now() + 30 * 60_000).toISOString(),
  finBloqueo: new Date(Date.now() + 30 * 60_000).toISOString(),
  notas: null,
  servicio: { id: 's1', nombre: 'Corte', precioCentimos: 1500 },
  recurso: { id: 'm', nombre: 'Marta' },
  cliente: { nombre: 'Ana', email: 'ana@example.com', telefono: null },
};

/** Prueba de humo: FullCalendar se monta en jsdom y pide las reservas del rango visible. */
describe('Agenda', () => {
  let fixture: ComponentFixture<Agenda>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Agenda],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: { rol: () => 'EMPLEADO' } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Agenda);
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  it('carga el negocio y pide las reservas del rango visible en UTC', async () => {
    http.expectOne('/api/panel/negocio').flush(NEGOCIO);
    await fixture.whenStable();

    const peticion = http.expectOne((p) => p.url === '/api/panel/reservas');
    expect(peticion.request.params.get('desde')).toMatch(/Z$/);
    expect(peticion.request.params.get('hasta')).toMatch(/Z$/);
    // Un empleado no filtra: el backend ya le limita a su recurso
    expect(peticion.request.params.has('recursoId')).toBe(false);
    peticion.flush([RESERVA]);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('full-calendar')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Ana · Corte');
  });
});
