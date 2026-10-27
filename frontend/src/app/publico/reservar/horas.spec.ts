import { HuecoPublico } from '../../core/api/modelos';
import { horasDelDia } from './horas';

function hueco(recursoId: string, inicio: string): HuecoPublico {
  return { recursoId, inicio, fin: inicio };
}

describe('horasDelDia', () => {
  it('con varios profesionales libres a la misma hora la ofrece una sola vez, asignada al primero', () => {
    const horas = horasDelDia(
      [
        hueco('marta', '2026-06-02T08:00:00Z'),
        hueco('pedro', '2026-06-02T08:00:00Z'),
        hueco('pedro', '2026-06-02T08:15:00Z'),
      ],
      'Europe/Madrid',
    );

    expect(horas.manana).toEqual([
      { inicio: '2026-06-02T08:00:00Z', recursoId: 'marta', etiqueta: '10:00' },
      { inicio: '2026-06-02T08:15:00Z', recursoId: 'pedro', etiqueta: '10:15' },
    ]);
  });

  it('separa las horas de mañana y de tarde', () => {
    const horas = horasDelDia(
      [hueco('marta', '2026-06-02T11:30:00Z'), hueco('marta', '2026-06-02T14:00:00Z')],
      'Europe/Madrid',
    );

    expect(horas.manana.map((h) => h.etiqueta)).toEqual(['13:30']);
    expect(horas.tarde.map((h) => h.etiqueta)).toEqual(['16:00']);
  });

  it('sin huecos devuelve dos listas vacías', () => {
    expect(horasDelDia([], 'Europe/Madrid')).toEqual({ manana: [], tarde: [] });
  });
});
