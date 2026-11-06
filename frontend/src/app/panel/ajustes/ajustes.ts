import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';

import { Negocio } from '../../core/api/modelos';
import { NegocioApiService } from '../../core/api/negocio-api.service';
import { erroresDeCampo, mensajeDeError } from '../../shared/errores-http';

/**
 * Datos del negocio (los que ven los clientes en la web y en los emails) y sus reglas de
 * reserva, incluida la confirmación manual. Solo para el propietario.
 */
@Component({
  selector: 'app-ajustes',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
  ],
  templateUrl: './ajustes.html',
  styleUrl: './ajustes.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Ajustes {
  private readonly api = inject(NegocioApiService);
  private readonly avisos = inject(MatSnackBar);

  protected readonly negocio = signal<Negocio | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.email, Validators.maxLength(160)]],
    telefono: ['', Validators.maxLength(30)],
    direccion: ['', Validators.maxLength(255)],
    etiquetaRecurso: ['', [Validators.required, Validators.maxLength(40)]],
    antelacionMinimaMin: [120, [Validators.required, Validators.min(0), Validators.max(10080)]],
    antelacionMaximaDias: [30, [Validators.required, Validators.min(1), Validators.max(365)]],
    plazoCancelacionHoras: [24, [Validators.required, Validators.min(0), Validators.max(720)]],
    confirmacionAutomatica: [true],
  });

  constructor() {
    this.api.obtener().subscribe({
      next: (negocio) => this.cargar(negocio),
      error: (error: unknown) => this.error.set(mensajeDeError(error)),
    });
  }

  protected guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const valores = this.formulario.getRawValue();
    this.guardando.set(true);
    this.error.set(null);
    this.api
      .actualizar({
        ...valores,
        nombre: valores.nombre.trim(),
        email: valores.email.trim() || null,
        telefono: valores.telefono.trim() || null,
        direccion: valores.direccion.trim() || null,
        etiquetaRecurso: valores.etiquetaRecurso.trim(),
      })
      .subscribe({
        next: (negocio) => {
          this.cargar(negocio);
          this.guardando.set(false);
          this.avisos.open('Ajustes guardados', undefined, { duration: 3000 });
        },
        error: (error: unknown) => {
          const campos = erroresDeCampo(error);
          this.error.set(
            Object.keys(campos).length > 0
              ? 'Revisa los campos: ' + Object.values(campos).join(', ')
              : mensajeDeError(error),
          );
          this.guardando.set(false);
        },
      });
  }

  private cargar(negocio: Negocio): void {
    this.negocio.set(negocio);
    this.formulario.reset({
      nombre: negocio.nombre,
      email: negocio.email ?? '',
      telefono: negocio.telefono ?? '',
      direccion: negocio.direccion ?? '',
      etiquetaRecurso: negocio.etiquetaRecurso,
      antelacionMinimaMin: negocio.antelacionMinimaMin,
      antelacionMaximaDias: negocio.antelacionMaximaDias,
      plazoCancelacionHoras: negocio.plazoCancelacionHoras,
      confirmacionAutomatica: negocio.confirmacionAutomatica,
    });
  }
}
