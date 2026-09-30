---
name: sync-api
description: Regenera el cliente TypeScript del front (frontend/src/app/api) desde el OpenAPI del backend y verifica que el front compile. Úsala siempre que cambie un endpoint o DTO del backend.
---

# Sincronizar el contrato back → front

1. El back debe estar arriba: `curl -s http://localhost:8080/v3/api-docs -o /dev/null -w "%{http_code}"` → `200` (si no, skill `run-stack`).
2. `cd frontend && rm -rf src/app/api && node node_modules/ng-openapi-gen/lib/index.js --config ng-openapi-gen.json` (salida `src/app/api/`).
   - Si el back corre en otro puerto: añade `--input http://localhost:<puerto>/v3/api-docs`.
   - No uses `corepack pnpm run api:gen` si hay un `ng serve` abierto: pnpm 12 intenta reinstalar antes de `run` y falla con `ERR_PNPM_PACKAGE_MANAGER_SYMLINK_FAILED` (archivos bloqueados en Windows).
   - El cliente usa el estilo funcional de ng-openapi-gen 1.x: `inject(Api).invoke(listStaff)` / `invoke(updateStaff, { id, body })` → `Promise<T>`.
   - Si los tipos salen con todo opcional o el cliente lee `Blob`: revisa `OpenApiConfig` (DTOs `*Response` requeridos) y `springdoc.default-produces-media-type`.
3. `src/app/api/` es generado: no se edita a mano (el hook lo bloquea). Sí se commitea, para que CI y Vercel no necesiten el back.
4. Compila: `npx ngc -p tsconfig.app.json --noEmit`. Los errores indican usos del contrato viejo en `features/` → corrígelos.
5. Resume al usuario qué endpoints/modelos cambiaron (`git diff --stat frontend/src/app/api`).
