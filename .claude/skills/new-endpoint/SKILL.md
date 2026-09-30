---
name: new-endpoint
description: Checklist para añadir o modificar un caso de uso / endpoint REST en backend/ siguiendo la arquitectura hexagonal por módulo (domain, application, infrastructure, presentation) con multi-tenant por gym_id. Úsala para cualquier cambio de API en el backend.
---

# Nuevo endpoint en backend/

Módulos en `com.gymflow.<modulo>` (`auth`, `gym`, `member`, `membership`, `payment`, `checkin`); lo transversal en `com.gymflow.shared`. Cada módulo:

```
<modulo>/
  domain/          model/ (POJOs/records sin anotaciones de Spring ni JPA), port/ (interfaces de repositorio), exception/
  application/     usecase/ (un caso de uso por clase), dto/ (records request/response), mapper/ (MapStruct)
  infrastructure/  persistence/ (entidades JPA, Spring Data repos, adapters que implementan los ports)
  presentation/    controller REST
```

Regla: `domain` no depende de nada; `application` solo de `domain`; `infrastructure`/`presentation` dependen hacia dentro.

## Pasos

1. **Esquema**: si cambia, crea migración con la skill `new-migration` (nunca edites una existente).
2. **Dominio**: modelo + port (`MemberRepository` interfaz) + excepciones de negocio.
3. **Entidad JPA** en `infrastructure/persistence`: campo `gymId` anotado con `@TenantId` (Hibernate lo rellena y filtra solo). Relaciones `LAZY`. Adapter que implementa el port con Spring Data + MapStruct.
4. **Caso de uso** en `application/usecase`: `@Service`, `@Transactional` en escrituras, recibe/devuelve DTOs `record` con `jakarta.validation`.
5. **Controller**: `@RequestMapping("/api/<recurso>")`, `@Tag`, `@Operation`, `@Valid`, 201 al crear, `@PreAuthorize("hasAnyRole('OWNER','ADMIN',...)")`.
6. **Tenant**: jamás aceptes `gymId` en body/query/path; sale del JWT. Queries nativas (`nativeQuery = true`) NO pasan por `@TenantId` → añade `WHERE gym_id = :gymId` explícito.
7. **Errores**: excepciones de negocio → `GlobalExceptionHandler` con cuerpo `{status, message, timestamp, errors?}` (400 validación, 404, 409 conflicto/regla de negocio).
8. **Test**: integración con Testcontainers (`@Import(TestcontainersConfiguration.class)`), incluyendo un caso con un usuario de **otro gym** que no debe ver el recurso.
9. **Verifica**: `./gradlew compileJava test`; con el stack arriba, prueba con curl.
10. **Contrato**: si el front lo consume, corre `/sync-api`. Pasa el agente `tenant-security-reviewer` sobre el diff.
