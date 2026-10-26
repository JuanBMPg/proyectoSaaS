# Reservas para pequeños negocios

[![CI](https://github.com/JuanBMPg/reservas/actions/workflows/ci.yml/badge.svg)](https://github.com/JuanBMPg/reservas/actions/workflows/ci.yml)

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
| Tests | JUnit, Mockito y Testcontainers en el backend; Vitest en el frontend |
| Infraestructura | Docker Compose y GitHub Actions |

## Estructura

```
.
├── backend/              # API Spring Boot, organizada por funcionalidad
│   └── src/main/
│       ├── java/com/juan/reservas/
│       │   ├── config/   # Seguridad, CORS, OpenAPI, reloj (Clock)
│       │   └── comun/    # Errores y Problem Details
│       └── resources/db/migration/   # Migraciones Flyway
├── frontend/             # Aplicación Angular
├── docker-compose.yml    # PostgreSQL y Mailpit para desarrollo
└── .github/workflows/    # CI
```

## Arrancar en local

**Requisitos:** JDK 21, Node 24.15 o superior, y Docker con Docker Compose. Maven no hace falta,
porque el proyecto incluye el Maven Wrapper (`mvnw`).

```bash
# 1. Variables de entorno: copia el ejemplo y pon una contraseña para la base de datos
cp .env.example .env

# 2. Infraestructura: PostgreSQL y Mailpit
docker compose up -d

# 3. Backend en http://localhost:8080 (aplica las migraciones al arrancar)
cd backend
./mvnw spring-boot:run        # en Windows: mvnw.cmd spring-boot:run

# 4. Frontend en http://localhost:4200
cd frontend
npm install
npm start
```

| Servicio | URL |
| --- | --- |
| Frontend | http://localhost:4200 |
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Mailpit (emails de prueba) | http://localhost:8025 |

### Tests

```bash
cd backend && ./mvnw verify                 # unitarios + integración (necesita Docker)
cd frontend && npm run lint && npm test
```

Los tests de integración levantan un PostgreSQL real con Testcontainers. Si no hay Docker disponible
se marcan como omitidos; en el CI se exige que se ejecuten.

## Estado del proyecto

**Fase 0: cimientos** ✅
- [x] Monorepo con backend Spring Boot y frontend Angular
- [x] docker-compose con PostgreSQL y Mailpit
- [x] Migración V1 con Flyway, incluida la restricción `reserva_sin_solape`
- [x] CI en GitHub Actions: compilar, lint y tests de ambos proyectos
- [x] README inicial

**Fase 1: MVP** (en curso)
- [ ] Registro de negocio y login con JWT
- [ ] CRUD de servicios y recursos, y asignación servicio-recurso
- [ ] Horario semanal por recurso
- [ ] Calculadora de disponibilidad con tests unitarios
- [ ] Página pública `/:slug` para reservar
- [ ] Crear reserva con protección contra dobles reservas
- [ ] Email de confirmación con enlace de cancelación
- [ ] Agenda del panel con FullCalendar
- [ ] Empleados con acceso solo a su agenda
- [ ] Datos de demo y despliegue público

Después vienen la fase 2 (excepciones de horario, recordatorios, confirmación manual y e2e),
la fase 3 (pagos, estadísticas, plantillas de sector) y la fase 4 (widget, iCal, lista de espera).

## Decisiones técnicas

- **Sin dobles reservas en la base de datos.** Una restricción `EXCLUDE USING gist` sobre
  `(recurso_id, tstzrange(inicio, fin_bloqueo))` impide que dos reservas activas del mismo recurso se solapen,
  aunque lleguen dos peticiones en el mismo milisegundo. La violación (SQLState `23P01`) se traduce a `409 Conflict`.
- **Zonas horarias.** Los instantes se guardan en UTC (`timestamptz` / `Instant`) y los horarios semanales
  en hora local del negocio (`time` / `LocalTime`).
- **Tiempo testeable.** Se inyecta un `java.time.Clock` en lugar de llamar a `Instant.now()`.
- **Configuración sin secretos.** Todo llega por variables de entorno; `.env` está en `.gitignore`
  y `.env.example` documenta las variables.
