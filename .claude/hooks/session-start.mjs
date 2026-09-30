// SessionStart: inject a short snapshot of the repo and which local services are up.
import { spawnSync } from 'node:child_process';
import net from 'node:net';
import { WORKSPACE } from './lib.mjs';

function git(args) {
  const r = spawnSync('git', args, { cwd: WORKSPACE, encoding: 'utf8' });
  return r.status === 0 ? r.stdout.trim() : '';
}

function portOpen(port) {
  return new Promise((res) => {
    const s = net.connect({ host: '127.0.0.1', port });
    const done = (ok) => { s.destroy(); res(ok); };
    s.setTimeout(400, () => done(false));
    s.once('connect', () => done(true));
    s.once('error', () => done(false));
  });
}

const branch = git(['branch', '--show-current']) || '?';
const dirty = git(['status', '--porcelain']).split('\n').filter(Boolean).length;
const [db, api, web] = await Promise.all([5433, 8080, 4200].map(portOpen));
const up = (b) => (b ? 'arriba' : 'abajo');

console.log([
  'Estado de gym-Flow:',
  `- Rama \`${branch}\`, ${dirty} archivo(s) con cambios sin commitear`,
  `- Servicios: Postgres :5433 ${up(db)} · API :8080 ${up(api)} · Front :4200 ${up(web)} (usa /run-stack para levantarlos)`,
].join('\n'));
