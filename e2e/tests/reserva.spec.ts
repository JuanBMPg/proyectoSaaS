import { expect, test } from '@playwright/test';

import {
  asuntosPara,
  crearNegocio,
  diaFuturo,
  elegirDia,
  emailUnico,
  reservarComoCliente,
} from './negocio';

test('un cliente reserva desde la web, recibe el email y cancela desde su enlace', async ({
  page,
  request,
}) => {
  const negocio = await crearNegocio(request);
  const email = emailUnico('cliente');
  const dia = diaFuturo(3);

  const enlace = await reservarComoCliente(page, negocio.slug, dia, email);

  // La página de la reserva, que es la misma que enlaza el email
  await expect(page.getByRole('status')).toHaveText('Cita confirmada');
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Corte de pelo');
  await expect(page.getByText('10:00 – 10:30')).toBeVisible();

  // El email sale por la cola (outbox): puede tardar unos segundos
  await expect
    .poll(() => asuntosPara(request, email), { timeout: 30_000 })
    .toEqual([expect.stringMatching(/^Tu cita en Peluquería E2E/)]);

  // La web ya no ofrece esa hora a nadie más
  await page.goto(`/${negocio.slug}`);
  await page.getByRole('button', { name: /Corte de pelo/ }).click();
  await page.getByRole('button', { name: 'Marta' }).click();
  await elegirDia(page, dia);
  await expect(page.getByRole('button', { name: '10:30' })).toBeVisible();
  await expect(page.getByRole('button', { name: '10:00', exact: true })).toHaveCount(0);

  // Cancelar desde el enlace, con su paso de confirmación
  await page.goto(enlace);
  await page.getByRole('button', { name: 'Cancelar la cita' }).click();
  await page.getByRole('button', { name: 'Sí, cancelar la cita' }).click();
  await expect(page.getByRole('status')).toHaveText('Cita cancelada');
  await expect(page.getByText('la hora queda libre para otra persona')).toBeVisible();

  await expect
    .poll(() => asuntosPara(request, email), { timeout: 30_000 })
    .toContainEqual(expect.stringMatching(/^Cita cancelada: Peluquería E2E/));
});
