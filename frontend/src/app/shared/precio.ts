/**
 * La API trabaja con céntimos (enteros) y el usuario escribe euros.
 * Math.round corrige los errores de coma flotante: 19.99 * 100 = 1998.9999999999998.
 */
export function eurosACentimos(euros: number): number {
  return Math.round(euros * 100);
}

export function centimosAEuros(centimos: number): number {
  return centimos / 100;
}
