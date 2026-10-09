# Errores de la API y cómo los maneja el frontend

Verificado el 2026-10-09 contra el backend local (`http://localhost:8080`) con 26 peticiones mal formadas o inválidas. **Ningún endpoint respondió 500.**

## Formato de error del backend

Todas las respuestas de error usan el mismo cuerpo (`exception/ErrorResponse.java`):

```json
{ "status": 400, "message": "Datos inválidos", "timestamp": "2026-10-09T15:02:54Z", "errors": { "active": "must not be null" } }
```

`errors` (campo → mensaje) solo aparece en errores de validación.

## Qué produce cada código (`exception/GlobalExceptionHandler.java`)

| Código | Causa | Mensaje |
|---|---|---|
| 400 | Validación de `@Valid` (body, parámetros) | `Datos inválidos` + `errors` |
| 400 | JSON mal formado, fecha inválida o enum desconocido en el body | `Cuerpo de la solicitud mal formado` |
| 400 | Parámetro con tipo incorrecto (`/cajas/abc`, `?date=2026-13-40`) | `Valor inválido para el parámetro '<nombre>'` |
| 400 | `?sort=` con un campo inexistente | `Campo de ordenamiento inválido: <campo>` o `Parámetros de consulta inválidos` |
| 400 | Falta un parámetro obligatorio (`from`/`to` del reporte) | `Falta el parámetro obligatorio '<nombre>'` |
| 400 | Reglas de negocio (rango de reporte, rol OWNER, etc.) | Mensaje específico en español |
| 401 | Token ausente, inválido o vencido | `No autenticado` |
| 403 | Rol sin permiso (`@PreAuthorize`) | `Acceso denegado` |
| 404 | Recurso inexistente | `<Recurso> no encontrado con id: <id>` |
| 405 / 415 | Método o Content-Type no soportado | `Método X no soportado` / `Tipo de contenido no soportado` |
| 409 | Conflicto de negocio (caja ya abierta, DNI/email duplicado), restricción de BD o bloqueo optimista | Mensaje específico o `La operación entra en conflicto con datos existentes` |
| **500** | **Solo una excepción no prevista** (`@ExceptionHandler(Exception.class)`): BD caída, bug, etc. | `Error interno del servidor` |

## Casos probados

| Petición | Resultado |
|---|---|
| `GET /api/members?sort=nope,asc` | 400 `Parámetros de consulta inválidos` |
| `GET /api/members?size=100000` | 200 (Spring limita a `max-page-size: 100`) |
| `GET /api/members?page=-1` | 200 (Spring lo trata como página 0) |
| `GET /api/check-ins?sort=nope` | 400 |
| `GET /api/check-ins?date=2026-13-40` | 400 `Valor inválido para el parámetro 'date'` |
| `GET /api/cash/sessions?sort=nope` | 400 `Campo de ordenamiento inválido: nope` |
| `GET /api/reports/income` con `to < from` | 400 `La fecha final no puede ser anterior a la inicial` |
| `GET /api/reports/income` con más de 1 año | 400 `El rango máximo es de 1 año` |
| `GET /api/public/member-card/not-a-uuid` | 404 `Carnet no encontrado` |
| `GET /api/public/member-card/<uuid inexistente>` | 404 `Carnet no encontrado` |
| `GET /api/members/999999` y `/999999/qr` | 404 `Socio no encontrado con id: 999999` |
| `POST /api/members` con `birthDate` inválida o JSON roto | 400 `Cuerpo de la solicitud mal formado` |
| `POST /api/members/1/memberships` con plan inexistente | 404 `Plan no encontrado con id: 999999` |
| `POST /api/members/1/memberships` con `paymentMethod: "BITCOIN"` | 400 `Cuerpo de la solicitud mal formado` |
| `POST /api/memberships/999999/freeze` | 404 |
| `POST /api/payments/999999/void` | 404 |
| `POST /api/check-ins` con `GF1:not-a-uuid` o `GF1:` | 200 `DENIED` / `INVALID_CODE` (por diseño) |
| `POST /api/cash/open` con caja ya abierta | 409 `Ya hay una caja abierta` |
| `PATCH /api/staff/999999` | 404 |
| `POST /api/staff` con `role: "SUPERUSER"` | 400 `Cuerpo de la solicitud mal formado` |
| `PUT /api/plans/999999` | 404 |
| `PATCH /api/members/1/active` con `{}` | 400 `Datos inválidos`, `errors.active = "must not be null"` |
| `GET /api/me` con token basura | 401 `No autenticado` |

## Cómo lo muestra el frontend (`src/app/core/http/`)

- **Sin conexión (status 0):** `No se pudo conectar con el servidor. Intenta de nuevo.`
- **401:** `interceptors.ts` cierra la sesión y redirige a `/login?redirect=<página actual>` (excepto en `/api/auth/*` y `/api/public/*`).
- **403:** `No tienes permiso para esta acción.`
- **404 en páginas de detalle:** estado "no encontrado" de la página.
- **409 de DNI o email duplicado:** el mensaje se muestra debajo del campo correspondiente.
- **400 con `errors`:** cada mensaje se muestra debajo de su campo; los mensajes por defecto en inglés de Bean Validation se traducen en `translateMessage()`.
- **500 y el resto:** se muestra el `message` del backend (`Error interno del servidor`) en un toast o en el estado de error de la página, con "Reintentar" donde aplica.

## Observaciones del backend (no son 500)

- Los mensajes de validación de campo salen en inglés (`must not be null`). El frontend los traduce, pero sería mejor configurarlos en español en el backend.
- `?sort=` inválido devuelve dos mensajes distintos según el endpoint (`Parámetros de consulta inválidos` vs `Campo de ordenamiento inválido`).
- Una fecha mal formada en el body da el genérico `Cuerpo de la solicitud mal formado`, sin indicar el campo.

## Sin probar

El manejo real de un 500 en la UI (por ejemplo, con la base de datos caída). Para probarlo: detener PostgreSQL con el backend arriba y recorrer las pantallas.
