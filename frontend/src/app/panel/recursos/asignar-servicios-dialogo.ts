import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

import { Recurso, Servicio } from '../../core/api/modelos';
import { RecursosApiService } from '../../core/api/recursos-api.service';
import { mensajeDeError } from '../../shared/errores-http';

export interface DatosAsignacion {
  recurso: Recurso;
  servicios: Servicio[];
}

/**
 * Marca qué servicios presta un recurso. Al guardar se envía el conjunto completo,
 * que sustituye al anterior (la operación es idempotente).
 */
@Component({
  selector: 'app-asignar-servicios-dialogo',
  imports: [MatButtonModule, MatCheckboxModule, MatDialogModule],
  templateUrl: './asignar-servicios-dialogo.html',
  styleUrl: '../dialogo-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AsignarServiciosDialogo {
  private readonly api = inject(RecursosApiService);
  private readonly dialogo = inject<MatDialogRef<AsignarServiciosDialogo, Recurso>>(MatDialogRef);
  protected readonly datos = inject<DatosAsignacion>(MAT_DIALOG_DATA);

  protected readonly seleccionados = signal(new Set(this.datos.recurso.servicioIds));
  /** Se ofrecen los activos y los inactivos que ya estaban asignados, para poder quitarlos. */
  protected readonly opciones = computed(() =>
    this.datos.servicios.filter(
      (servicio) => servicio.activo || this.datos.recurso.servicioIds.includes(servicio.id),
    ),
  );
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected alternar(servicioId: string, marcado: boolean): void {
    this.seleccionados.update((actuales) => {
      const nuevos = new Set(actuales);
      if (marcado) {
        nuevos.add(servicioId);
      } else {
        nuevos.delete(servicioId);
      }
      return nuevos;
    });
  }

  protected guardar(): void {
    this.guardando.set(true);
    this.error.set(null);
    this.api.asignarServicios(this.datos.recurso.id, [...this.seleccionados()]).subscribe({
      next: (recurso) => this.dialogo.close(recurso),
      error: (error: unknown) => {
        this.error.set(mensajeDeError(error));
        this.guardando.set(false);
      },
    });
  }
}
