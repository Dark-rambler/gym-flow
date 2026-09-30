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
- Roles: `OWNER`, `ADMIN`, `RECEPTIONIST`. Un ADMIN solo crea/gestiona RECEPTIONIST; roles ADMIN/OWNER los gestiona el OWNER. Siempre debe quedar ≥1 OWNER activo.
- Antes de cerrar cambios de back: agente `tenant-security-reviewer`.

### Cómo está implementado (semana 1)

- `GymTenantResolver` (`shared/infrastructure/tenant`): override de `TenantContext` → claim `gymId` del JWT (solo si > 0) → `NONE` (0, no ve nada). `TenantContext.SYSTEM` (-1) es el tenant root de Hibernate y **desactiva** el filtro: solo vía `TenantContext.callAsSystem(...)` en flujos sin JWT (registro, login, refresh, logout, jobs).
- Hibernate fija el tenant al abrir la sesión: `callAs`/`callAsSystem` deben envolver la transacción (`callAsSystem(() -> tx.execute(...))`), nunca llamarse dentro de un `@Transactional` (lanza `IllegalStateException`).
- Búsqueda por id en entidades con tenant: usa JPQL (`findScopedById`), no `findById`/`em.find`.
- Casos de uso reciben un `Actor(userId, gymId, role)` construido en el controller con `CurrentActor.of(jwt)`.
- Auth: JWT HS256 (`spring-boot-starter-oauth2-resource-server`), access 15 min con claims `sub, gymId, role, name, iss=gymflow`. Refresh token opaco (SHA-256 en `refresh_token`), 7 días, rotación con consumo atómico; reutilizar uno ya usado revoca todas las sesiones del usuario → el front serializa los refresh (Web Locks).
- `JWT_SECRET` es obligatorio y sin default en `application.yml` (bootRun/tests inyectan uno de desarrollo). En producción también `API_DOCS_ENABLED=false`.
- Defensa en BD (V4): FKs compuestas `(gym_id, x_id) → tabla(gym_id, id)`. Toda tabla nueva que referencie a otra con tenant debe seguir ese patrón (`UNIQUE (gym_id, id)` en la referenciada).

## Socios, planes y membresías (semana 2)

- Estado efectivo **derivado de fechas, no persistido**: en BD `membership.status` ∈ `ACTIVE|FROZEN|CANCELLED`; `SCHEDULED`/`EXPIRED` salen de `Membership.statusOn(hoy)`. No hay job de vencimientos.
- "Hoy" = `GymCalendar.today(gymId)` (zona horaria del gym, `Clock` inyectable). Nunca `LocalDate.now()`. En tests: `MutableClock` (`clock.advanceDays(n)`), "hoy" = 2026-03-10.
- Fechas inclusivas: `endDate = start + durationDays - 1`. Renovación encadenada (`Memberships.nextStartDate`), máximo **una** renovación programada.
- Congelar: solo la vigente, solo si no hay renovación programada; no se puede renovar con una congelada. Descongelar suma los días al `endDate`. Cancelar no adelanta renovaciones programadas (limitación conocida).
- La membresía guarda foto de `plan_name` y `price`. Precio distinto al del plan: solo OWNER/ADMIN.
- Operaciones sobre membresías/socio bloquean la fila del socio (`lockById`) **antes** de leer (si se lee antes, la sesión de Hibernate queda con datos viejos).
- `qrToken` del socio no se expone en ninguna respuesta todavía: en la semana 4 tendrá endpoint propio + rotación (OWNER/ADMIN).
- RECEPTIONIST: socios (crear/editar), vender/renovar al precio del plan, ver planes. OWNER/ADMIN: además planes, precio, congelar/cancelar, desactivar socios.
- Front: estados/labels en `features/members/membership-status.ts`; `ConfirmService` para acciones destructivas; locale `es-PE` y moneda `PEN` globales (`{{ x | currency }}` → S/).

## Caja y pagos (semana 3)

- **Vender = membresía + pago en la caja abierta, en una transacción** (`MembershipUseCases.assign`). Sin caja abierta → 409. Cobro siempre completo. Métodos `CASH|YAPE|PLIN|CARD`.
- Una caja abierta por gym (índice único parcial `uk_cash_session_open`). **Orden de bloqueo: socio → caja** (venta, anulación); el cierre solo bloquea la caja. No invertirlo (deadlock).
- **Idempotencia**: `idempotencyKey` (UUID del cliente, uno por intento de venta; el front lo genera al abrir el diálogo). Repetirla con la misma venta devuelve la original; con otro socio/plan/método/precio → 409.
- Arqueo: `esperado = inicial + efectivo no anulado`; `diferencia = contado − esperado`. **Arqueo a ciegas**: RECEPTIONIST no recibe `expectedCash`/`totals`/`difference` (`CashSessionDetailResponse.blind()`); OWNER/ADMIN sí (historial).
- Anular pago (OWNER/ADMIN, motivo): solo si su caja sigue abierta; cancela la membresía; bloqueado si hay una renovación posterior no cancelada. Cancelar una membresía desde la ficha NO toca el pago (no hay devoluciones).
- Montos con `Money.of` (2 decimales) y tope S/ 99,999.99 en precios/monto inicial.
- Reporte `GET /api/reports/income?from&to` (inclusive, días en zona del gym, máx. 1 año). Tests: helpers `openCash(session)` y `sell(...)` en `ApiTestSupport`.

## Check-in, QR y dashboard (semana 4)

- `POST /api/check-ins {code}`: `code` es el QR (`GF1:<uuid>` o el uuid solo) o un DNI (`CheckInCode.parse`). Responde 200 también si se deniega (`result`, `reason`, `message` listo para mostrar).
- Veredicto (`CheckInVerdict`): UNKNOWN → MEMBER_INACTIVE → NO_MEMBERSHIP → según `Memberships.current(hoy)` (solo ACTIVE pasa). Se registran los denegados (INVALID_CODE no). Otra lectura del mismo socio en < 2 h devuelve `duplicate: true` y **no inserta**.
- Buscar el socio por **id escalar** (`findIdByQrToken`/`findIdByDni`) → `lockById` → leer. Nunca cargar la entidad antes del bloqueo.
- El `qrToken` es la credencial de acceso. `GET /api/members/{id}/qr` (todos los roles, para imprimir carnet) y `POST .../qr/rotate` (OWNER/ADMIN) que invalida carnet y enlace.
- **Ruta pública** `GET /api/public/member-card/{token}` (sin login, `callAsSystem`): solo nombre, gym, payload y estado de membresía; socio inactivo o token viejo → 404. El enlace del celular contiene la credencial: si se filtra, se **rota**. `index.html` usa `referrer: no-referrer`; `BearerTokenResolver` e interceptor ignoran `/api/public/`.
- Dashboard `GET /api/dashboard/summary`: activos hoy, congelados, entradas de hoy, por vencer en 7 días (sin renovación posterior y socio activo).
- Front: `/check-in` (input siempre enfocado para lector USB, cámara con `@zxing/browser` cargado por import dinámico), `gf-qr-code` (lib `qrcode`), `/imprimir/carnet/:id` (tarjeta CR80) y `/carnet/:token` (pública, fuera del shell).

## Contrato front ↔ back

- Base `/api`. Errores `{ status, message, timestamp, errors? }`: 400 validación, 401, 403, 404, 409 regla de negocio/conflicto.
- Fechas ISO-8601 (`LocalDate` → `yyyy-MM-dd`, instantes con zona). Dinero `NUMERIC(10,2)` / `BigDecimal`, nunca `double`.
- `frontend/src/app/api/` es **generado** desde `/v3/api-docs`: tras cambiar un endpoint/DTO corre `/sync-api`. No escribir interfaces TS a mano para DTOs.

## Backend

Hexagonal por módulo: `com.gymflow.<modulo>.{domain,application,infrastructure,presentation}`; lo transversal en `com.gymflow.shared`. Detalle en la skill `/new-endpoint`. Migraciones Flyway en `backend/src/main/resources/db/migration`, inmutables una vez creadas (`/new-migration`). `ddl-auto: validate`. Config por variables de entorno (`backend/.env.example`).

## Frontend

`src/app/{core,shared/ui,api,features/<f>}`, páginas lazy, `OnPush`, signals/`resource`, Tailwind. Textos en español. Detalle en `/angular-feature`.

- Sesión: `core/auth/auth.store.ts` (signals + localStorage, sincronizado entre pestañas), `auth.service.ts` (login/registro/logout/refresh), `auth.interceptor.ts` (Bearer + un refresh y reintento ante 401; no toca `/api/auth/*` ni `/api/public/*`), `auth.guards.ts` (`authGuard`, `guestGuard`, `roleGuard(...)`).
- API: `inject(Api).invoke(fn, params)` con las funciones de `api/functions`. Errores: `apiErrorMessage` / `apiFieldErrors` de `core/http/api-error.ts`; notificaciones con `ToastService`.
- UI base en `shared/ui`: `button[gfButton]`, `gf-form-field` (+ `fieldError()` de `shared/forms`), `gf-icon`, toasts. Inputs con la clase `gf-input`; colores de marca `brand-*` en `styles.css`.
- `environment.development.ts` (apiUrl `http://localhost:8080`) reemplaza a `environment.ts` en `ng serve`.

## Flujo de trabajo con Claude

- Feature completa: `/fullstack-feature`. Solo back: `/new-endpoint`. Solo front: `/angular-feature`. Esquema: `/new-migration`. Contrato: `/sync-api`.
- MCPs (`.mcp.json`): `postgres` (BD local, lectura), `angular-cli`, `playwright` (verificación en navegador), `context7` (docs actualizadas).
- Hooks (`.claude/settings.json`): bloquean editar `.env`, lockfiles, output generado, `src/app/api/` y migraciones existentes; al terminar un turno compilan el lado tocado (`compileJava` / `ngc --noEmit`) y devuelven errores.
- Revisión antes de PR: `tenant-security-reviewer` + `/code-review` (+ `/security-review` si toca auth).
- Commits en español `feat:`/`fix:`/`chore:`; solo cuando el usuario lo pida.

## Hoja de ruta

Plan completo: semana 0 setup ✅ · 1 auth + tenant ✅ (pendiente: rate limiting de login/registro → semana 5) · 2 socios/planes/membresías ✅ · 3 pagos y caja ✅ · 4 check-in (QR con rotación / DNI) + dashboard ✅ · 5 E2E, hardening, rate limiting y deploy.
