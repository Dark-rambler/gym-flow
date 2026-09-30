---
name: new-migration
description: Crea una nueva migración Flyway en backend/src/main/resources/db/migration con el siguiente número de versión y las convenciones multi-tenant (gym_id, índices). Úsala para cualquier cambio de esquema de BD.
---

# Nueva migración Flyway

1. Lista `backend/src/main/resources/db/migration/` y toma el mayor `V{n}__`. La nueva es `V{n+1}__descripcion_en_snake_case.sql`.
2. **Nunca edites una migración existente** (el hook `protect-files` lo bloquea): ya puede estar aplicada en alguna BD. Para corregir, crea otra.
3. Convenciones:
   - PK `id BIGSERIAL`; timestamps `TIMESTAMPTZ NOT NULL DEFAULT now()`; dinero `NUMERIC(10,2)`; enums como `VARCHAR` + `CHECK`.
   - Toda tabla de negocio: `gym_id BIGINT NOT NULL REFERENCES gym(id)` + `CREATE INDEX idx_<tabla>_gym ON <tabla>(gym_id)`.
   - Unicidad por tenant: `UNIQUE (gym_id, dni)`, no `UNIQUE (dni)`.
   - FKs con índice en la columna referenciadora.
4. Refleja el cambio en la entidad JPA (`ddl-auto: validate` falla al arrancar si no coincide).
5. Verifica: arranca el back (skill `run-stack`) o corre los tests de integración; revisa `flyway_schema_history` con el MCP `postgres`.
