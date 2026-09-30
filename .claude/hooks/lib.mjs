// Shared helpers for the gym-Flow hooks.
import { readFileSync, mkdirSync, writeFileSync, existsSync, rmSync } from 'node:fs';
import { dirname, join, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

export const WORKSPACE = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
export const FRONT = join(WORKSPACE, 'frontend');
export const BACK = join(WORKSPACE, 'backend');
const STATE_DIR = join(WORKSPACE, '.claude', 'hooks', '.state');

export function readInput() {
  try {
    return JSON.parse(readFileSync(0, 'utf8') || '{}');
  } catch {
    return {};
  }
}

export function norm(p) {
  return resolve(p).toLowerCase();
}

export function sideOf(filePath) {
  const p = norm(filePath);
  if (p.startsWith(norm(FRONT) + sep)) return 'front';
  if (p.startsWith(norm(BACK) + sep)) return 'back';
  return null;
}

function stateFile(sessionId) {
  return join(STATE_DIR, `${(sessionId || 'default').replace(/[^\w-]/g, '')}.json`);
}

export function readState(sessionId) {
  const f = stateFile(sessionId);
  if (!existsSync(f)) return { front: false, back: false };
  try {
    return JSON.parse(readFileSync(f, 'utf8'));
  } catch {
    return { front: false, back: false };
  }
}

export function writeState(sessionId, state) {
  mkdirSync(STATE_DIR, { recursive: true });
  if (!state.front && !state.back) {
    rmSync(stateFile(sessionId), { force: true });
    return;
  }
  writeFileSync(stateFile(sessionId), JSON.stringify(state));
}
