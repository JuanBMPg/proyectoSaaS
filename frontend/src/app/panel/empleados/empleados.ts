import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { forkJoin } from 'rxjs';

import { Empleado, Recurso } from '../../core/api/modelos';
import { EmpleadosApiService } from '../../core/api/empleados-api.service';
import { RecursosApiService } from '../../core/api/recursos-api.service';
import { mensajeDeError } from '../../shared/errores-http';
import { DatosEmpleado, EmpleadoDialogo } from './empleado-dialogo';

/** Empleados del negocio: cada uno ve en el panel solo la agenda de su recurso. */
@Component({
  selector: 'app-empleados',
  imports: [MatButtonModule, MatIconModule, MatTableModule],
  templateUrl: './empleados.html',
  styleUrl: '../tabla-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Empleados {
  private readonly api = inject(EmpleadosApiService);
  private readonly recursosApi = inject(RecursosApiService);
  private readonly dialogos = inject(MatDialog);
  private readonly avisos = inject(MatSnackBar);

  protected readonly columnas = ['nombre', 'email', 'recurso'];
  protected readonly empleados = signal<Empleado[]>([]);
  private readonly recursos = signal<Recurso[]>([]);
  protected readonly cargando = signal(true);

  /** Recursos activos que aún no tienen empleado (un recurso, un empleado). */
  protected readonly recursosLibres = computed(() => {
    const ocupados = new Set(this.empleados().map((e) => e.recurso?.id));
    return this.recursos().filter((r) => r.activo && !ocupados.has(r.id));
  });

  constructor() {
    this.cargar();
  }

  protected nuevo(): void {
    this.dialogos
      .open<EmpleadoDialogo, DatosEmpleado, Empleado>(EmpleadoDialogo, {
        data: { recursosLibres: this.recursosLibres() },
        width: '440px',
      })
      .afterClosed()
      .subscribe((creado) => {
        if (creado) {
          this.avisos.open(`${creado.nombre} ya puede entrar con su email`, undefined, {
            duration: 4000,
          });
          this.cargar();
        }
      });
  }

  private cargar(): void {
    forkJoin({ empleados: this.api.listar(), recursos: this.recursosApi.listar() }).subscribe({
      next: ({ empleados, recursos }) => {
        this.empleados.set(empleados);
        this.recursos.set(recursos);
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.avisos.open(mensajeDeError(error), 'Cerrar', { duration: 5000 });
        this.cargando.set(false);
      },
    });
  }
}
