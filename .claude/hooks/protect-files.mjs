// PreToolUse (Edit|Write|MultiEdit|NotebookEdit): block edits to secrets, generated output, lockfiles
// and Flyway migrations that already exist (applied migrations are immutable — create a new V{n}__ instead).
import { existsSync } from 'node:fs';
import { readInput } from './lib.mjs';

const input = readInput();
const filePath = String(input.tool_input?.file_path ?? input.tool_input?.notebook_path ?? '');
const p = filePath.replace(/\\/g, '/').toLowerCase();

const rules = [
  [/(^|\/)\.env(\.[^/]*)?$/, 'contiene secretos; edita .env.example y pide al usuario que actualice su .env', (x) => !x.endsWith('.env.example')],
  [/\/(node_modules|dist|build|\.angular|\.gradle|out-tsc)\//, 'es output generado o dependencias; cambia el código fuente'],
  [/(^|\/)(pnpm-lock\.yaml|package-lock\.json)$/, 'es un lockfile; usa `corepack pnpm add/remove` en su lugar'],
  [/gradle\/wrapper\//, 'es el wrapper de Gradle; usa `./gradlew wrapper --gradle-version X`'],
  [/\/db\/migration\/v\d+__[^/]+\.sql$/, 'es una migración Flyway existente (inmutable); crea una nueva con la skill /new-migration', () => existsSync(filePath)],
  [/\/frontend\/src\/app\/api\//, 'es el cliente generado desde OpenAPI; cambia el back y corre /sync-api'],
];

for (const [re, why, extra] of rules) {
  if (re.test(p) && (!extra || extra(p))) {
    process.stderr.write(`Bloqueado por hook protect-files: ${filePath} ${why}.`);
    process.exit(2);
  }
}
