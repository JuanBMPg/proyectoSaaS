import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';

import { Negocio } from '../core/api/modelos';
import { NegocioApiService } from '../core/api/negocio-api.service';
import { AuthService } from '../core/auth/auth.service';

/**
 * Esqueleto del panel privado. De momento muestra el negocio y permite cerrar sesión;
 * la agenda, los servicios y los recursos llegarán en las siguientes tareas de la fase 1.
 */
@Component({
  selector: 'app-panel',
  imports: [MatButtonModule, MatToolbarModule],
  templateUrl: './panel.html',
  styleUrl: './panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Panel {
  private readonly auth = inject(AuthService);

  protected readonly rol = this.auth.rol;
  protected readonly negocio = signal<Negocio | null>(null);

  constructor() {
    // Los datos del negocio solo los puede ver el propietario (el backend responde 403 al empleado)
    if (this.auth.rol() === 'PROPIETARIO') {
      inject(NegocioApiService)
        .obtener()
        .subscribe((negocio) => this.negocio.set(negocio));
    }
  }

  protected cerrarSesion(): void {
    this.auth.logout().subscribe();
  }
}
