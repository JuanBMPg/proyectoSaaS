import { EventInput } from '@fullcalendar/angular';
import { Temporal } from 'temporal-polyfill';

import { EstadoReserva, ReservaPanel } from '../../core/api/modelos';

export const ETIQUETA_ESTADO: Record<EstadoReserva, string> = {
  PENDIENTE: 'Pendiente',
  CONFIRMADA: 'Confirmada',
  CANCELADA: 'Cancelada',
  COMPLETADA: 'Completada',
  NO_PRESENTADO: 'No se presentó',
};

/** Texto del botón que lleva a cada estado. */
export const ACCION_ESTADO: Record<EstadoReserva, string> = {
  PENDIENTE: 'Dejar pendiente',
  CONFIRMADA: 'Confirmar',
  CANCELADA: 'Cancelar reserva',
  COMPLETADA: 'Marcar completada',
  NO_PRESENTADO: 'No se presentó',
};

/**
 * Cambios de estado permitidos: copia de EstadoReserva.puedePasarA del backend, que es quien
 * decide de verdad (responde 422). Aquí solo sirve para no mostrar botones que fallarían.
 */
const TRANSICIONES: Record<EstadoReserva, EstadoReserva[]> = {
  PENDIENTE: ['CONFIRMADA', 'CANCELADA'],
  CONFIRMADA: ['COMPLETADA', 'NO_PRESENTADO', 'CANCELADA'],
  COMPLETADA: ['NO_PRESENTADO'],
  NO_PRESENTADO: ['COMPLETADA'],
  CANCELADA: [],
};

export function transicionesDesde(estado: EstadoReserva): EstadoReserva[] {
  return TRANSICIONES[estado];
}

/**
 * Reservas de la API → eventos de FullCalendar. El estado va como clase CSS para darle color,
 * y la reserva completa en extendedProps para abrir su detalle al pulsarla.
 *
 * @param conRecurso añade el nombre del recurso al título (vista de todo el negocio)
 */
export function aEventos(reservas: ReservaPanel[], conRecurso: boolean): EventInput[] {
  return reservas.map((reserva) => ({
    id: reserva.id,
    title: conRecurso
      ? `${reserva.cliente.nombre} · ${reserva.servicio.nombre} · ${reserva.recurso.nombre}`
      : `${reserva.cliente.nombre} · ${reserva.servicio.nombre}`,
    start: reserva.inicio,
    end: reserva.fin,
    classNames: [`reserva-${reserva.estado.toLowerCase()}`],
    extendedProps: { reserva },
  }));
}

/**
 * Día y hora escritos en la zona del NEGOCIO → instante UTC para la API.
 * Usa Temporal (polyfill que ya necesita FullCalendar), que conoce las reglas de cada zona:
 * el 29 de marzo, las 03:30 de Madrid son las 01:30 UTC aunque ese día empiece en invierno.
 */
export function instanteEnZona(fecha: string, hora: string, zona: string): string {
  return Temporal.PlainDateTime.from(`${fecha}T${hora}`)
    .toZonedDateTime(zona)
    .toInstant()
    .toString();
}
