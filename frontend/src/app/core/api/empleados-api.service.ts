import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Empleado, EmpleadoRequest } from './modelos';

const RUTA = '/api/panel/empleados';

@Injectable({ providedIn: 'root' })
export class EmpleadosApiService {
  private readonly http = inject(HttpClient);

  listar(): Observable<Empleado[]> {
    return this.http.get<Empleado[]>(RUTA);
  }

  crear(datos: EmpleadoRequest): Observable<Empleado> {
    return this.http.post<Empleado>(RUTA, datos);
  }
}
