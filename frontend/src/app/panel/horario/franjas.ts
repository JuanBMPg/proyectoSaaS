import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

import { FranjaHoraria } from '../../core/api/modelos';

/** Nombres de los días en el orden ISO: índice 0 = lunes (diaSemana 1). */
export const NOMBRES_DIA = [
  'Lunes',
  'Martes',
  'Miércoles',
  'Jueves',
  'Viernes',
  'Sábado',
  'Domingo',
] as const;

/** Franja sin el día: el día lo da la posición en la semana. */
export interface Tramo {
  horaInicio: string;
  horaFin: string;
}

/** Lista plana de la API → siete listas (lunes a domingo) ordenadas por hora. */
export function agruparPorDia(franjas: FranjaHoraria[]): Tramo[][] {
  const semana: Tramo[][] = NOMBRES_DIA.map(() => []);
  for (const franja of franjas) {
    semana[franja.diaSemana - 1].push({ horaInicio: franja.horaInicio, horaFin: franja.horaFin });
  }
  return semana.map((tramos) =>
    [...tramos].sort((a, b) => a.horaInicio.localeCompare(b.horaInicio)),
  );
}

/** Siete listas → lista plana para la API. */
export function aplanar(semana: Tramo[][]): FranjaHoraria[] {
  return semana.flatMap((tramos, indice) =>
    tramos.map((tramo) => ({ diaSemana: indice + 1, ...tramo })),
  );
}

/**
 * Hay solape si, ordenados por inicio, alguno empieza antes de que termine el anterior.
 * Rango [inicio, fin): 09:00-14:00 y 14:00-18:00 son contiguos y no se solapan.
 * Las horas "HH:mm" se pueden comparar como texto porque tienen siempre la misma longitud.
 */
export function haySolapes(tramos: Tramo[]): boolean {
  const completos = tramos
    .filter((tramo) => tramo.horaInicio && tramo.horaFin)
    .sort((a, b) => a.horaInicio.localeCompare(b.horaInicio));
  return completos.some((tramo, i) => i > 0 && tramo.horaInicio < completos[i - 1].horaFin);
}

/** Validador de una franja: la hora de inicio debe ser anterior a la de fin. */
export const inicioAntesDeFin: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const { horaInicio, horaFin } = control.value as Tramo;
  return horaInicio && horaFin && horaInicio >= horaFin ? { inicioDespuesDeFin: true } : null;
};

/** Validador de un día: sus franjas no pueden solaparse (el backend lo comprueba también). */
export const sinSolapes: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  return haySolapes(control.value as Tramo[]) ? { solape: true } : null;
};
