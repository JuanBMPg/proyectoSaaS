import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Disponibilidad, NegocioPublico } from './modelos';

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
}
