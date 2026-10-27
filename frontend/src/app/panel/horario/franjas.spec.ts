import { agruparPorDia, aplanar, haySolapes } from './franjas';

describe('funciones del horario', () => {
  it('agrupa la lista plana de la API por día y ordena cada día por hora', () => {
    const semana = agruparPorDia([
      { diaSemana: 1, horaInicio: '16:00', horaFin: '20:00' },
      { diaSemana: 1, horaInicio: '09:00', horaFin: '14:00' },
      { diaSemana: 7, horaInicio: '10:00', horaFin: '13:00' },
    ]);

    expect(semana).toHaveLength(7);
    expect(semana[0]).toEqual([
      { horaInicio: '09:00', horaFin: '14:00' },
      { horaInicio: '16:00', horaFin: '20:00' },
    ]);
    expect(semana[6]).toEqual([{ horaInicio: '10:00', horaFin: '13:00' }]);
    expect(semana.slice(1, 6).every((dia) => dia.length === 0)).toBe(true);
  });

  it('aplanar es la operación inversa de agrupar', () => {
    const franjas = [
      { diaSemana: 1, horaInicio: '09:00', horaFin: '14:00' },
      { diaSemana: 3, horaInicio: '10:00', horaFin: '18:00' },
    ];

    expect(aplanar(agruparPorDia(franjas))).toEqual(franjas);
  });

  it('detecta franjas solapadas, sin importar el orden', () => {
    expect(
      haySolapes([
        { horaInicio: '13:00', horaFin: '18:00' },
        { horaInicio: '09:00', horaFin: '14:00' },
      ]),
    ).toBe(true);
  });

  it('las franjas contiguas no se solapan', () => {
    expect(
      haySolapes([
        { horaInicio: '09:00', horaFin: '14:00' },
        { horaInicio: '14:00', horaFin: '18:00' },
      ]),
    ).toBe(false);
  });

  it('una jornada partida no se solapa', () => {
    expect(
      haySolapes([
        { horaInicio: '09:00', horaFin: '14:00' },
        { horaInicio: '16:00', horaFin: '20:00' },
      ]),
    ).toBe(false);
  });
});
