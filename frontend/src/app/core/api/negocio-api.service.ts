import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Negocio } from './modelos';

@Injectable({ providedIn: 'root' })
export class NegocioApiService {
  private readonly http = inject(HttpClient);

  obtener(): Observable<Negocio> {
    return this.http.get<Negocio>('/api/panel/negocio');
  }
}
