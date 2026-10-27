import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { NegocioPublico, ServicioPublico } from '../../core/api/modelos';
import { PublicoApiService } from '../../core/api/publico-api.service';
import { erroresDeCampo, mensajeDeError } from '../../shared/errores-http';
import { fechaIso, fechaLargaEnZona } from '../../shared/fechas';
import { centimosAEuros } from '../../shared/precio';
import { HoraOfrecida, HorasDelDia, horasDelDia } from './horas';

/** Mismo patrón que valida el backend: números, espacios y + ( ) -. */
const PATRON_TELEFONO = /^[0-9+()\s-]{6,30}$/;

/**
 * Página pública de un negocio (/:slug). Una sola página que se va completando de arriba
 * abajo: servicio, profesional, día, hora y, al final, los datos del cliente para confirmar.
 */
@Component({
  selector: 'app-reservar',
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  providers: [provideNativeDateAdapter(), { provide: MAT_DATE_LOCALE, useValue: 'es-ES' }],
  templateUrl: './reservar.html',
  styleUrl: './reservar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Reservar implements OnInit {
  private readonly api = inject(PublicoApiService);
  private readonly titulo = inject(Title);
  private readonly router = inject(Router);

  /** Slug del negocio, tomado de la URL. */
  readonly slug = input.required<string>();

  protected readonly negocio = signal<NegocioPublico | null>(null);
  protected readonly estado = signal<'cargando' | 'listo' | 'no-existe' | 'error'>('cargando');
  protected readonly error = signal<string | null>(null);

  protected readonly servicio = signal<ServicioPublico | null>(null);
  /** null = "Sin preferencia". */
  protected readonly recursoId = signal<string | null>(null);
  protected readonly fecha = signal<Date | null>(null);
  protected readonly horas = signal<HorasDelDia | null>(null);
  protected readonly cargandoHoras = signal(false);
  protected readonly horaElegida = signal<HoraOfrecida | null>(null);

  /** Formulario de datos: aparece al pulsar "Continuar" y se conserva si hay que cambiar de hora. */
  protected readonly mostrarFormulario = signal(false);
  protected readonly enviando = signal(false);
  protected readonly errorReserva = signal<string | null>(null);
  protected readonly datos = inject(NonNullableFormBuilder).group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(160)]],
    telefono: ['', Validators.pattern(PATRON_TELEFONO)],
    notas: ['', Validators.maxLength(500)],
  });

  protected readonly centimosAEuros = centimosAEuros;
  protected readonly hoy = new Date();
  protected readonly ultimoDia = computed(() => {
    const ultimo = new Date();
    ultimo.setDate(ultimo.getDate() + (this.negocio()?.antelacionMaximaDias ?? 30));
    return ultimo;
  });

  /** Profesionales que prestan el servicio elegido. */
  protected readonly recursosDelServicio = computed(() => {
    const servicio = this.servicio();
    const negocio = this.negocio();
    if (!servicio || !negocio) {
      return [];
    }
    return negocio.recursos.filter((recurso) => servicio.recursoIds.includes(recurso.id));
  });

  protected readonly sinHoras = computed(() => {
    const horas = this.horas();
    return horas !== null && horas.manana.length === 0 && horas.tarde.length === 0;
  });

  protected readonly resumen = computed(() => {
    const negocio = this.negocio();
    const servicio = this.servicio();
    const hora = this.horaElegida();
    if (!negocio || !servicio || !hora) {
      return null;
    }
    const recurso = negocio.recursos.find((r) => r.id === hora.recursoId);
    return {
      servicio: servicio.nombre,
      recurso: recurso?.nombre ?? '',
      dia: fechaLargaEnZona(hora.inicio, negocio.zonaHoraria),
      hora: hora.etiqueta,
      precioCentimos: servicio.precioCentimos,
    };
  });

  private peticionHoras: Subscription | null = null;

  ngOnInit(): void {
    this.api.negocio(this.slug()).subscribe({
      next: (negocio) => {
        this.negocio.set(negocio);
        this.titulo.setTitle(`${negocio.nombre} · Reservar cita`);
        this.estado.set('listo');
      },
      error: (error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 404) {
          this.estado.set('no-existe');
        } else {
          this.error.set(mensajeDeError(error));
          this.estado.set('error');
        }
      },
    });
  }

  /** Cambiar de servicio reinicia el profesional y la hora: puede que ya no apliquen. */
  protected elegirServicio(servicio: ServicioPublico): void {
    this.servicio.set(servicio);
    this.recursoId.set(null);
    this.cargarHoras();
  }

  protected elegirRecurso(recursoId: string | null): void {
    this.recursoId.set(recursoId);
    this.cargarHoras();
  }

  protected elegirFecha(fecha: Date | null): void {
    this.fecha.set(fecha);
    this.cargarHoras();
  }

  protected elegirHora(hora: HoraOfrecida): void {
    this.horaElegida.set(hora);
    this.errorReserva.set(null);
  }

  protected continuar(): void {
    this.mostrarFormulario.set(true);
  }

  protected confirmar(): void {
    const servicio = this.servicio();
    const hora = this.horaElegida();
    if (!servicio || !hora) {
      return;
    }
    if (this.datos.invalid) {
      this.datos.markAllAsTouched();
      return;
    }
    const { nombre, email, telefono, notas } = this.datos.getRawValue();

    this.enviando.set(true);
    this.errorReserva.set(null);
    this.api
      .crearReserva(this.slug(), {
        servicioId: servicio.id,
        recursoId: hora.recursoId,
        inicio: hora.inicio,
        nombre: nombre.trim(),
        email: email.trim(),
        telefono: telefono.trim() || null,
        notas: notas.trim() || null,
      })
      .subscribe({
        next: ({ token }) => void this.router.navigate(['/reserva', token]),
        error: (error: unknown) => this.tratarErrorReserva(error),
      });
  }

  /**
   * 409: otra persona se ha quedado el hueco. Se recargan las horas para que elija otra,
   * sin perder los datos que ya ha escrito. 400: errores bajo cada campo. Resto: aviso.
   */
  private tratarErrorReserva(error: unknown): void {
    this.enviando.set(false);
    if (error instanceof HttpErrorResponse && error.status === 409) {
      this.errorReserva.set('Ese hueco acaba de ocuparse. Elige otra hora.');
      this.cargarHoras();
      return;
    }
    const porCampo = erroresDeCampo(error);
    const campos = Object.entries(porCampo).filter(([campo]) => this.datos.get(campo));
    for (const [campo, mensaje] of campos) {
      this.datos.get(campo)?.setErrors({ servidor: mensaje });
      this.datos.get(campo)?.markAsTouched();
    }
    if (campos.length === 0) {
      this.errorReserva.set(mensajeDeError(error));
    }
  }

  /**
   * Pide los huecos del día con las elecciones actuales. Si el cliente cambia de opción
   * antes de que llegue la respuesta, se cancela la petición anterior para que una
   * respuesta vieja no pise a la nueva.
   */
  private cargarHoras(): void {
    this.horaElegida.set(null);
    this.horas.set(null);
    this.peticionHoras?.unsubscribe();

    const negocio = this.negocio();
    const servicio = this.servicio();
    const fecha = this.fecha();
    if (!negocio || !servicio || !fecha) {
      return;
    }

    this.cargandoHoras.set(true);
    this.peticionHoras = this.api
      .disponibilidad(negocio.slug, servicio.id, fechaIso(fecha), this.recursoId())
      .subscribe({
        next: (disponibilidad) => {
          this.horas.set(horasDelDia(disponibilidad.huecos, disponibilidad.zonaHoraria));
          this.cargandoHoras.set(false);
        },
        error: (error: unknown) => {
          this.error.set(mensajeDeError(error));
          this.cargandoHoras.set(false);
        },
      });
  }
}
