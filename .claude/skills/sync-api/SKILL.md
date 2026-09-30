---
name: sync-api
description: Regenera el cliente TypeScript del front (frontend/src/app/api) desde el OpenAPI del backend y verifica que el front compile. Úsala siempre que cambie un endpoint o DTO del backend.
---

# Sincronizar el contrato back → front

1. El back debe estar arriba: `curl -s http://localhost:8080/v3/api-docs -o /dev/null -w "%{http_code}"` → `200` (si no, skill `run-stack`).
2. `cd frontend && corepack pnpm run api:gen` (config en `frontend/ng-openapi-gen.json`, salida `src/app/api/`).
3. `src/app/api/` es generado: no se edita a mano (el hook lo bloquea). Sí se commitea, para que CI y Vercel no necesiten el back.
4. Compila: `npx ngc -p tsconfig.app.json --noEmit`. Los errores indican usos del contrato viejo en `features/` → corrígelos.
5. Resume al usuario qué endpoints/modelos cambiaron (`git diff --stat frontend/src/app/api`).
