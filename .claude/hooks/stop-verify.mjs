// Stop: compile the side(s) Claude edited this turn. On failure, exit 2 so Claude sees the errors and fixes them.
//   frontend → ngc --noEmit (type-checks TS + templates)
//   backend  → gradlew compileJava --offline
import { spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { readInput, readState, writeState, FRONT, BACK } from './lib.mjs';

const input = readInput();
if (input.stop_hook_active) process.exit(0); // already retried once; don't loop

const state = readState(input.session_id);
if (!state.front && !state.back) process.exit(0);

const isWin = process.platform === 'win32';
const checks = {
  front: { cwd: FRONT, cmd: 'npx --no-install ngc -p tsconfig.app.json --noEmit', ready: existsSync(join(FRONT, 'node_modules')) },
  // absolute path: Claude Code sets NoDefaultCurrentDirectoryInExePath, so cmd.exe won't find gradlew.bat in cwd
  back: { cwd: BACK, cmd: `"${join(BACK, isWin ? 'gradlew.bat' : 'gradlew')}" compileJava -q --offline`, ready: existsSync(join(BACK, 'build.gradle.kts')) },
};

const failures = [];
for (const side of ['front', 'back']) {
  if (!state[side]) continue;
  const { cwd, cmd, ready } = checks[side];
  if (!ready) { state[side] = false; continue; }
  const r = spawnSync(cmd, { cwd, shell: true, encoding: 'utf8', timeout: 240_000, env: { ...process.env, FORCE_COLOR: '0', NO_COLOR: '1' } });
  if (r.status === 0) {
    state[side] = false;
    continue;
  }
  // strip ANSI codes and keep the output short
  const out = `${r.stdout ?? ''}\n${r.stderr ?? ''}`.replace(/\x1b\[[0-9;]*m/g, '').trim().split('\n').slice(0, 60).join('\n');
  failures.push(`[${side}] \`${cmd}\` falló (exit ${r.status ?? 'timeout'}):\n${out}`);
}

writeState(input.session_id, state);

if (failures.length) {
  process.stderr.write(
    `Verificación automática de compilación falló. Corrige estos errores (si son previos a tus cambios, dilo al usuario en vez de tocarlos):\n\n${failures.join('\n\n')}`
  );
  process.exit(2);
}
