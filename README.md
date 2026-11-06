# Reservas para pequeños negocios

[![CI](https://github.com/JuanBMPg/proyectoSaaS/actions/workflows/ci.yml/badge.svg)](https://github.com/JuanBMPg/proyectoSaaS/actions/workflows/ci.yml)

Plataforma SaaS de reservas online para cualquier negocio pequeño: peluquerías, fisioterapeutas,
talleres, pistas deportivas o academias. Cada negocio configura sus servicios, sus recursos
(personas o cosas) y sus horarios, y sus clientes reservan desde una página pública sin necesidad de llamar.

Es un proyecto personal de portfolio y está en desarrollo.

## Objetivos técnicos

- **Modelo de dominio genérico**: nada específico de un sector en el código; todo se configura por negocio.
- **Multi-tenant** con aislamiento estricto de datos entre negocios.
- **Cálculo de disponibilidad** correcto y testeado, incluidos los cambios de hora.
- **Cero dobles reservas**, garantizado por la base de datos con una restricción de exclusión de PostgreSQL
  y no solo por el código.
- **Calidad profesional**: tests, CI, Docker, despliegue público y documentación.

## Stack

| Capa | Tecnología |
| --- | --- |
| Backend | Java 21, Spring Boot 4.1 (Web MVC, Security, Data JPA, Validation, Mail) |
| Base de datos | PostgreSQL 16 + Flyway |
| API | REST con errores en formato Problem Details (RFC 9457) y Swagger UI (springdoc-openapi) |
| Mapeo | MapStruct (las entidades JPA nunca salen del backend; la API usa DTOs `record`) |
| Frontend | Angular 22 (componentes standalone y signals) + Angular Material |
| Tests | JUnit, Mockito y Testcontainers en el backend; Vitest en el frontend; Playwright de extremo a extremo |
| Infraestructura | Docker (imágenes multi-stage, Nginx), Docker Compose y GitHub Actions |

## Estructura

```
.
├── backend/                 # API Spring Boot, organizada por funcionalidad
│   ├── Dockerfile           # Compila con el JDK y ejecuta con el JRE
│   └── src/main/java/com/juan/reservas/
│       ├── auth/            # Registro, login, JWT y refresh tokens
│       ├── negocio/ servicio/ recurso/ empleado/ cliente/
│       ├── reserva/         # Reservas: web pública, agenda del panel y estados
│       ├── disponibilidad/  # CalculadoraDisponibilidad y HorarioDelDia (lógica pura) y su carga de datos
│       ├── excepcion/       # Vacaciones, festivos y horarios especiales
│       ├── publico/         # Endpoints de la página pública
│       ├── notificacion/    # Emails: plantillas Thymeleaf, cola (outbox) y recordatorios
│       ├── demo/            # Negocio de demostración (opcional, DEMO=true)
│       ├── config/ comun/   # Seguridad, CORS, OpenAPI, Clock; errores y utilidades
│       └── ../resources/db/migration/   # Migraciones Flyway
├── frontend/                # Angular: panel privado y página pública
│   ├── Dockerfile           # Compila con Node y sirve con Nginx
│   └── nginx.conf.template  # Sirve la SPA y hace de proxy de /api
├── e2e/                     # Tests de extremo a extremo con Playwright
├── docker-compose.yml       # PostgreSQL, Mailpit, backend y frontend
└── .github/workflows/       # CI: tests, lint, cobertura, imágenes Docker y e2e
```

## Arrancar en local

Primero, las variables de entorno: copia el ejemplo y rellena `DB_PASSWORD` y `JWT_SECRET`
(genera uno con `openssl rand -base64 48`).

```bash
cp .env.example .env
```

### Opción 1: todo con Docker

```bash
docker compose up --build
```

Levanta PostgreSQL, Mailpit, el backend y el frontend, con el negocio de demostración ya cargado.
La aplicación queda en **http://localhost:8081**.

### Opción 2: para desarrollar

Requisitos: JDK 21, Node 24.15 o superior y Docker. Maven no hace falta, el proyecto incluye el Maven Wrapper.

```bash
docker compose up -d postgres mailpit     # solo la infraestructura

cd backend && ./mvnw spring-boot:run      # API en http://localhost:8080 (Windows: mvnw.cmd)
cd frontend && npm install && npm start   # web en http://localhost:4200
```

`npm start` redirige `/api` al backend con un proxy, así que el navegador ve frontend y API en el mismo
origen, igual que con Nginx. Con `DEMO=true` en el `.env`, el backend crea el negocio de demostración al arrancar.

| Servicio | Docker | Desarrollo |
| --- | --- | --- |
| Web | http://localhost:8081 | http://localhost:4200 |
| Swagger UI | http://localhost:8081/swagger-ui.html | http://localhost:8080/swagger-ui.html |
| Mailpit (emails de prueba) | http://localhost:8025 | http://localhost:8025 |

### Cuentas de demostración

| Quién | Dónde | Email | Contraseña |
| --- | --- | --- | --- |
| Cliente (sin cuenta) | `/peluqueria-lola` | — | — |
| Propietaria | `/login` | `demo@reservas.local` | `demo12345` |
| Empleada (solo su agenda) | `/login` | `marta@reservas.local` | `demo12345` |

### Tests

```bash
cd backend && ./mvnw verify                 # unitarios + integración (necesita Docker)
cd frontend && npm run lint && npm test
```

Los tests de extremo a extremo (Playwright) recorren la aplicación completa en un navegador: reservar,
recibir el email, cancelar, crear un festivo en el panel y confirmar una solicitud. Cada test crea su propio
negocio por la API, así que no dependen de la demo.

```bash
docker compose up -d --build                # la aplicación completa en http://localhost:8081
cd e2e && npm ci && npx playwright install chromium && npm test
```

Los tests de integración levantan PostgreSQL y Mailpit reales con Testcontainers. Si no hay Docker
disponible se marcan como omitidos; en el CI se exige que se ejecuten.

`mvnw verify` mide la cobertura con JaCoCo (informe en `backend/target/site/jacoco/index.html`) y falla si el
paquete `disponibilidad`, el núcleo del producto, baja del 80 % de líneas o de ramas.

## Despliegue

Las dos imágenes están listas para cualquier servicio que ejecute contenedores (Railway, Render, un VPS...):

1. Una base de datos PostgreSQL 16 gestionada.
2. El **backend** desde `backend/Dockerfile`, con `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`,
   `DB_PASSWORD`, `JWT_SECRET`, `MAIL_HOST`/`MAIL_PORT` (un SMTP real), `MAIL_FROM`,
   `FRONTEND_URL` (la dirección pública de la web) y `DEMO=true` para la demo.
3. El **frontend** desde `frontend/Dockerfile`, con `BACKEND_URL` apuntando a la dirección interna del backend.
   Es el único servicio público: Nginx sirve la web y reenvía `/api`, así que la cookie de sesión funciona
   en el mismo dominio y no hace falta CORS.

## Estado del proyecto

**Fase 0: cimientos** ✅
- [x] Monorepo con backend Spring Boot y frontend Angular
- [x] docker-compose con PostgreSQL y Mailpit
- [x] Migración V1 con Flyway, incluida la restricción `reserva_sin_solape`
- [x] CI en GitHub Actions: compilar, lint y tests de ambos proyectos
- [x] README inicial

**Fase 1: MVP** ✅
- [x] Registro de negocio y login con JWT
- [x] CRUD de servicios y recursos, y asignación servicio-recurso
- [x] Horario semanal por recurso
- [x] Calculadora de disponibilidad con tests unitarios
- [x] Página pública `/:slug`: elegir servicio, profesional, día y hora
- [x] Crear reserva con protección contra dobles reservas
- [x] Email de confirmación con el enlace a la reserva
- [x] Agenda del panel con FullCalendar
- [x] Empleados con acceso solo a su agenda
- [x] Datos de demo e imágenes Docker listas para desplegar (falta publicar la demo en un hosting)

**Fase 2: automatización** ✅
- [x] Excepciones: vacaciones, festivos y horarios especiales
- [x] Recordatorios automáticos 24 h antes con tarea programada (y emails con patrón outbox)
- [x] Cancelación por enlace respetando el plazo
- [x] Confirmación manual para negocios que la activen (y ajustes del negocio en el panel)
- [x] Tests e2e con Playwright del flujo de reserva

Lo siguiente es la fase 3 (pagos, estadísticas, plantillas de sector) y después la fase 4 (widget, iCal, lista de espera).

## Decisiones técnicas

- **Sin dobles reservas en la base de datos.** Una restricción `EXCLUDE USING gist` sobre
  `(recurso_id, tstzrange(inicio, fin_bloqueo))` impide que dos reservas activas del mismo recurso se solapen,
  aunque lleguen dos peticiones en el mismo milisegundo. La violación (SQLState `23P01`) se traduce a `409 Conflict`.
- **Zonas horarias.** Los instantes se guardan en UTC (`timestamptz` / `Instant`) y los horarios semanales
  en hora local del negocio (`time` / `LocalTime`).
- **Dos capas contra las dobles reservas.** Al crear una reserva se vuelve a comprobar la disponibilidad
  dentro de la transacción, pero la garantía real es la restricción de PostgreSQL: un test lanza 10 reservas
  simultáneas al mismo hueco y exige que solo se guarde una (las demás reciben 409).
- **Disponibilidad como lógica pura.** `CalculadoraDisponibilidad` recibe horarios, reservas y reglas ya cargados
  y devuelve los huecos en UTC, sin base de datos ni reloj. Tiene tests de los dos cambios de hora del año
  (días de 23 y 25 horas) y un test de propiedades con datos aleatorios.
- **Excepciones sin tocar la calculadora.** Una excepción cubre unos días (fechas locales del negocio) y es
  un cierre o un horario especial, de un recurso o de todo el negocio. `HorarioDelDia` resuelve las franjas
  de cada recurso antes de calcular (cerrar gana a abrir y lo del recurso gana a lo del negocio), así que
  la calculadora sigue recibiendo franjas, como antes.
- **Tiempo testeable.** Se inyecta un `java.time.Clock` en lugar de llamar a `Instant.now()`.
- **Sesión segura.** El access token (JWT de 15 min) vive solo en memoria en el frontend; el refresh token
  (7 días, rotado en cada uso y guardado como hash) viaja en una cookie `HttpOnly; SameSite=Strict`.
- **Emails con patrón outbox.** El email ya redactado se guarda en la tabla `email_pendiente` dentro de la
  misma transacción que la reserva: si la reserva no llega a guardarse, tampoco el email, y si la aplicación se
  cae antes de enviarlo, sigue en la cola. Una tarea `@Scheduled` lo envía con `FOR UPDATE SKIP LOCKED` (dos
  instancias no se pisan) y, si el servidor de correo falla, reintenta esperando cada vez el doble. Al enviarse
  se vacía el cuerpo, que lleva el enlace secreto. En local los emails se ven en Mailpit (http://localhost:8025).
- **Recordatorios 24 h antes.** Otra tarea programada encola el recordatorio de las citas confirmadas que
  empiezan en las próximas 24 horas (salvo las reservadas con menos antelación). Un índice único parcial
  garantiza un solo recordatorio por reserva.
- **Cancelar sin cuenta.** El enlace secreto del email permite cancelar hasta el plazo del negocio (24 h por
  defecto). El servidor decide si aún se puede, con su reloj, y bloquea la fila (`SELECT ... FOR UPDATE`) para
  que un doble clic no cancele dos veces ni envíe dos emails.
- **Configuración sin secretos.** Todo llega por variables de entorno; `.env` está en `.gitignore`
  y `.env.example` documenta las variables.
