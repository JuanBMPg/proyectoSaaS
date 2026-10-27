import { HuecoPublico } from '../../core/api/modelos';
import { esPorLaManana, horaEnZona } from '../../shared/fechas';

/** Hora que se ofrece al cliente, con el recurso al que se asignará la reserva. */
export interface HoraOfrecida {
  inicio: string;
  recursoId: string;
  etiqueta: string;
}

export interface HorasDelDia {
  manana: HoraOfrecida[];
  tarde: HoraOfrecida[];
}

/**
 * Convierte los huecos de la API en las horas que ve el cliente.
 * Con "sin preferencia" llegan los huecos de todos los profesionales: si varios están libres
 * a la misma hora, se ofrece una sola vez y la reserva irá al primero (la API los ordena por
 * hora y, a igual hora, por id de recurso), así el cliente no ve horas repetidas.
 */
export function horasDelDia(huecos: HuecoPublico[], zona: string): HorasDelDia {
  const vistas = new Set<string>();
  const unicas: HoraOfrecida[] = [];
  for (const hueco of huecos) {
    if (!vistas.has(hueco.inicio)) {
      vistas.add(hueco.inicio);
      unicas.push({
        inicio: hueco.inicio,
        recursoId: hueco.recursoId,
        etiqueta: horaEnZona(hueco.inicio, zona),
      });
    }
  }
  return {
    manana: unicas.filter((hora) => esPorLaManana(hora.inicio, zona)),
    tarde: unicas.filter((hora) => !esPorLaManana(hora.inicio, zona)),
  };
}
