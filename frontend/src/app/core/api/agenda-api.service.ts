import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { EstadoReserva, ReservaManualRequest, ReservaPanel } from './modelos';

const RUTA = '/api/panel/reservas';

@Injectable({ providedIn: 'root' })
export class AgendaApiService {
  private readonly http = inject(HttpClient);

  /** Reservas que se cruzan con [desde, hasta), instantes ISO en UTC. */
  listar(desde: string, hasta: string, recursoId: string | null): Observable<ReservaPanel[]> {
    let params = new HttpParams().set('desde', desde).set('hasta', hasta);
    if (recursoId) {
      params = params.set('recursoId', recursoId);
    }
    return this.http.get<ReservaPanel[]>(RUTA, { params });
  }

  crear(datos: ReservaManualRequest): Observable<ReservaPanel> {
    return this.http.post<ReservaPanel>(RUTA, datos);
  }

  cambiarEstado(id: string, estado: EstadoReserva): Observable<ReservaPanel> {
    return this.http.patch<ReservaPanel>(`${RUTA}/${id}/estado`, { estado });
  }
}
