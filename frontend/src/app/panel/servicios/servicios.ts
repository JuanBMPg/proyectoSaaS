import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { Observable } from 'rxjs';

import { Servicio } from '../../core/api/modelos';
import { ServiciosApiService } from '../../core/api/servicios-api.service';
import { mensajeDeError } from '../../shared/errores-http';
import { centimosAEuros } from '../../shared/precio';
import { ServicioDialogo } from './servicio-dialogo';

/** Listado de servicios del negocio con alta, edición y activación. */
@Component({
  selector: 'app-servicios',
  imports: [CurrencyPipe, MatButtonModule, MatIconModule, MatSlideToggleModule, MatTableModule],
  templateUrl: './servicios.html',
  styleUrl: '../tabla-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Servicios {
  private readonly api = inject(ServiciosApiService);
  private readonly dialogos = inject(MatDialog);
  private readonly avisos = inject(MatSnackBar);

  protected readonly columnas = ['nombre', 'duracion', 'precio', 'activo', 'acciones'];
  protected readonly servicios = signal<Servicio[]>([]);
  protected readonly cargando = signal(true);
  protected readonly centimosAEuros = centimosAEuros;

  constructor() {
    this.cargar();
  }

  protected abrirDialogo(servicio: Servicio | null = null): void {
    this.dialogos
      .open<ServicioDialogo, Servicio | null, Servicio>(ServicioDialogo, {
        data: servicio,
        width: '480px',
      })
      .afterClosed()
      .subscribe((guardado) => {
        if (guardado) {
          this.cargar();
        }
      });
  }

  /** DELETE desactiva; para reactivar se reenvía el servicio con activo = true. */
  protected cambiarActivo(servicio: Servicio, activo: boolean): void {
    const peticion: Observable<unknown> = activo
      ? this.api.actualizar(servicio.id, { ...servicio, activo: true })
      : this.api.desactivar(servicio.id);
    peticion.subscribe({
      next: () => this.cargar(),
      error: (error: unknown) => {
        this.avisos.open(mensajeDeError(error), 'Cerrar', { duration: 5000 });
        this.cargar();
      },
    });
  }

  private cargar(): void {
    this.api.listar().subscribe({
      next: (servicios) => {
        this.servicios.set(servicios);
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.avisos.open(mensajeDeError(error), 'Cerrar', { duration: 5000 });
        this.cargando.set(false);
      },
    });
  }
}
