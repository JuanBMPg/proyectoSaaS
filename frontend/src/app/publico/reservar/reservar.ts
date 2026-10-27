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
import { provideNativeDateAdapter, MAT_DATE_LOCALE } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Title } from '@angular/platform-browser';
import { Subscription } from 'rxjs';

import { NegocioPublico, ServicioPublico } from '../../core/api/modelos';
import { PublicoApiService } from '../../core/api/publico-api.service';
import { mensajeDeError } from '../../shared/errores-http';
import { fechaIso, fechaLargaEnZona } from '../../shared/fechas';
import { centimosAEuros } from '../../shared/precio';
import { HoraOfrecida, HorasDelDia, horasDelDia } from './horas';

/**
 * Página pública de un negocio (/:slug). Una sola página que se va completando de arriba
 * abajo: servicio, profesional, día y hora. Cada elección actualiza lo que hay debajo.
 */
@Component({
  selector: 'app-reservar',
  imports: [CurrencyPipe, MatButtonModule, MatDatepickerModule, MatProgressSpinnerModule],
  providers: [provideNativeDateAdapter(), { provide: MAT_DATE_LOCALE, useValue: 'es-ES' }],
  templateUrl: './reservar.html',
  styleUrl: './reservar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Reservar implements OnInit {
  private readonly api = inject(PublicoApiService);
  private readonly titulo = inject(Title);

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
