import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

import { Negocio } from '../../core/api/modelos';
import { NegocioApiService } from '../../core/api/negocio-api.service';
import { AuthService } from '../../core/auth/auth.service';

/** Página de inicio del panel. La agenda llegará en una tarea posterior de la fase 1. */
@Component({
  selector: 'app-inicio',
  imports: [MatButtonModule, RouterLink],
  templateUrl: './inicio.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Inicio {
  protected readonly rol = inject(AuthService).rol;
  protected readonly negocio = signal<Negocio | null>(null);

  constructor() {
    // Los datos del negocio solo los puede ver el propietario (el backend responde 403 al empleado)
    if (this.rol() === 'PROPIETARIO') {
      inject(NegocioApiService)
        .obtener()
        .subscribe((negocio) => this.negocio.set(negocio));
    }
  }
}
