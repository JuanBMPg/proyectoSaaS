import { Page, expect, test } from '@playwright/test';

import {
  NegocioDePrueba,
  PASSWORD,
  crearNegocio,
  diaFuturo,
  elegirDia,
  emailUnico,
  reservarComoCliente,
} from './negocio';

async function entrarAlPanel(page: Page, negocio: NegocioDePrueba): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('Email').fill(negocio.email);
  await page.getByLabel('Contraseña').fill(PASSWORD);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await page.waitForURL(/\/panel/);
}

test('un festivo creado en el panel quita las horas de la web pública', async ({
  page,
  request,
}) => {
  const negocio = await crearNegocio(request);
  const dia = diaFuturo(4);

  await entrarAlPanel(page, negocio);
  await page.getByRole('link', { name: 'Vacaciones y festivos' }).click();
  await page.getByRole('button', { name: 'Nueva excepción' }).click();

  // Un solo día: se elige el mismo día como inicio y fin del rango
  const dialogo = page.getByRole('dialog');
  await dialogo.getByRole('button', { name: /calendar|calendario/i }).click();
  await elegirDia(page, dia);
  await elegirDia(page, dia);
  await dialogo.getByLabel('Motivo').fill('Fiesta local');
  await dialogo.getByRole('button', { name: 'Guardar' }).click();
  await expect(page.getByRole('row', { name: /Fiesta local/ })).toContainText('Cerrado');

  // La web pública ya no ofrece horas ese día
  await page.goto(`/${negocio.slug}`);
  await page.getByRole('button', { name: /Corte de pelo/ }).click();
  await elegirDia(page, dia);
  await expect(page.getByText('No quedan horas libres este día')).toBeVisible();
});

test('con confirmación manual, la reserva espera hasta que el negocio la confirma', async ({
  page,
  request,
}) => {
  const negocio = await crearNegocio(request, { confirmacionAutomatica: false });

  const enlace = await reservarComoCliente(page, negocio.slug, diaFuturo(5), emailUnico('pendiente'));
  await expect(page.getByRole('status')).toHaveText('Pendiente de confirmar por el negocio');

  // La propietaria ve la solicitud en el inicio del panel y la confirma
  await entrarAlPanel(page, negocio);
  const solicitudes = page.getByRole('region', { name: /Solicitudes por confirmar/ });
  await expect(solicitudes).toContainText('Juan Cliente');
  await solicitudes.getByRole('button', { name: 'Confirmar' }).click();
  await expect(page.getByText('Cita de Juan Cliente confirmada')).toBeVisible();
  await expect(solicitudes).toHaveCount(0);

  // El cliente lo ve en su enlace
  await page.goto(enlace);
  await expect(page.getByRole('status')).toHaveText('Cita confirmada');
});
