import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import {
  FormArray,
  FormControl,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Recurso } from '../../core/api/modelos';
import { HorarioApiService } from '../../core/api/horario-api.service';
import { RecursosApiService } from '../../core/api/recursos-api.service';
import { mensajeDeError } from '../../shared/errores-http';
import {
  NOMBRES_DIA,
  Tramo,
  agruparPorDia,
  aplanar,
  inicioAntesDeFin,
  sinSolapes,
} from './franjas';

type FormularioTramo = FormGroup<{
  horaInicio: FormControl<string>;
  horaFin: FormControl<string>;
}>;

/** Índices de martes a viernes, para el atajo "copiar el lunes a los laborables". */
const MARTES_A_VIERNES = [1, 2, 3, 4];

/**
 * Edición del horario semanal de un recurso: siete días, cada uno con sus franjas.
 * Al guardar se envía la semana completa, que sustituye a la anterior.
 */
@Component({
  selector: 'app-horario-recurso',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './horario-recurso.html',
  styleUrl: './horario-recurso.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HorarioRecurso implements OnInit {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly api = inject(HorarioApiService);
  private readonly recursosApi = inject(RecursosApiService);
  private readonly avisos = inject(MatSnackBar);

  /** Id del recurso, tomado de la URL /panel/recursos/:id/horario. */
  readonly id = input.required<string>();

  protected readonly nombresDia = NOMBRES_DIA;
  protected readonly recurso = signal<Recurso | null>(null);
  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly formulario = this.fb.group({
    dias: this.fb.array(NOMBRES_DIA.map(() => this.fb.array<FormularioTramo>([], sinSolapes))),
  });

  ngOnInit(): void {
    // No hay endpoint para un solo recurso: se busca en el listado, que ya trae su nombre
    forkJoin({
      horario: this.api.obtener(this.id()),
      recursos: this.recursosApi.listar(),
    }).subscribe({
      next: ({ horario, recursos }) => {
        this.recurso.set(recursos.find((r) => r.id === this.id()) ?? null);
        this.cargarSemana(agruparPorDia(horario.franjas));
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.error.set(mensajeDeError(error));
        this.cargando.set(false);
      },
    });
  }

  protected dia(indice: number): FormArray<FormularioTramo> {
    return this.formulario.controls.dias.at(indice);
  }

  /** La primera franja propone una mañana; la segunda, una tarde (jornada partida). */
  protected anadirFranja(indice: number): void {
    const dia = this.dia(indice);
    const tramo: Tramo =
      dia.length === 0
        ? { horaInicio: '09:00', horaFin: '14:00' }
        : { horaInicio: '16:00', horaFin: '20:00' };
    dia.push(this.crearTramo(tramo));
    this.formulario.markAsDirty();
  }

  protected quitarFranja(indice: number, posicion: number): void {
    this.dia(indice).removeAt(posicion);
    this.formulario.markAsDirty();
  }

  protected copiarLunesALaborables(): void {
    const lunes = this.dia(0).getRawValue();
    for (const indice of MARTES_A_VIERNES) {
      this.reemplazarDia(indice, lunes);
    }
    this.formulario.markAsDirty();
  }

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const franjas = aplanar(this.formulario.controls.dias.getRawValue());

    this.guardando.set(true);
    this.error.set(null);
    this.api.reemplazar(this.id(), franjas).subscribe({
      next: (horario) => {
        this.cargarSemana(agruparPorDia(horario.franjas));
        this.guardando.set(false);
        this.avisos.open('Horario guardado', undefined, { duration: 3000 });
      },
      error: (error: unknown) => {
        this.error.set(mensajeDeError(error));
        this.guardando.set(false);
      },
    });
  }

  private cargarSemana(semana: Tramo[][]): void {
    semana.forEach((tramos, indice) => this.reemplazarDia(indice, tramos));
    this.formulario.markAsPristine();
  }

  private reemplazarDia(indice: number, tramos: Tramo[]): void {
    const dia = this.dia(indice);
    dia.clear();
    tramos.forEach((tramo) => dia.push(this.crearTramo(tramo)));
  }

  private crearTramo(tramo: Tramo): FormularioTramo {
    return this.fb.group(
      {
        horaInicio: [tramo.horaInicio, Validators.required],
        horaFin: [tramo.horaFin, Validators.required],
      },
      { validators: inicioAntesDeFin },
    );
  }
}
