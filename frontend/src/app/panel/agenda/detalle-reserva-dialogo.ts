import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

import { AgendaApiService } from '../../core/api/agenda-api.service';
import { EstadoReserva, ReservaPanel } from '../../core/api/modelos';
import { mensajeDeError } from '../../shared/errores-http';
import { fechaLargaEnZona, horaEnZona } from '../../shared/fechas';
import { centimosAEuros } from '../../shared/precio';
import { ACCION_ESTADO, ETIQUETA_ESTADO, transicionesDesde } from './agenda-utils';

export interface DatosDetalle {
  reserva: ReservaPanel;
  zona: string;
}

/** Detalle de una reserva de la agenda, con los cambios de estado que admite. */
@Component({
  selector: 'app-detalle-reserva-dialogo',
  imports: [CurrencyPipe, MatButtonModule, MatDialogModule],
  templateUrl: './detalle-reserva-dialogo.html',
  styleUrl: '../dialogo-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetalleReservaDialogo {
  private readonly api = inject(AgendaApiService);
  private readonly dialogo =
    inject<MatDialogRef<DetalleReservaDialogo, ReservaPanel>>(MatDialogRef);
  protected readonly datos = inject<DatosDetalle>(MAT_DIALOG_DATA);

  protected readonly reserva = this.datos.reserva;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO;
  protected readonly accionEstado = ACCION_ESTADO;
  protected readonly centimosAEuros = centimosAEuros;
  protected readonly acciones = transicionesDesde(this.reserva.estado);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly cuando = computed(() => ({
    dia: fechaLargaEnZona(this.reserva.inicio, this.datos.zona),
    desde: horaEnZona(this.reserva.inicio, this.datos.zona),
    hasta: horaEnZona(this.reserva.fin, this.datos.zona),
  }));

  protected cambiarA(estado: EstadoReserva): void {
    this.guardando.set(true);
    this.error.set(null);
    this.api.cambiarEstado(this.reserva.id, estado).subscribe({
      next: (actualizada) => this.dialogo.close(actualizada),
      error: (error: unknown) => {
        this.error.set(mensajeDeError(error));
        this.guardando.set(false);
      },
    });
  }
}
