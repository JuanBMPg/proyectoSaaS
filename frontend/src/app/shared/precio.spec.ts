import { centimosAEuros, eurosACentimos } from './precio';

describe('conversión de precios', () => {
  it('convierte euros a céntimos sin errores de coma flotante', () => {
    // 19.99 * 100 = 1998.9999999999998 en JavaScript
    expect(eurosACentimos(19.99)).toBe(1999);
    expect(eurosACentimos(0.1 + 0.2)).toBe(30);
    expect(eurosACentimos(15)).toBe(1500);
    expect(eurosACentimos(0)).toBe(0);
  });

  it('convierte céntimos a euros', () => {
    expect(centimosAEuros(1999)).toBe(19.99);
    expect(centimosAEuros(1500)).toBe(15);
  });
});
