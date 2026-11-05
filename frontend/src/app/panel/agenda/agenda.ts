import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { CalendarOptions, FullCalendarComponent, FullCalendarModule } from '@fullcalendar/angular';
import dayGridPlugin from '@fullcalendar/angular/daygrid';
import listPlugin from '@fullcalendar/angular/list';
// El paquete de Angular 7.1.4 declara los idiomas pero no los incluye: se toman del paquete base
import esLocale from 'fullcalendar/locales/es';
import themePlugin from '@fullcalendar/angular/themes/monarch';
import timeGridPlugin from '@fullcalendar/angular/timegrid';

import { AgendaApiService } from '../../core/api/agenda-api.service';
import { Negocio, Recurso, ReservaPanel } from '../../core/api/modelos';
import { NegocioApiService } from '../../core/api/negocio-api.service';
import { RecursosApiService } from '../../core/api/recursos-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { mensajeDeError } from '../../shared/errores-http';
import { fechaIso } from '../../shared/fechas';
import { aEventos } from './agenda-utils';
import { DatosDetalle, DetalleReservaDialogo } from './detalle-reserva-dialogo';
import { DatosReservaManual, ReservaManualDialogo } from './reserva-manual-dialogo';

/**
 * Agenda del panel con FullCalendar. El propietario ve todo el negocio y puede filtrar por
 * recurso; un empleado ve solo su recurso (lo decide el backend).
 * Las horas se muestran en la zona del NEGOCIO, no en la del navegador.
 */
@Component({
  selector: 'app-agenda',
  imports: [
    FullCalendarModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatSelectModule,
  ],
  templateUrl: './agenda.html',
  styleUrl: './agenda.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Agenda implements OnInit {
  private readonly api = inject(AgendaApiService);
  private readonly auth = inject(AuthService);
  private readonly negocioApi = inject(NegocioApiService);
  private readonly recursosApi = inject(RecursosApiService);
  private readonly dialogos = inject(MatDialog);
  private readonly movil = inject(BreakpointObserver).isMatched([
    Breakpoints.XSmall,
    Breakpoints.Small,
  ]);

  private readonly calendario = viewChild(FullCalendarComponent);

  protected readonly esPropietario = computed(() => this.auth.rol() === 'PROPIETARIO');
  protected readonly negocio = signal<Negocio | null>(null);
  protected readonly recursos = signal<Recurso[]>([]);
  /** null = todos los recursos (solo propietario). */
  protected readonly recursoFiltro = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);

  /** Las opciones dependen de la zona del negocio: se crean cuando se conoce. */
  protected readonly opciones = computed<CalendarOptions | null>(() => {
    const negocio = this.negocio();
    if (!negocio) {
      return null;
    }
    return {
      plugins: [themePlugin, timeGridPlugin, dayGridPlugin, listPlugin],
      locale: esLocale,
      timeZone: negocio.zonaHoraria,
      initialView: this.movil ? 'timeGridDay' : 'timeGridWeek',
      headerToolbar: {
        start: 'prev,next today',
        center: 'title',
        end: 'timeGridDay,timeGridWeek,dayGridMonth,listWeek',
      },
      slotMinTime: '07:00:00',
      slotMaxTime: '22:00:00',
      allDaySlot: false,
      nowIndicator: true,
      height: 'auto',
      // FullCalendar pide los eventos del rango visible cada vez que cambia de semana o vista
      events: (rango, exito, fallo) =>
        this.api
          .listar(rango.start.toISOString(), rango.end.toISOString(), this.recursoFiltro())
          .subscribe({
            next: (reservas) => exito(aEventos(reservas, this.verTodos())),
            error: (error: unknown) => {
              this.error.set(mensajeDeError(error));
              fallo(error as Error);
            },
          }),
      eventClick: (clic) => this.abrirDetalle(clic.event.extendedProps['reserva'] as ReservaPanel),
    };
  });

  ngOnInit(): void {
    this.negocioApi.obtener().subscribe({
      next: (negocio) => this.negocio.set(negocio),
      error: (error: unknown) => this.error.set(mensajeDeError(error)),
    });
    if (this.esPropietario()) {
      this.recursosApi
        .listar()
        .subscribe((recursos) => this.recursos.set(recursos.filter((r) => r.activo)));
    }
  }

  protected filtrar(recursoId: string | null): void {
    this.recursoFiltro.set(recursoId);
    this.recargar();
  }

  protected nuevaReserva(): void {
    const negocio = this.negocio();
    if (!negocio) {
      return;
    }
    const diaVisible = this.calendario()?.getApi().getDate() ?? new Date();
    this.dialogos
      .open<ReservaManualDialogo, DatosReservaManual, ReservaPanel>(ReservaManualDialogo, {
        data: { slug: negocio.slug, zona: negocio.zonaHoraria, fecha: fechaIso(diaVisible) },
        width: '520px',
      })
      .afterClosed()
      .subscribe((creada) => creada && this.recargar());
  }

  private abrirDetalle(reserva: ReservaPanel): void {
    const negocio = this.negocio();
    if (!negocio) {
      return;
    }
    this.dialogos
      .open<DetalleReservaDialogo, DatosDetalle, ReservaPanel>(DetalleReservaDialogo, {
        data: { reserva, zona: negocio.zonaHoraria },
        width: '440px',
      })
      .afterClosed()
      .subscribe((cambiada) => cambiada && this.recargar());
  }

  /** El nombre del recurso en el título solo hace falta cuando se ve todo el negocio. */
  private verTodos(): boolean {
    return this.esPropietario() && this.recursoFiltro() === null;
  }

  private recargar(): void {
    this.calendario()?.getApi().refetchEvents();
  }
}
