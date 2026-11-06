import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Excepcion, ExcepcionCreada, ExcepcionRequest } from './modelos';

const RUTA = '/api/panel/excepciones';

@Injectable({ providedIn: 'root' })
export class ExcepcionesApiService {
  private readonly http = inject(HttpClient);

  /** Las vigentes hoy o futuras. */
  listar(): Observable<Excepcion[]> {
    return this.http.get<Excepcion[]>(RUTA);
  }

  crear(datos: ExcepcionRequest): Observable<ExcepcionCreada> {
    return this.http.post<ExcepcionCreada>(RUTA, datos);
  }

  borrar(id: string): Observable<void> {
    return this.http.delete<void>(`${RUTA}/${id}`);
  }
}
