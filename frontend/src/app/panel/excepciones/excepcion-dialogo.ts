import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';

import { ExcepcionCreada, Recurso } from '../../core/api/modelos';
import { ExcepcionesApiService } from '../../core/api/excepciones-api.service';
import { mensajeDeError } from '../../shared/errores-http';
import { fechaIso } from '../../shared/fechas';

export interface DatosExcepcion {
  /** Recursos activos que se pueden elegir (además de "todo el negocio"). */
  recursos: Recurso[];
}

/** Valor del selector "Afecta a" que significa todo el negocio (recursoId null en la API). */
export const TODO_EL_NEGOCIO = '';

/** Con horario especial, las dos horas son obligatorias y la de inicio va antes. */
function horasValidas(grupo: AbstractControl): ValidationErrors | null {
  const { tipo, horaInicio, horaFin } = grupo.value as {
    tipo: string;
    horaInicio: string;
    horaFin: string;
  };
  if (tipo !== 'especial') {
    return null;
  }
  if (!horaInicio || !horaFin) {
    return { horasIncompletas: true };
  }
  return horaInicio < horaFin ? null : { horasInvertidas: true };
}

/** Alta de unas vacaciones, un festivo o un horario especial. */
@Component({
  selector: 'app-excepcion-dialogo',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    MatSelectModule,
  ],
  providers: [provideNativeDateAdapter(), { provide: MAT_DATE_LOCALE, useValue: 'es-ES' }],
  templateUrl: './excepcion-dialogo.html',
  styleUrl: '../dialogo-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExcepcionDialogo {
  private readonly api = inject(ExcepcionesApiService);
  private readonly dialogo = inject<MatDialogRef<ExcepcionDialogo, ExcepcionCreada>>(MatDialogRef);
  protected readonly datos = inject<DatosExcepcion>(MAT_DIALOG_DATA);

  protected readonly formulario = inject(NonNullableFormBuilder).group(
    {
      recursoId: [TODO_EL_NEGOCIO],
      // Date | null: el selector de rango los deja a null mientras no se elige
      fechaInicio: this.fechaVacia(),
      fechaFin: this.fechaVacia(),
      tipo: ['cierre' as 'cierre' | 'especial'],
      horaInicio: [''],
      horaFin: [''],
      motivo: ['', [Validators.required, Validators.maxLength(120)]],
    },
    { validators: horasValidas },
  );
  protected readonly tipo = toSignal(this.formulario.controls.tipo.valueChanges, {
    initialValue: this.formulario.controls.tipo.value,
  });
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly TODO_EL_NEGOCIO = TODO_EL_NEGOCIO;

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    const especial = valores.tipo === 'especial';
    this.guardando.set(true);
    this.error.set(null);
    this.api
      .crear({
        recursoId: valores.recursoId === TODO_EL_NEGOCIO ? null : valores.recursoId,
        fechaInicio: fechaIso(valores.fechaInicio!),
        fechaFin: fechaIso(valores.fechaFin!),
        horaInicio: especial ? valores.horaInicio : null,
        horaFin: especial ? valores.horaFin : null,
        motivo: valores.motivo.trim(),
      })
      .subscribe({
        next: (creada) => this.dialogo.close(creada),
        error: (error: unknown) => {
          this.error.set(mensajeDeError(error));
          this.guardando.set(false);
        },
      });
  }

  private fechaVacia() {
    return inject(NonNullableFormBuilder).control<Date | null>(null, Validators.required);
  }
}
