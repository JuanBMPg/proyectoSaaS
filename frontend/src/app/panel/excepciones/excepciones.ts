import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { forkJoin } from 'rxjs';

import { Excepcion, ExcepcionCreada, Recurso } from '../../core/api/modelos';
import { ExcepcionesApiService } from '../../core/api/excepciones-api.service';
import { RecursosApiService } from '../../core/api/recursos-api.service';
import { mensajeDeError } from '../../shared/errores-http';
import { DatosExcepcion, ExcepcionDialogo } from './excepcion-dialogo';
import { avisoReservas, describirFechas, describirHorario } from './excepciones-utils';

/** Vacaciones, festivos y horarios especiales: cambian los huecos que ofrece la web pública. */
@Component({
  selector: 'app-excepciones',
  imports: [MatButtonModule, MatIconModule, MatTableModule],
  templateUrl: './excepciones.html',
  styleUrl: '../tabla-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Excepciones {
  private readonly api = inject(ExcepcionesApiService);
  private readonly recursosApi = inject(RecursosApiService);
  private readonly dialogos = inject(MatDialog);
  private readonly avisos = inject(MatSnackBar);

  protected readonly columnas = ['fechas', 'afecta', 'horario', 'motivo', 'acciones'];
  protected readonly excepciones = signal<Excepcion[]>([]);
  private readonly recursos = signal<Recurso[]>([]);
  protected readonly cargando = signal(true);
  protected readonly recursosActivos = computed(() => this.recursos().filter((r) => r.activo));

  protected readonly describirFechas = describirFechas;
  protected readonly describirHorario = describirHorario;

  constructor() {
    this.cargar();
  }

  protected nueva(): void {
    this.dialogos
      .open<ExcepcionDialogo, DatosExcepcion, ExcepcionCreada>(ExcepcionDialogo, {
        data: { recursos: this.recursosActivos() },
        width: '480px',
      })
      .afterClosed()
      .subscribe((creada) => {
        if (creada) {
          // Si ya había citas esos días, el aviso se queda más tiempo y hay que cerrarlo
          const aviso = avisoReservas(creada.reservasActivas);
          this.avisos.open(aviso ?? 'Excepción guardada', aviso ? 'Entendido' : undefined, {
            duration: aviso ? 10000 : 4000,
          });
          this.cargar();
        }
      });
  }

  protected borrar(excepcion: Excepcion): void {
    this.api.borrar(excepcion.id).subscribe({
      next: () => {
        this.avisos.open('Excepción borrada: vuelve a regir el horario semanal', undefined, {
          duration: 4000,
        });
        this.excepciones.update((lista) => lista.filter((e) => e.id !== excepcion.id));
      },
      error: (error: unknown) =>
        this.avisos.open(mensajeDeError(error), 'Cerrar', { duration: 5000 }),
    });
  }

  private cargar(): void {
    forkJoin({ excepciones: this.api.listar(), recursos: this.recursosApi.listar() }).subscribe({
      next: ({ excepciones, recursos }) => {
        this.excepciones.set(excepciones);
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
