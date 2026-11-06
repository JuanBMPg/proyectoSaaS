import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

import { Negocio } from '../../core/api/modelos';
import { NegocioApiService } from '../../core/api/negocio-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { SolicitudesPendientes } from './solicitudes-pendientes';

/** Página de inicio del panel: accesos directos y las solicitudes que esperan confirmación. */
@Component({
  selector: 'app-inicio',
  imports: [MatButtonModule, RouterLink, SolicitudesPendientes],
  templateUrl: './inicio.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Inicio {
  protected readonly rol = inject(AuthService).rol;
  protected readonly negocio = signal<Negocio | null>(null);

  constructor() {
    // El resumen del negocio es solo para el propietario; el empleado ve un acceso a su agenda
    if (this.rol() === 'PROPIETARIO') {
      inject(NegocioApiService)
        .obtener()
        .subscribe((negocio) => this.negocio.set(negocio));
    }
  }
}
