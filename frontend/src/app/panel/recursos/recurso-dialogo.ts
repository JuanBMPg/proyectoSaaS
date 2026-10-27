import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';

import { Recurso, RecursoRequest, TipoRecurso } from '../../core/api/modelos';
import { RecursosApiService } from '../../core/api/recursos-api.service';
import { mensajeDeError } from '../../shared/errores-http';

/** Alta y edición de un recurso (nombre y tipo). Los servicios se asignan en otro diálogo. */
@Component({
  selector: 'app-recurso-dialogo',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
  ],
  templateUrl: './recurso-dialogo.html',
  styleUrl: '../dialogo-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecursoDialogo {
  private readonly api = inject(RecursosApiService);
  private readonly dialogo = inject<MatDialogRef<RecursoDialogo, Recurso>>(MatDialogRef);
  protected readonly recurso = inject<Recurso | null>(MAT_DIALOG_DATA);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nombre: [this.recurso?.nombre ?? '', [Validators.required, Validators.maxLength(120)]],
    tipo: [this.recurso?.tipo ?? ('PERSONA' as TipoRecurso), Validators.required],
  });
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const datos: RecursoRequest = this.formulario.getRawValue();

    this.guardando.set(true);
    this.error.set(null);
    const peticion = this.recurso
      ? this.api.actualizar(this.recurso.id, datos)
      : this.api.crear(datos);
    peticion.subscribe({
      next: (guardado) => this.dialogo.close(guardado),
      error: (error: unknown) => {
        this.error.set(mensajeDeError(error));
        this.guardando.set(false);
      },
    });
  }
}
