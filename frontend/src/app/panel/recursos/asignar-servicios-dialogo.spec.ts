import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { Recurso, Servicio } from '../../core/api/modelos';
import { AsignarServiciosDialogo, DatosAsignacion } from './asignar-servicios-dialogo';

function servicio(id: string, nombre: string, activo = true): Servicio {
  return {
    id,
    nombre,
    descripcion: null,
    duracionMin: 30,
    margenMin: 0,
    precioCentimos: 0,
    activo,
  };
}

const MARTA: Recurso = {
  id: 'r1',
  nombre: 'Marta',
  tipo: 'PERSONA',
  activo: true,
  servicioIds: ['corte', 'antiguo'],
};

const DATOS: DatosAsignacion = {
  recurso: MARTA,
  servicios: [
    servicio('corte', 'Corte'),
    servicio('tinte', 'Tinte'),
    servicio('antiguo', 'Permanente', false),
    servicio('retirado', 'Alisado', false),
  ],
};

describe('AsignarServiciosDialogo', () => {
  let fixture: ComponentFixture<AsignarServiciosDialogo>;
  let http: HttpTestingController;
  let elemento: HTMLElement;
  let cerrar: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    cerrar = vi.fn();
    await TestBed.configureTestingModule({
      imports: [AsignarServiciosDialogo],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MAT_DIALOG_DATA, useValue: DATOS },
        { provide: MatDialogRef, useValue: { close: cerrar } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(AsignarServiciosDialogo);
    http = TestBed.inject(HttpTestingController);
    elemento = fixture.nativeElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function casillas(): HTMLInputElement[] {
    return [...elemento.querySelectorAll<HTMLInputElement>('mat-checkbox input')];
  }

  it('ofrece los activos y los inactivos ya asignados, con los asignados marcados', () => {
    const textos = [...elemento.querySelectorAll('mat-checkbox')]
      .map((c) => c.textContent)
      .join('|');
    expect(textos).toContain('Corte');
    expect(textos).toContain('Tinte');
    expect(textos).toContain('Permanente');
    // Inactivo y sin asignar: no se ofrece
    expect(textos).not.toContain('Alisado');

    expect(casillas().map((c) => c.checked)).toEqual([true, false, true]);
  });

  it('envía el conjunto completo de servicios marcados', async () => {
    casillas()[1].click(); // marca Tinte
    casillas()[2].click(); // desmarca Permanente
    await fixture.whenStable();

    elemento.querySelector<HTMLButtonElement>('mat-dialog-actions button:last-child')!.click();
    const peticion = http.expectOne('/api/panel/recursos/r1/servicios');
    expect(peticion.request.method).toBe('PUT');
    expect([...peticion.request.body.servicioIds].sort()).toEqual(['corte', 'tinte']);
    peticion.flush({ ...MARTA, servicioIds: ['corte', 'tinte'] });

    expect(cerrar).toHaveBeenCalledWith(
      expect.objectContaining({ servicioIds: ['corte', 'tinte'] }),
    );
  });
});
