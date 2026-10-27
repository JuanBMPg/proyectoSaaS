import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeEs from '@angular/common/locales/es';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Servicio } from '../../core/api/modelos';
import { Servicios } from './servicios';

registerLocaleData(localeEs);

const SERVICIOS: Servicio[] = [
  {
    id: 's1',
    nombre: 'Corte',
    descripcion: null,
    duracionMin: 30,
    margenMin: 10,
    precioCentimos: 1500,
    activo: true,
  },
  {
    id: 's2',
    nombre: 'Mechas',
    descripcion: null,
    duracionMin: 90,
    margenMin: 0,
    precioCentimos: 4550,
    activo: false,
  },
];

describe('Servicios', () => {
  let fixture: ComponentFixture<Servicios>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Servicios],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LOCALE_ID, useValue: 'es-ES' },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Servicios);
    http = TestBed.inject(HttpTestingController);
    elemento = fixture.nativeElement;

    http.expectOne('/api/panel/servicios').flush(SERVICIOS);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('muestra cada servicio con su duración, margen y precio en euros', () => {
    const filas = elemento.querySelectorAll('tr[mat-row]');
    expect(filas.length).toBe(2);
    expect(filas[0].textContent).toContain('Corte');
    expect(filas[0].textContent).toContain('30 min');
    expect(filas[0].textContent).toContain('+ 10 de margen');
    expect(filas[0].textContent).toMatch(/15,00\s€/);
    expect(filas[1].textContent).toMatch(/45,50\s€/);
  });

  it('marca visualmente los servicios inactivos', () => {
    const filas = elemento.querySelectorAll('tr[mat-row]');
    expect(filas[0].classList).not.toContain('inactivo');
    expect(filas[1].classList).toContain('inactivo');
  });

  it('al apagar el interruptor desactiva el servicio con DELETE y recarga', async () => {
    elemento.querySelector<HTMLButtonElement>('mat-slide-toggle button')!.click();
    await fixture.whenStable();

    const peticion = http.expectOne('/api/panel/servicios/s1');
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush(null);
    http.expectOne('/api/panel/servicios').flush(SERVICIOS);
  });

  it('al encender el interruptor reactiva el servicio con PUT y activo = true', async () => {
    elemento.querySelectorAll<HTMLButtonElement>('mat-slide-toggle button')[1].click();
    await fixture.whenStable();

    const peticion = http.expectOne('/api/panel/servicios/s2');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body.activo).toBe(true);
    peticion.flush({ ...SERVICIOS[1], activo: true });
    http.expectOne('/api/panel/servicios').flush(SERVICIOS);
  });
});
