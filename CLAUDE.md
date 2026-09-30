# CLAUDE.md — gym-Flow

SaaS multi-gimnasio (cada gimnasio = tenant). MVP: socios y membresías, pagos manuales + caja, check-in (QR/DNI), dashboard. Fuera del MVP: pasarela de pago, app del socio, clases/rutinas, WhatsApp, facturación SUNAT.

Monorepo (git propio en esta carpeta, rama de trabajo `dev`, PR a `main`):

| Carpeta | Stack |
|---|---|
| `backend/` | Java 21 · Spring Boot 4.1 · Gradle Kotlin DSL · PostgreSQL 17 · Flyway · JPA · MapStruct · Spring Security + JWT · springdoc · Testcontainers |
| `frontend/` | Angular 22 standalone + signals · Tailwind v4 · Angular CDK · pnpm (vía `corepack`) · cliente API generado con ng-openapi-gen |

Deploy: back en Railway (Dockerfile + Railway Postgres), front en Vercel.

## Arrancar en local

Skill `/run-stack`. Resumen: `docker compose up -d db` (Postgres en **:5433**) → `cd backend && ./gradlew bootRun` (JDK 21; en PATH hay 17, usa `JAVA_HOME=~/.jdks/ms-21.0.7` si falla) → `cd frontend && corepack pnpm start`. Health: `http://localhost:8080/actuator/health`; Swagger: `/swagger-ui.html`.

## Reglas multi-tenant (críticas)

- Toda tabla de negocio tiene `gym_id NOT NULL` + índice; la entidad lo marca con Hibernate `@TenantId`, que filtra y rellena solo. El tenant sale del claim `gymId` del JWT.
- **Nunca** aceptar `gymId` en body/query/path. Queries nativas no pasan por `@TenantId`: filtrar `gym_id` a mano.
- Unicidades por tenant: `UNIQUE (gym_id, dni)`.
- Roles: `OWNER`, `ADMIN`, `RECEPTIONIST`.
- Antes de cerrar cambios de back: agente `tenant-security-reviewer`.

## Contrato front ↔ back

- Base `/api`. Errores `{ status, message, timestamp, errors? }`: 400 validación, 401, 403, 404, 409 regla de negocio/conflicto.
- Fechas ISO-8601 (`LocalDate` → `yyyy-MM-dd`, instantes con zona). Dinero `NUMERIC(10,2)` / `BigDecimal`, nunca `double`.
- `frontend/src/app/api/` es **generado** desde `/v3/api-docs`: tras cambiar un endpoint/DTO corre `/sync-api`. No escribir interfaces TS a mano para DTOs.

## Backend

Hexagonal por módulo: `com.gymflow.<modulo>.{domain,application,infrastructure,presentation}`; lo transversal en `com.gymflow.shared`. Detalle en la skill `/new-endpoint`. Migraciones Flyway en `backend/src/main/resources/db/migration`, inmutables una vez creadas (`/new-migration`). `ddl-auto: validate`. Config por variables de entorno (`backend/.env.example`).

## Frontend

`src/app/{core,shared/ui,api,features/<f>}`, páginas lazy, `OnPush`, signals/`resource`, Tailwind. Textos en español. Detalle en `/angular-feature`.

## Flujo de trabajo con Claude

- Feature completa: `/fullstack-feature`. Solo back: `/new-endpoint`. Solo front: `/angular-feature`. Esquema: `/new-migration`. Contrato: `/sync-api`.
- MCPs (`.mcp.json`): `postgres` (BD local, lectura), `angular-cli`, `playwright` (verificación en navegador), `context7` (docs actualizadas).
- Hooks (`.claude/settings.json`): bloquean editar `.env`, lockfiles, output generado, `src/app/api/` y migraciones existentes; al terminar un turno compilan el lado tocado (`compileJava` / `ngc --noEmit`) y devuelven errores.
- Revisión antes de PR: `tenant-security-reviewer` + `/code-review` (+ `/security-review` si toca auth).
- Commits en español `feat:`/`fix:`/`chore:`; solo cuando el usuario lo pida.

## Hoja de ruta

Plan completo: semana 0 setup · 1 auth + tenant · 2 socios/planes/membresías · 3 pagos y caja · 4 check-in + job de vencimientos + dashboard · 5 E2E, hardening y deploy.
