import { APIRequestContext, Page, expect } from '@playwright/test';

export const MAILPIT = process.env.E2E_MAILPIT ?? 'http://localhost:8025';
export const PASSWORD = 'secreta123';

/** Negocio recién creado para un test: así no depende de la demo ni de otros tests. */
export interface NegocioDePrueba {
  slug: string;
  nombre: string;
  email: string;
  token: string;
}

/**
 * Registra un negocio y lo deja listo para reservar con la API del panel: un servicio de
 * 30 min, una profesional (Marta) que lo presta y un horario de 9:00 a 14:00 todos los días.
 * Montarlo por la API es más rápido y estable; lo que se prueba con el navegador es lo demás.
 */
export async function crearNegocio(
  request: APIRequestContext,
  ajustes: { confirmacionAutomatica?: boolean } = {},
): Promise<NegocioDePrueba> {
  const sufijo = Math.random().toString(36).slice(2, 10);
  const negocio = {
    slug: `e2e-${sufijo}`,
    nombre: `Peluquería E2E ${sufijo}`,
    email: `propietaria-${sufijo}@example.com`,
  };
  const registro = await request.post('/api/auth/registro', {
    data: {
      nombreNegocio: negocio.nombre,
      slug: negocio.slug,
      nombre: 'Propietaria',
      email: negocio.email,
      password: PASSWORD,
    },
  });
  expect(registro.status()).toBe(201);
  const token: string = (await registro.json()).accessToken;
  const headers = { Authorization: `Bearer ${token}` };

  const servicio = await (
    await request.post('/api/panel/servicios', {
      headers,
      data: { nombre: 'Corte de pelo', duracionMin: 30, margenMin: 0, precioCentimos: 1500 },
    })
  ).json();
  const marta = await (
    await request.post('/api/panel/recursos', { headers, data: { nombre: 'Marta', tipo: 'PERSONA' } })
  ).json();
  await request.put(`/api/panel/recursos/${marta.id}/servicios`, {
    headers,
    data: { servicioIds: [servicio.id] },
  });
  const franjas = [1, 2, 3, 4, 5, 6, 7].map((diaSemana) => ({
    diaSemana,
    horaInicio: '09:00',
    horaFin: '14:00',
  }));
  expect((await request.put(`/api/panel/recursos/${marta.id}/horario`, { headers, data: { franjas } })).ok()).toBe(true);

  if (ajustes.confirmacionAutomatica === false) {
    const actual = await (await request.get('/api/panel/negocio', { headers })).json();
    const cambio = await request.put('/api/panel/negocio', {
      headers,
      data: { ...actual, confirmacionAutomatica: false },
    });
    expect(cambio.ok()).toBe(true);
  }
  return { ...negocio, token };
}

/** Un día dentro de unos días (fecha local), con margen sobre la antelación mínima. */
export function diaFuturo(dias: number): Date {
  const dia = new Date();
  dia.setHours(12, 0, 0, 0);
  dia.setDate(dia.getDate() + dias);
  return dia;
}

/** "13 de noviembre de 2026": así etiqueta Material cada día del calendario en español. */
function etiquetaDia(dia: Date): string {
  return new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'long', year: 'numeric' }).format(dia);
}

/** Elige un día en un calendario de Material, pasando de mes si hace falta. */
export async function elegirDia(page: Page, dia: Date): Promise<void> {
  // Primero, que el calendario esté pintado: isVisible() no espera, y sin esto se pasaría de
  // mes solo porque el calendario aún no ha aparecido
  await page.locator('.mat-calendar-body-cell').first().waitFor();
  const celda = page.getByRole('button', { name: etiquetaDia(dia), exact: true });
  if (!(await celda.isVisible())) {
    await page.locator('.mat-calendar-next-button').click();
  }
  await celda.click();
}

/** Reserva como cliente desde la página pública y devuelve la URL de su reserva. */
export async function reservarComoCliente(page: Page, slug: string, dia: Date, email: string): Promise<string> {
  await page.goto(`/${slug}`);
  await page.getByRole('button', { name: /Corte de pelo/ }).click();
  await page.getByRole('button', { name: 'Marta' }).click();
  await elegirDia(page, dia);
  await page.getByRole('button', { name: '10:00' }).click();
  await page.getByRole('button', { name: 'Continuar' }).click();
  await page.getByLabel('Nombre').fill('Juan Cliente');
  await page.getByLabel('Email').fill(email);
  await page.getByRole('button', { name: 'Confirmar reserva' }).click();
  await page.waitForURL(/\/reserva\/.+/);
  return page.url();
}

/** Asuntos de los emails que ha recibido Mailpit para esa dirección. */
export async function asuntosPara(request: APIRequestContext, email: string): Promise<string[]> {
  const consulta = encodeURIComponent(`to:"${email}"`);
  const respuesta = await request.get(`${MAILPIT}/api/v1/search?query=${consulta}`);
  const cuerpo = (await respuesta.json()) as { messages: { Subject: string }[] };
  return cuerpo.messages.map((mensaje) => mensaje.Subject);
}

export function emailUnico(prefijo: string): string {
  return `${prefijo}-${Math.random().toString(36).slice(2, 10)}@example.com`;
}
