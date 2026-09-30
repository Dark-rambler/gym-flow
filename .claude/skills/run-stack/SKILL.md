---
name: run-stack
description: Levanta y verifica el stack local de gym-Flow (Postgres con docker compose, API Spring Boot en :8080, front Angular en :4200). Úsala cuando haya que correr, probar a mano o depurar la app completa.
---

# Levantar el stack local

Trabaja desde la raíz `D:\gym-Flow`. El hook de inicio de sesión informa qué puertos (5433/8080/4200) están arriba; levanta solo lo que falte.

## 1. Postgres

```bash
docker compose up -d db
docker compose ps   # esperar a (healthy)
```

BD `gymflow` en el puerto **5433** (el 5432 lo usa otro contenedor), usuario/clave `gymflow`/`gymflow` (solo local). Resetear datos (`docker compose down -v`) es destructivo: solo con confirmación del usuario.

## 2. API (backend/)

En background (Bash con `run_in_background`). Requiere JDK 21: si `java -version` no es 21, usa `JAVA_HOME="$HOME/.jdks/ms-21.0.7"`.

```bash
cd backend && ./gradlew bootRun
```

Listo cuando `curl -s http://localhost:8080/actuator/health` → `{"status":"UP"}`. Flyway aplica `db/migration` al arrancar; si falla la validación (`ddl-auto: validate`), la entidad y la migración no coinciden. Config por variables de entorno (ver `backend/.env.example`); los defaults de `application.yml` sirven en local.

Swagger: http://localhost:8080/swagger-ui.html

## 3. Front (frontend/)

```bash
cd frontend && corepack pnpm start
```

Listo cuando `curl -sf http://localhost:4200` responde.

## 4. Inspeccionar la BD

MCP `postgres` (solo lectura): `SELECT * FROM flyway_schema_history`, tablas de negocio filtrando por `gym_id`.
