import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReservaPanel } from '../../core/api/modelos';
import { SolicitudesPendientes } from './solicitudes-pendientes';

function solicitud(id: string, cliente: string): ReservaPanel {
  return {
    id,
    estado: 'PENDIENTE',
    inicio: '2026-06-02T08:15:00Z',
    fin: '2026-06-02T08:45:00Z',
    finBloqueo: '2026-06-02T08:45:00Z',
    notas: null,
    servicio: { id: 's1', nombre: 'Corte', precioCentimos: 1500 },
    recurso: { id: 'marta', nombre: 'Marta' },
    cliente: { nombre: cliente, email: 'cliente@example.com', telefono: null },
  };
}

describe('SolicitudesPendientes', () => {
  let fixture: ComponentFixture<SolicitudesPendientes>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  async function abrir(solicitudes: ReservaPanel[]): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [SolicitudesPendientes],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(SolicitudesPendientes);
    http = TestBed.inject(HttpTestingController);
    elemento = fixture.nativeElement;
    http.expectOne('/api/panel/negocio').flush({ zonaHoraria: 'Europe/Madrid' });
    http.expectOne('/api/panel/reservas/pendientes').flush(solicitudes);
    await fixture.whenStable();
  }

  afterEach(() => http.verify());

  function botones(texto: string): HTMLButtonElement[] {
    return [...elemento.querySelectorAll<HTMLButtonElement>('button')].filter((b) =>
      b.textContent?.includes(texto),
    );
  }

  it('sin solicitudes no muestra nada', async () => {
    await abrir([]);

    expect(elemento.textContent?.trim()).toBe('');
  });

  it('lista las solicitudes con la hora del negocio', async () => {
    await abrir([solicitud('r1', 'Ana'), solicitud('r2', 'Luis')]);

    expect(elemento.querySelector('h2')?.textContent).toContain('Solicitudes por confirmar (2)');
    expect(elemento.textContent).toContain('Ana');
    expect(elemento.textContent).toContain('Corte con Marta');
    expect(elemento.textContent).toContain('2 de junio, 10:15');
  });

  it('confirmar o rechazar cambia el estado y quita la solicitud de la lista', async () => {
    await abrir([solicitud('r1', 'Ana'), solicitud('r2', 'Luis')]);

    botones('Confirmar')[0].click();
    const confirmar = http.expectOne('/api/panel/reservas/r1/estado');
    expect(confirmar.request.body).toEqual({ estado: 'CONFIRMADA' });
    confirmar.flush({ ...solicitud('r1', 'Ana'), estado: 'CONFIRMADA' });
    await fixture.whenStable();

    botones('Rechazar')[0].click();
    const rechazar = http.expectOne('/api/panel/reservas/r2/estado');
    expect(rechazar.request.body).toEqual({ estado: 'CANCELADA' });
    rechazar.flush({ ...solicitud('r2', 'Luis'), estado: 'CANCELADA' });
    await fixture.whenStable();

    expect(elemento.querySelectorAll('li')).toHaveLength(0);
  });
});
