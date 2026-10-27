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

/** Franja del horario semanal. Horas locales del negocio en formato "HH:mm". */
export interface FranjaHoraria {
  /** 1 = lunes ... 7 = domingo (ISO 8601). */
  diaSemana: number;
  horaInicio: string;
  horaFin: string;
}

export interface Horario {
  franjas: FranjaHoraria[];
}

// --- Web pública ---

export interface ServicioPublico {
  id: string;
  nombre: string;
  descripcion: string | null;
  duracionMin: number;
  precioCentimos: number;
  /** Recursos que prestan el servicio. */
  recursoIds: string[];
}

export interface RecursoPublico {
  id: string;
  nombre: string;
}

export interface NegocioPublico {
  nombre: string;
  slug: string;
  zonaHoraria: string;
  direccion: string | null;
  telefono: string | null;
  /** Cómo llama el negocio a sus recursos: "Profesional", "Pista"... */
  etiquetaRecurso: string;
  antelacionMaximaDias: number;
  servicios: ServicioPublico[];
  recursos: RecursoPublico[];
}

export interface HuecoPublico {
  recursoId: string;
  /** Instante ISO-8601 en UTC. */
  inicio: string;
  fin: string;
}

export interface Disponibilidad {
  fecha: string;
  zonaHoraria: string;
  huecos: HuecoPublico[];
}

export type EstadoReserva =
  'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA' | 'COMPLETADA' | 'NO_PRESENTADO';

export interface ReservaPublicaRequest {
  servicioId: string;
  recursoId: string;
  /** Instante UTC del hueco elegido. */
  inicio: string;
  nombre: string;
  email: string;
  telefono: string | null;
  notas: string | null;
}

/** Una reserva vista desde su enlace secreto. */
export interface ReservaPublica {
  estado: EstadoReserva;
  inicio: string;
  fin: string;
  clienteNombre: string;
  servicio: { nombre: string; duracionMin: number; precioCentimos: number };
  recurso: { nombre: string; etiqueta: string };
  negocio: {
    nombre: string;
    slug: string;
    zonaHoraria: string;
    direccion: string | null;
    telefono: string | null;
  };
}

/** Respuesta al crear: la única vez que llega el token en claro. */
export interface ReservaCreada {
  token: string;
  reserva: ReservaPublica;
}
