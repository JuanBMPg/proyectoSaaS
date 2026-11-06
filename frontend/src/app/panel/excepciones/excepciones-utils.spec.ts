import { avisoReservas, describirFechas, describirHorario } from './excepciones-utils';

describe('excepciones-utils', () => {
  it('describe un día suelto o un rango', () => {
    expect(describirFechas({ fechaInicio: '2026-12-24', fechaFin: '2026-12-24' })).toBe(
      'jue, 24 dic',
    );
    expect(describirFechas({ fechaInicio: '2026-08-03', fechaFin: '2026-08-14' })).toBe(
      'lun, 3 ago – vie, 14 ago',
    );
  });

  it('distingue un cierre de un horario especial', () => {
    expect(describirHorario({ horaInicio: null, horaFin: null })).toBe('Cerrado');
    expect(describirHorario({ horaInicio: '10:00', horaFin: '14:00' })).toBe(
      'Solo de 10:00 a 14:00',
    );
  });

  it('solo avisa si hay reservas afectadas', () => {
    expect(avisoReservas(0)).toBeNull();
    expect(avisoReservas(1)).toBe('Hay 1 reserva en esas fechas: revísala en la agenda.');
    expect(avisoReservas(3)).toBe('Hay 3 reservas en esas fechas: revísalas en la agenda.');
  });
});
