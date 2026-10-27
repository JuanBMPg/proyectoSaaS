/**
 * Utilidades de fechas y horas para la web pública.
 *
 * Regla del proyecto: la API trabaja con instantes UTC y las horas se muestran en la zona
 * horaria del NEGOCIO, no en la del navegador. Un cliente que mira la web desde Canarias o
 * desde el extranjero ve las 10:15 de Madrid, que es cuando tiene que presentarse.
 */

/**
 * Fecha de calendario "YYYY-MM-DD" a partir de un Date del selector de fechas.
 * Usa el año, mes y día del propio Date, nunca toISOString(): esa función pasa a UTC y,
 * a las 00:30 en España, devolvería el día anterior.
 */
export function fechaIso(fecha: Date): string {
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

/** "09:00" en la zona indicada. */
export function horaEnZona(instante: string, zona: string): string {
  return new Intl.DateTimeFormat('es-ES', {
    timeZone: zona,
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).format(new Date(instante));
}

/** "martes, 2 de junio" en la zona indicada. */
export function fechaLargaEnZona(instante: string, zona: string): string {
  return new Intl.DateTimeFormat('es-ES', {
    timeZone: zona,
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(new Date(instante));
}

/** Las horas antes de las 14:00 (en la zona del negocio) se agrupan como mañana. */
export function esPorLaManana(instante: string, zona: string): boolean {
  return horaEnZona(instante, zona) < '14:00';
}
