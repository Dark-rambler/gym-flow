# gymFlow · frontend

Angular 22 (standalone + signals) con Tailwind CSS v4. Instrucciones completas de instalación y ejecución en el [README principal](../README.md).

```bash
corepack pnpm install          # nunca npm install
corepack pnpm start            # http://localhost:4200 (espera el backend en :8080)
corepack pnpm test --watch=false
corepack pnpm build
corepack pnpm run api:gen      # regenera src/app/api desde el OpenAPI del backend levantado
```
