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
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';

import { EstadoReserva, ReservaPublica } from '../../core/api/modelos';
import { PublicoApiService } from '../../core/api/publico-api.service';
import { mensajeDeError } from '../../shared/errores-http';
import { fechaLargaEnZona, horaEnZona } from '../../shared/fechas';
import { centimosAEuros } from '../../shared/precio';

const TEXTO_ESTADO: Record<EstadoReserva, string> = {
  CONFIRMADA: 'Cita confirmada',
  PENDIENTE: 'Pendiente de confirmar por el negocio',
  CANCELADA: 'Cita cancelada',
  COMPLETADA: 'Cita realizada',
  NO_PRESENTADO: 'No se presentó',
};

/**
 * Una reserva vista desde su enlace secreto (/reserva/:token). Es la página a la que se llega
 * al confirmar y la que enlazará el email de confirmación. El token de la URL es la llave:
 * no hace falta cuenta.
 */
@Component({
  selector: 'app-ver-reserva',
  imports: [CurrencyPipe, MatButtonModule, MatProgressSpinnerModule, RouterLink],
  templateUrl: './ver-reserva.html',
  styleUrl: './ver-reserva.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VerReserva implements OnInit {
  private readonly api = inject(PublicoApiService);
  private readonly titulo = inject(Title);

  readonly token = input.required<string>();

  protected readonly reserva = signal<ReservaPublica | null>(null);
  protected readonly estado = signal<'cargando' | 'listo' | 'no-existe' | 'error'>('cargando');
  protected readonly error = signal<string | null>(null);
  protected readonly centimosAEuros = centimosAEuros;

  /** Fecha y hora en la zona del negocio, que es donde hay que presentarse. */
  protected readonly cuando = computed(() => {
    const reserva = this.reserva();
    if (!reserva) {
      return null;
    }
    const zona = reserva.negocio.zonaHoraria;
    return {
      dia: fechaLargaEnZona(reserva.inicio, zona),
      desde: horaEnZona(reserva.inicio, zona),
      hasta: horaEnZona(reserva.fin, zona),
    };
  });

  protected readonly textoEstado = computed(() => {
    const reserva = this.reserva();
    return reserva ? TEXTO_ESTADO[reserva.estado] : '';
  });

  ngOnInit(): void {
    this.api.reserva(this.token()).subscribe({
      next: (reserva) => {
        this.reserva.set(reserva);
        this.titulo.setTitle(`Tu cita en ${reserva.negocio.nombre}`);
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
}
