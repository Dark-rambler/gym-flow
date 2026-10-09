# gymFlow

SaaS para gestionar gimnasios: cada gimnasio se registra y trabaja con sus propios datos, aislados de los demás.

**Qué hace hoy (MVP):**

- **Staff y roles**: dueño (OWNER), administrador (ADMIN) y recepción (RECEPTIONIST).
- **Socios**: alta con DNI, búsqueda por nombre o DNI, ficha con historial.
- **Planes y membresías**: venta, renovación encadenada al vencimiento, congelar/descongelar y cancelar.
- **Caja**: apertura, cobros en efectivo, Yape, Plin o tarjeta, anulación de pagos y cierre con arqueo (a ciegas para recepción).
- **Check-in**: control de acceso por QR (lector USB o cámara) o DNI, carnet imprimible y carnet digital para el celular del socio.
- **Dashboard**: estado de la caja, ingresos, asistencias del día y socios por vencer.

## Stack

| | |
|---|---|
| Backend | Java 21 · Spring Boot 4 · Spring Security (JWT) · JPA/Hibernate (multi-tenant con `@TenantId`) · Flyway · PostgreSQL 17 |
| Frontend | Angular 22 (standalone + signals) · Tailwind CSS v4 · cliente de API generado desde OpenAPI |
| Tests | JUnit 5 + Testcontainers + ArchUnit · Vitest |
| Infra local | Docker Compose (Postgres) |

## Requisitos

- **Docker**: para la base de datos y para los tests del backend (Testcontainers).
- **JDK 21**.
- **Node.js 22** con **corepack** (trae pnpm). Usa siempre `corepack pnpm`, **nunca `npm install`**: mezclar gestores rompe `node_modules`.

## Levantar en local

```bash
# 1. Base de datos: Postgres en el puerto 5433 (usuario, clave y BD: gymflow)
docker compose up -d db

# 2. Backend → http://localhost:8080
cd backend
./gradlew bootRun              # PowerShell: .\gradlew.bat bootRun

# 3. Frontend → http://localhost:4200 (en otra terminal)
cd frontend
corepack pnpm install          # solo la primera vez o si cambian las dependencias
corepack pnpm start
```

Al arrancar, el backend aplica solo las migraciones de base de datos (Flyway).

**Primeros pasos en la app:**

1. Abre http://localhost:4200/registro y registra un gimnasio; quedas como dueño.
2. En **Planes**, crea uno (por ejemplo "Mensual", 30 días).
3. En **Caja**, ábrela.
4. En **Socios**, registra un socio y véndele una membresía.
5. En **Check-in**, escribe su DNI y pulsa Enter.

**Documentación de la API:** http://localhost:8080/swagger-ui.html · **Health:** http://localhost:8080/actuator/health

## Configuración

El backend se configura con variables de entorno. Los valores por defecto de `application.yml` sirven para desarrollo local; `backend/.env.example` las documenta todas.

| Variable | Por defecto (local) | Notas |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/gymflow` | |
| `DB_USERNAME` / `DB_PASSWORD` | `gymflow` / `gymflow` | |
| `PORT` | `8080` | |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | En producción, la URL del front |
| `JWT_SECRET` | — | **Obligatoria**, mínimo 32 bytes (`openssl rand -base64 48`). `bootRun` y los tests inyectan uno de desarrollo; el jar de producción no arranca sin ella. |
| `JWT_ACCESS_TTL` / `JWT_REFRESH_TTL` | `15m` / `7d` | |
| `API_DOCS_ENABLED` | `true` | Poner `false` en producción |

La URL del backend que usa el frontend está en `frontend/src/environments/`: `environment.development.ts` para `ng serve` y `environment.ts` para producción.

## Tests

```bash
# Backend: unitarios + integración con Postgres real (necesita Docker) + reglas de arquitectura
cd backend && ./gradlew test

# Frontend
cd frontend
corepack pnpm test --watch=false
corepack pnpm build
```

El CI de GitHub Actions (`.github/workflows/ci.yml`) ejecuta ambos en cada push y PR a `main` y `dev`.

## Estructura

```
gym-Flow/
├── backend/                     Spring Boot
│   └── src/main/java/com/gymflow/
│       ├── shared/              seguridad, multi-tenant, errores, config
│       ├── auth/  staff/  gym/
│       ├── member/  plan/  membership/
│       ├── cash/                caja, pagos y reportes
│       ├── checkin/  dashboard/
│       └── (cada módulo: domain / application / infrastructure / presentation)
│   └── src/main/resources/db/migration/   migraciones Flyway (V1…)
├── frontend/                    Angular
│   └── src/app/
│       ├── api/                 cliente generado (no editar a mano)
│       ├── core/                auth, layout, http
│       ├── shared/ui/           componentes reutilizables
│       └── features/            una carpeta por pantalla
├── docker-compose.yml
└── CLAUDE.md                    guía técnica detallada (reglas de negocio y convenciones)
```

## Desarrollo

- **Contrato front ↔ back**: si cambias un endpoint o DTO, regenera el cliente del front con el backend levantado:
  ```bash
  cd frontend && corepack pnpm run api:gen
  ```
- **Base de datos**: los cambios de esquema van en una migración nueva `V{n}__descripcion.sql`. Nunca edites una migración que ya existe.
- **Multi-tenant**: todo dato de negocio lleva `gym_id`, y el backend lo toma siempre del token, nunca del request.
- **Ramas**: se trabaja en `dev` (o en ramas `feat/...` que salen de `dev`) y se pasa a `main` con un PR.
- **Commits**: en español, con prefijos `feat:`, `fix:` o `chore:`.
- Las reglas de negocio detalladas (membresías, caja, check-in) están en [`CLAUDE.md`](CLAUDE.md).

## Estado

MVP funcional completo. Pendiente para producción: límite de intentos en login y check-in, cambio y recuperación de contraseña, página de reportes y despliegue (backend en Railway, frontend en Vercel).
