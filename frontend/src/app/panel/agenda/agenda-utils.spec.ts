import { ReservaPanel } from '../../core/api/modelos';
import { aEventos, instanteEnZona, transicionesDesde } from './agenda-utils';

const RESERVA: ReservaPanel = {
  id: 'r1',
  estado: 'CONFIRMADA',
  inicio: '2026-11-09T09:00:00Z',
  fin: '2026-11-09T09:30:00Z',
  finBloqueo: '2026-11-09T09:40:00Z',
  notas: null,
  servicio: { id: 's1', nombre: 'Corte', precioCentimos: 1500 },
  recurso: { id: 'm', nombre: 'Marta' },
  cliente: { nombre: 'Ana', email: 'ana@example.com', telefono: null },
};

describe('utilidades de la agenda', () => {
  it('convierte una reserva en un evento con su estado como clase y la reserva adjunta', () => {
    const [evento] = aEventos([RESERVA], false);

    expect(evento).toMatchObject({
      id: 'r1',
      title: 'Ana · Corte',
      start: '2026-11-09T09:00:00Z',
      end: '2026-11-09T09:30:00Z',
      classNames: ['reserva-confirmada'],
    });
    expect(evento.extendedProps?.['reserva']).toBe(RESERVA);
  });

  it('añade el recurso al título cuando se ve todo el negocio', () => {
    expect(aEventos([RESERVA], true)[0].title).toBe('Ana · Corte · Marta');
  });

  it('ofrece solo los cambios de estado que admite el backend', () => {
    expect(transicionesDesde('PENDIENTE')).toEqual(['CONFIRMADA', 'CANCELADA']);
    expect(transicionesDesde('CONFIRMADA')).toEqual(['COMPLETADA', 'NO_PRESENTADO', 'CANCELADA']);
    expect(transicionesDesde('CANCELADA')).toEqual([]);
  });

  it('convierte el día y la hora del negocio al instante UTC correcto', () => {
    expect(instanteEnZona('2026-11-09', '10:00', 'Europe/Madrid')).toBe('2026-11-09T09:00:00Z');
    expect(instanteEnZona('2026-06-02', '10:00', 'Europe/Madrid')).toBe('2026-06-02T08:00:00Z');
    expect(instanteEnZona('2026-06-02', '10:00', 'Atlantic/Canary')).toBe('2026-06-02T09:00:00Z');
  });

  it('respeta el cambio de hora de marzo', () => {
    // Ese domingo las 02:00 pasan a ser las 03:00: las 03:30 ya son horario de verano (UTC+2)
    expect(instanteEnZona('2026-03-29', '01:30', 'Europe/Madrid')).toBe('2026-03-29T00:30:00Z');
    expect(instanteEnZona('2026-03-29', '03:30', 'Europe/Madrid')).toBe('2026-03-29T01:30:00Z');
  });
});
