---
name: fullstack-feature
description: Orquesta una funcionalidad de gym-Flow que toca backend y frontend — contrato, back, sync del cliente, front y verificación end-to-end en el navegador.
---

# Feature full-stack

1. **Contrato primero**: muestra al usuario endpoints (método, ruta, roles), JSON de ejemplo, errores esperados y cambios de esquema. Pregunta solo si hay decisiones de producto ambiguas.
2. **Backend**: skill `new-endpoint` (+ `new-migration` si hay esquema). Termina con tests verdes.
3. **Contrato → front**: skill `sync-api`.
4. **Frontend**: skill `angular-feature`.
5. **Verificación**:
   - Agente `tenant-security-reviewer` sobre el diff del back.
   - E2E con MCP `playwright` contra el stack local: flujo feliz + un rol sin permiso. Si aplica, confirma en BD con MCP `postgres`.
6. **Cierre**: resumen de archivos cambiados. Commit (`feat: ...` en español, rama `dev`) solo si el usuario lo pide.
