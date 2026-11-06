import { Excepcion } from '../../core/api/modelos';
import { fechaCorta } from '../../shared/fechas';

/** "jue, 24 dic" o "lun, 3 ago – vie, 14 ago". */
export function describirFechas(excepcion: Pick<Excepcion, 'fechaInicio' | 'fechaFin'>): string {
  const inicio = fechaCorta(excepcion.fechaInicio);
  return excepcion.fechaInicio === excepcion.fechaFin
    ? inicio
    : `${inicio} – ${fechaCorta(excepcion.fechaFin)}`;
}

/** "Cerrado" o "Solo de 10:00 a 14:00". */
export function describirHorario(excepcion: Pick<Excepcion, 'horaInicio' | 'horaFin'>): string {
  return excepcion.horaInicio && excepcion.horaFin
    ? `Solo de ${excepcion.horaInicio} a ${excepcion.horaFin}`
    : 'Cerrado';
}

/** Aviso tras crear una excepción en días que ya tienen citas (no se cancelan solas). */
export function avisoReservas(reservasActivas: number): string | null {
  if (reservasActivas === 0) {
    return null;
  }
  return reservasActivas === 1
    ? 'Hay 1 reserva en esas fechas: revísala en la agenda.'
    : `Hay ${reservasActivas} reservas en esas fechas: revísalas en la agenda.`;
}
