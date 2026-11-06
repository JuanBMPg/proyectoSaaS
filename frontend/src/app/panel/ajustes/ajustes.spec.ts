import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Negocio } from '../../core/api/modelos';
import { Ajustes } from './ajustes';

const LOLA: Negocio = {
  id: 'n1',
  nombre: 'Peluquería Lola',
  slug: 'peluqueria-lola',
  zonaHoraria: 'Europe/Madrid',
  email: null,
  telefono: '600123123',
  direccion: 'C/ Mayor 12',
  etiquetaRecurso: 'Profesional',
  antelacionMinimaMin: 120,
  antelacionMaximaDias: 30,
  plazoCancelacionHoras: 24,
  confirmacionAutomatica: true,
};

describe('Ajustes', () => {
  let fixture: ComponentFixture<Ajustes>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Ajustes],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(Ajustes);
    http = TestBed.inject(HttpTestingController);
    elemento = fixture.nativeElement;
    http.expectOne('/api/panel/negocio').flush(LOLA);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function campo(nombre: string): HTMLInputElement {
    return elemento.querySelector<HTMLInputElement>(`[formcontrolname="${nombre}"]`)!;
  }

  function escribir(nombre: string, valor: string): void {
    campo(nombre).value = valor;
    campo(nombre).dispatchEvent(new Event('input'));
  }

  it('muestra los datos actuales del negocio y su página pública', () => {
    expect(campo('nombre').value).toBe('Peluquería Lola');
    expect(campo('telefono').value).toBe('600123123');
    expect(campo('plazoCancelacionHoras').value).toBe('24');
    expect(elemento.textContent).toContain('/peluqueria-lola');
    expect(elemento.textContent).toContain('quedan confirmadas al momento');
  });

  it('activar la confirmación manual explica qué cambia y se guarda', async () => {
    elemento.querySelector<HTMLButtonElement>('mat-slide-toggle button')!.click();
    await fixture.whenStable();
    expect(elemento.textContent).toContain('llegan como solicitudes');

    escribir('direccion', '   ');
    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));

    const peticion = http.expectOne('/api/panel/negocio');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toMatchObject({
      nombre: 'Peluquería Lola',
      email: null,
      direccion: null,
      confirmacionAutomatica: false,
    });
    peticion.flush({ ...LOLA, direccion: null, confirmacionAutomatica: false });
  });

  it('no envía reglas fuera de rango', () => {
    escribir('antelacionMaximaDias', '0');
    elemento.querySelector('form')!.dispatchEvent(new Event('submit'));

    http.expectNone('/api/panel/negocio');
  });
});
