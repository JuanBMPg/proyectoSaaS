import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AgendaApiService } from '../../core/api/agenda-api.service';
import { EstadoReserva, ReservaPanel } from '../../core/api/modelos';
import { NegocioApiService } from '../../core/api/negocio-api.service';
import { mensajeDeError } from '../../shared/errores-http';
import { fechaLargaEnZona, horaEnZona } from '../../shared/fechas';

/**
 * Solicitudes que esperan confirmación (negocios con confirmación manual), con los botones
 * para confirmarlas o rechazarlas sin abrir la agenda. Si no hay ninguna, no se muestra nada.
 */
@Component({
  selector: 'app-solicitudes-pendientes',
  imports: [MatButtonModule],
  templateUrl: './solicitudes-pendientes.html',
  styleUrl: './solicitudes-pendientes.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SolicitudesPendientes {
  private readonly api = inject(AgendaApiService);
  private readonly avisos = inject(MatSnackBar);

  protected readonly solicitudes = signal<ReservaPanel[]>([]);
  /** Zona del negocio: las horas se muestran como las verá el cliente. */
  private readonly zona = signal('Europe/Madrid');
  /** Id de la solicitud que se está guardando, para desactivar sus botones. */
  protected readonly guardando = signal<string | null>(null);

  constructor() {
    inject(NegocioApiService)
      .obtener()
      .subscribe((negocio) => this.zona.set(negocio.zonaHoraria));
    this.api.pendientes().subscribe({
      next: (solicitudes) => this.solicitudes.set(solicitudes),
      error: (error: unknown) =>
        this.avisos.open(mensajeDeError(error), 'Cerrar', { duration: 5000 }),
    });
  }

  protected cuando(reserva: ReservaPanel): string {
    return `${fechaLargaEnZona(reserva.inicio, this.zona())}, ${horaEnZona(reserva.inicio, this.zona())}`;
  }

  protected decidir(
    reserva: ReservaPanel,
    estado: Extract<EstadoReserva, 'CONFIRMADA' | 'CANCELADA'>,
  ): void {
    this.guardando.set(reserva.id);
    this.api.cambiarEstado(reserva.id, estado).subscribe({
      next: () => {
        this.solicitudes.update((lista) => lista.filter((r) => r.id !== reserva.id));
        this.guardando.set(null);
        this.avisos.open(
          estado === 'CONFIRMADA'
            ? `Cita de ${reserva.cliente.nombre} confirmada. Le hemos avisado por email.`
            : `Solicitud de ${reserva.cliente.nombre} rechazada. Le hemos avisado por email.`,
          undefined,
          { duration: 4000 },
        );
      },
      error: (error: unknown) => {
        this.guardando.set(null);
        this.avisos.open(mensajeDeError(error), 'Cerrar', { duration: 5000 });
      },
    });
  }
}
