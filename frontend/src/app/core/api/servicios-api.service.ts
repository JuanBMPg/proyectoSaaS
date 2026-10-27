import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Servicio, ServicioRequest } from './modelos';

const RUTA = '/api/panel/servicios';

@Injectable({ providedIn: 'root' })
export class ServiciosApiService {
  private readonly http = inject(HttpClient);

  listar(): Observable<Servicio[]> {
    return this.http.get<Servicio[]>(RUTA);
  }

  crear(datos: ServicioRequest): Observable<Servicio> {
    return this.http.post<Servicio>(RUTA, datos);
  }

  actualizar(id: string, datos: ServicioRequest): Observable<Servicio> {
    return this.http.put<Servicio>(`${RUTA}/${id}`, datos);
  }

  desactivar(id: string): Observable<void> {
    return this.http.delete<void>(`${RUTA}/${id}`);
  }
}
