import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { Servicio, ServicioRequest } from '../../core/api/modelos';
import { ServiciosApiService } from '../../core/api/servicios-api.service';
import { mensajeDeError } from '../../shared/errores-http';
import { centimosAEuros, eurosACentimos } from '../../shared/precio';

/**
 * Alta y edición de un servicio. Recibe el servicio a editar (o null para crear uno),
 * guarda contra la API y se cierra devolviendo el servicio guardado.
 */
@Component({
  selector: 'app-servicio-dialogo',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './servicio-dialogo.html',
  styleUrl: '../dialogo-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ServicioDialogo {
  private readonly api = inject(ServiciosApiService);
  private readonly dialogo = inject<MatDialogRef<ServicioDialogo, Servicio>>(MatDialogRef);
  protected readonly servicio = inject<Servicio | null>(MAT_DIALOG_DATA);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nombre: [this.servicio?.nombre ?? '', [Validators.required, Validators.maxLength(120)]],
    descripcion: [this.servicio?.descripcion ?? '', Validators.maxLength(1000)],
    duracionMin: [
      this.servicio?.duracionMin ?? 30,
      [Validators.required, Validators.min(1), Validators.max(480)],
    ],
    margenMin: [
      this.servicio?.margenMin ?? 0,
      [Validators.required, Validators.min(0), Validators.max(240)],
    ],
    // El usuario trabaja en euros; la API, en céntimos
    precioEuros: [
      centimosAEuros(this.servicio?.precioCentimos ?? 0),
      [Validators.required, Validators.min(0), Validators.max(100_000)],
    ],
  });
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    const datos: ServicioRequest = {
      nombre: valores.nombre,
      descripcion: valores.descripcion || null,
      duracionMin: valores.duracionMin,
      margenMin: valores.margenMin,
      precioCentimos: eurosACentimos(valores.precioEuros),
    };

    this.guardando.set(true);
    this.error.set(null);
    const peticion = this.servicio
      ? this.api.actualizar(this.servicio.id, datos)
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
