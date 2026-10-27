import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { FranjaHoraria, Horario } from './modelos';

@Injectable({ providedIn: 'root' })
export class HorarioApiService {
  private readonly http = inject(HttpClient);

  obtener(recursoId: string): Observable<Horario> {
    return this.http.get<Horario>(this.ruta(recursoId));
  }

  /** Sustituye la semana completa; una lista vacía deja el recurso sin horario. */
  reemplazar(recursoId: string, franjas: FranjaHoraria[]): Observable<Horario> {
    return this.http.put<Horario>(this.ruta(recursoId), { franjas });
  }

  private ruta(recursoId: string): string {
    return `/api/panel/recursos/${recursoId}/horario`;
  }
}
