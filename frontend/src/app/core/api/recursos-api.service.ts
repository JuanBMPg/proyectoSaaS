import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Recurso, RecursoRequest } from './modelos';

const RUTA = '/api/panel/recursos';

@Injectable({ providedIn: 'root' })
export class RecursosApiService {
  private readonly http = inject(HttpClient);

  listar(): Observable<Recurso[]> {
    return this.http.get<Recurso[]>(RUTA);
  }

  crear(datos: RecursoRequest): Observable<Recurso> {
    return this.http.post<Recurso>(RUTA, datos);
  }

  actualizar(id: string, datos: RecursoRequest): Observable<Recurso> {
    return this.http.put<Recurso>(`${RUTA}/${id}`, datos);
  }

  desactivar(id: string): Observable<void> {
    return this.http.delete<void>(`${RUTA}/${id}`);
  }

  /** Sustituye el conjunto completo de servicios que presta el recurso. */
  asignarServicios(id: string, servicioIds: string[]): Observable<Recurso> {
    return this.http.put<Recurso>(`${RUTA}/${id}/servicios`, { servicioIds });
  }
}
