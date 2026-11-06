import { defineConfig, devices } from '@playwright/test';

/**
 * Tests de extremo a extremo contra la aplicación completa (docker compose up --build):
 * Nginx sirve la web y reenvía /api al backend, como en producción.
 *
 *   E2E_URL      dirección de la web (por defecto http://localhost:8081)
 *   E2E_MAILPIT  API de Mailpit para comprobar los emails (por defecto http://localhost:8025)
 */
export default defineConfig({
  testDir: './tests',
  // Cada test crea su propio negocio, así que no se pisan; aun así, en serie es más fácil de seguir
  workers: 1,
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env.E2E_URL ?? 'http://localhost:8081',
    locale: 'es-ES',
    timezoneId: 'Europe/Madrid',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
