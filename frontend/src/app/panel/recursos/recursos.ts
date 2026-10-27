import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { Observable, forkJoin } from 'rxjs';

import { Recurso, Servicio } from '../../core/api/modelos';
import { RecursosApiService } from '../../core/api/recursos-api.service';
import { ServiciosApiService } from '../../core/api/servicios-api.service';
import { mensajeDeError } from '../../shared/errores-http';
import { AsignarServiciosDialogo, DatosAsignacion } from './asignar-servicios-dialogo';
import { RecursoDialogo } from './recurso-dialogo';

/** Listado de recursos con alta, edición, activación y asignación de servicios. */
@Component({
  selector: 'app-recursos',
  imports: [MatButtonModule, MatChipsModule, MatIconModule, MatSlideToggleModule, MatTableModule],
  templateUrl: './recursos.html',
  styleUrl: '../tabla-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Recursos {
  private readonly api = inject(RecursosApiService);
  private readonly serviciosApi = inject(ServiciosApiService);
  private readonly dialogos = inject(MatDialog);
  private readonly avisos = inject(MatSnackBar);

  protected readonly columnas = ['nombre', 'tipo', 'servicios', 'activo', 'acciones'];
  protected readonly recursos = signal<Recurso[]>([]);
  private readonly servicios = signal<Servicio[]>([]);
  protected readonly cargando = signal(true);
  /** Para mostrar el nombre de cada servicio asignado a partir de su id. */
  private readonly nombresServicio = computed(
    () => new Map(this.servicios().map((servicio) => [servicio.id, servicio.nombre])),
  );

  constructor() {
    this.cargar();
  }

  protected nombresDe(recurso: Recurso): string[] {
    const nombres = this.nombresServicio();
    return recurso.servicioIds
      .map((id) => nombres.get(id))
      .filter((nombre): nombre is string => nombre !== undefined)
      .sort((a, b) => a.localeCompare(b, 'es'));
  }

  protected abrirDialogo(recurso: Recurso | null = null): void {
    this.dialogos
      .open<RecursoDialogo, Recurso | null, Recurso>(RecursoDialogo, {
        data: recurso,
        width: '440px',
      })
      .afterClosed()
      .subscribe((guardado) => {
        if (guardado) {
          this.cargar();
        }
      });
  }

  protected asignarServicios(recurso: Recurso): void {
    this.dialogos
      .open<AsignarServiciosDialogo, DatosAsignacion, Recurso>(AsignarServiciosDialogo, {
        data: { recurso, servicios: this.servicios() },
        width: '440px',
      })
      .afterClosed()
      .subscribe((guardado) => {
        if (guardado) {
          this.cargar();
        }
      });
  }

  /** DELETE desactiva; para reactivar se reenvía el recurso con activo = true. */
  protected cambiarActivo(recurso: Recurso, activo: boolean): void {
    const peticion: Observable<unknown> = activo
      ? this.api.actualizar(recurso.id, {
          nombre: recurso.nombre,
          tipo: recurso.tipo,
          activo: true,
        })
      : this.api.desactivar(recurso.id);
    peticion.subscribe({
      next: () => this.cargar(),
      error: (error: unknown) => {
        this.avisos.open(mensajeDeError(error), 'Cerrar', { duration: 5000 });
        this.cargar();
      },
    });
  }

  /** Recursos y servicios en paralelo: hacen falta los dos para mostrar las asignaciones. */
  private cargar(): void {
    forkJoin({ recursos: this.api.listar(), servicios: this.serviciosApi.listar() }).subscribe({
      next: ({ recursos, servicios }) => {
        this.recursos.set(recursos);
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
