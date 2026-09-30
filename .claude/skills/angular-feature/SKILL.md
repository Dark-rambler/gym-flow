---
name: angular-feature
description: Checklist para crear o ampliar una pantalla/feature en frontend/ (Angular 22 standalone, signals, Tailwind v4, cliente API generado). Úsala para cualquier trabajo de UI o de consumo de API en el front.
---

# Feature en frontend/

Estructura: `src/app/core` (auth, interceptors, guards, layout), `shared/ui` (componentes presentacionales), `api/` (generado, no tocar), `features/<f>/` (páginas lazy).

1. **Contrato**: si el endpoint es nuevo o cambió, corre `/sync-api` primero. Usa los servicios y modelos de `src/app/api/` — no escribas interfaces a mano para DTOs del back.
2. **Estado**: en la página, `inject()` del servicio generado; lecturas con `rxResource`/`resource` + `computed`; mutaciones que al terminar hacen `reload()`. Nada de `subscribe` manual sin `takeUntilDestroyed`.
3. **Página** `features/<f>/<f>.page.ts` standalone, `ChangeDetectionStrategy.OnPush`, control flow `@if`/`@for (…; track id)`, formularios con Reactive Forms tipados.
4. **UI**: Tailwind en el template; componentes reutilizables en `shared/ui/<nombre>/` solo con `input()`/`output()`. Diálogos con `@angular/cdk/dialog`. Textos en español. Pensar en tablet (recepción del gym) y móvil.
5. **Ruta** lazy en `app.routes.ts` (`loadComponent`) bajo el layout autenticado + guard por rol si aplica; entrada en el menú lateral.
6. Consulta el MCP `angular-cli` (best practices / ejemplos) ante dudas de APIs nuevas de Angular.
7. **Verifica**: `npx ngc -p tsconfig.app.json --noEmit` (el hook Stop también lo corre); con el stack arriba, MCP `playwright`: login, navegar, revisar consola/red y screenshot.
