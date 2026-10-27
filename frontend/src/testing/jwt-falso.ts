import { Rol } from '../app/core/api/modelos';

/**
 * Construye un JWT con la forma correcta para los tests. La firma es inventada:
 * el frontend nunca la verifica, solo lee el payload para decidir qué mostrar.
 */
export function jwtFalso(rol: Rol = 'PROPIETARIO', sub = 'usuario-1'): string {
  const base64Url = (objeto: object) =>
    btoa(JSON.stringify(objeto)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  const cabecera = base64Url({ alg: 'HS256', typ: 'JWT' });
  const payload = base64Url({ sub, negocioId: 'negocio-1', rol });
  return `${cabecera}.${payload}.firma-de-prueba`;
}

export function respuestaToken(token: string) {
  return { accessToken: token, tokenType: 'Bearer', expiraEn: '2026-06-02T08:15:00Z' };
}
