/** Tipos de los DTOs de la API. Reflejan los `record` del backend. */

export type Rol = 'PROPIETARIO' | 'EMPLEADO';

export interface AccessTokenResponse {
  accessToken: string;
  tokenType: 'Bearer';
  /** Instante ISO-8601 en UTC. */
  expiraEn: string;
}

export interface RegistroRequest {
  nombreNegocio: string;
  slug: string;
  nombre: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface Negocio {
  id: string;
  nombre: string;
  slug: string;
  zonaHoraria: string;
  email: string | null;
  telefono: string | null;
  direccion: string | null;
  etiquetaRecurso: string;
  antelacionMinimaMin: number;
  antelacionMaximaDias: number;
  plazoCancelacionHoras: number;
  confirmacionAutomatica: boolean;
}

/** Formato de error del backend (RFC 9457), con los errores por campo en las respuestas 400. */
export interface ProblemDetail {
  title?: string;
  status: number;
  detail?: string;
  errores?: Record<string, string>;
}

export interface Servicio {
  id: string;
  nombre: string;
  descripcion: string | null;
  duracionMin: number;
  margenMin: number;
  /** Precio en céntimos: 1500 = 15,00 €. */
  precioCentimos: number;
  activo: boolean;
}

export interface ServicioRequest {
  nombre: string;
  descripcion: string | null;
  duracionMin: number;
  margenMin: number;
  precioCentimos: number;
  /** Opcional: null deja el estado como está; true reactiva. */
  activo?: boolean | null;
}

export type TipoRecurso = 'PERSONA' | 'COSA';

export interface Recurso {
  id: string;
  nombre: string;
  tipo: TipoRecurso;
  activo: boolean;
  servicioIds: string[];
}

export interface RecursoRequest {
  nombre: string;
  tipo: TipoRecurso;
  activo?: boolean | null;
}
