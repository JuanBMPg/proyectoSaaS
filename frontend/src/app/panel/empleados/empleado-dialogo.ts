import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { Empleado, Recurso } from '../../core/api/modelos';
import { EmpleadosApiService } from '../../core/api/empleados-api.service';
import { mensajeDeError } from '../../shared/errores-http';

/** Recursos que todavía no tienen empleado: son los que se pueden asignar. */
export interface DatosEmpleado {
  recursosLibres: Recurso[];
}

/** Alta de un empleado con una contraseña inicial que el propietario le comunica. */
@Component({
  selector: 'app-empleado-dialogo',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './empleado-dialogo.html',
  styleUrl: '../dialogo-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmpleadoDialogo {
  private readonly api = inject(EmpleadosApiService);
  private readonly dialogo = inject<MatDialogRef<EmpleadoDialogo, Empleado>>(MatDialogRef);
  protected readonly datos = inject<DatosEmpleado>(MAT_DIALOG_DATA);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    recursoId: ['', Validators.required],
  });
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    this.guardando.set(true);
    this.error.set(null);
    this.api
      .crear({ ...valores, nombre: valores.nombre.trim(), email: valores.email.trim() })
      .subscribe({
        next: (creado) => this.dialogo.close(creado),
        error: (error: unknown) => {
          this.error.set(mensajeDeError(error));
          this.guardando.set(false);
        },
      });
  }
}
