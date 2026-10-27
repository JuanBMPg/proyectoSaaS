import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Disponibilidad,
  NegocioPublico,
  ReservaCreada,
  ReservaPublica,
  ReservaPublicaRequest,
} from './modelos';

/** API de la web pública: sin login, el negocio se identifica por su slug. */
@Injectable({ providedIn: 'root' })
export class PublicoApiService {
  private readonly http = inject(HttpClient);

  negocio(slug: string): Observable<NegocioPublico> {
    return this.http.get<NegocioPublico>(`/api/publico/${encodeURIComponent(slug)}`);
  }

  /**
   * @param fecha día en formato "YYYY-MM-DD", en la zona del negocio
   * @param recursoId opcional: sin él llegan los huecos de todos los recursos
   */
  disponibilidad(
    slug: string,
    servicioId: string,
    fecha: string,
    recursoId: string | null,
  ): Observable<Disponibilidad> {
    let params = new HttpParams().set('servicioId', servicioId).set('fecha', fecha);
    if (recursoId) {
      params = params.set('recursoId', recursoId);
    }
    return this.http.get<Disponibilidad>(
      `/api/publico/${encodeURIComponent(slug)}/disponibilidad`,
      { params },
    );
  }

  /** 409 si el hueco se acaba de ocupar; 422 si incumple la antelación del negocio. */
  crearReserva(slug: string, datos: ReservaPublicaRequest): Observable<ReservaCreada> {
    return this.http.post<ReservaCreada>(
      `/api/publico/${encodeURIComponent(slug)}/reservas`,
      datos,
    );
  }

  reserva(token: string): Observable<ReservaPublica> {
    return this.http.get<ReservaPublica>(`/api/publico/reservas/${encodeURIComponent(token)}`);
  }
}
