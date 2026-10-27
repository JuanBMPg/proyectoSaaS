import { esPorLaManana, fechaIso, fechaLargaEnZona, horaEnZona } from './fechas';

describe('fechas y horas', () => {
  it('fechaIso usa el día del calendario, también de madrugada', () => {
    // 00:30 del 2 de junio en el navegador: toISOString() daría el 1 de junio en España
    expect(fechaIso(new Date(2026, 5, 2, 0, 30))).toBe('2026-06-02');
    expect(fechaIso(new Date(2026, 0, 9))).toBe('2026-01-09');
  });

  it('muestra la hora en la zona del negocio y no en la del navegador', () => {
    const instante = '2026-06-02T07:00:00Z';

    expect(horaEnZona(instante, 'Europe/Madrid')).toBe('09:00');
    expect(horaEnZona(instante, 'Atlantic/Canary')).toBe('08:00');
    expect(horaEnZona(instante, 'America/Mexico_City')).toBe('01:00');
  });

  it('tiene en cuenta el horario de invierno', () => {
    expect(horaEnZona('2026-01-13T08:00:00Z', 'Europe/Madrid')).toBe('09:00');
  });

  it('escribe el día completo en español en la zona del negocio', () => {
    // 23:30 UTC del lunes 1 de junio ya es martes 2 en Madrid
    expect(fechaLargaEnZona('2026-06-01T23:30:00Z', 'Europe/Madrid')).toBe('martes, 2 de junio');
  });

  it('separa la mañana de la tarde a las 14:00 del negocio', () => {
    expect(esPorLaManana('2026-06-02T11:45:00Z', 'Europe/Madrid')).toBe(true); // 13:45
    expect(esPorLaManana('2026-06-02T12:00:00Z', 'Europe/Madrid')).toBe(false); // 14:00
  });
});
