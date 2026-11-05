import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { AgendaApiService } from '../../core/api/agenda-api.service';
import { NegocioPublico, ReservaPanel } from '../../core/api/modelos';
import { PublicoApiService } from '../../core/api/publico-api.service';
import { mensajeDeError } from '../../shared/errores-http';
import { instanteEnZona } from './agenda-utils';

export interface DatosReservaManual {
  slug: string;
  zona: string;
  /** Día propuesto ("YYYY-MM-DD"), p. ej. el que se está viendo en la agenda. */
  fecha: string;
}

/**
 * Reserva hecha por el personal (p. ej. por teléfono). El negocio puede reservar a cualquier
 * hora; lo único que el backend no permite es solapar otra reserva (409).
 * Los servicios y quién los presta se leen de la API pública del negocio.
 */
@Component({
  selector: 'app-reserva-manual-dialogo',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './reserva-manual-dialogo.html',
  styleUrl: '../dialogo-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReservaManualDialogo implements OnInit {
  private readonly api = inject(AgendaApiService);
  private readonly publico = inject(PublicoApiService);
  private readonly dialogo = inject<MatDialogRef<ReservaManualDialogo, ReservaPanel>>(MatDialogRef);
  private readonly datos = inject<DatosReservaManual>(MAT_DIALOG_DATA);

  protected readonly negocio = signal<NegocioPublico | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly formulario = inject(NonNullableFormBuilder).group({
    servicioId: ['', Validators.required],
    recursoId: ['', Validators.required],
    fecha: [this.datos.fecha, Validators.required],
    hora: ['10:00', Validators.required],
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    telefono: ['', Validators.pattern(/^[0-9+()\s-]{6,30}$/)],
    notas: ['', Validators.maxLength(500)],
  });

  private readonly servicioElegido = toSignal(this.formulario.controls.servicioId.valueChanges, {
    initialValue: '',
  });

  /** Solo los recursos que prestan el servicio elegido. */
  protected readonly recursos = computed(() => {
    const negocio = this.negocio();
    const servicio = negocio?.servicios.find((s) => s.id === this.servicioElegido());
    return negocio && servicio
      ? negocio.recursos.filter((r) => servicio.recursoIds.includes(r.id))
      : [];
  });

  ngOnInit(): void {
    this.publico.negocio(this.datos.slug).subscribe({
      next: (negocio) => this.negocio.set(negocio),
      error: (error: unknown) => this.error.set(mensajeDeError(error)),
    });
    // Al cambiar de servicio, el recurso elegido puede dejar de valer
    this.formulario.controls.servicioId.valueChanges.subscribe(() =>
      this.formulario.controls.recursoId.setValue(''),
    );
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
      .crear({
        servicioId: valores.servicioId,
        recursoId: valores.recursoId,
        inicio: instanteEnZona(valores.fecha, valores.hora, this.datos.zona),
        nombre: valores.nombre.trim(),
        email: valores.email.trim(),
        telefono: valores.telefono.trim() || null,
        notas: valores.notas.trim() || null,
      })
      .subscribe({
        next: (creada) => this.dialogo.close(creada),
        error: (error: unknown) => {
          this.error.set(
            error instanceof HttpErrorResponse && error.status === 409
              ? 'Esa hora se solapa con otra reserva de ese profesional.'
              : mensajeDeError(error),
          );
          this.guardando.set(false);
        },
      });
  }
}
