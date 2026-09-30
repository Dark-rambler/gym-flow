// PostToolUse (Edit|Write|MultiEdit): remember which project (front/back) Claude touched this session,
// so the Stop hook only compiles what changed.
import { readInput, sideOf, readState, writeState } from './lib.mjs';

const input = readInput();
const filePath = String(input.tool_input?.file_path ?? '');
const side = sideOf(filePath);
if (!side) process.exit(0);

const relevant = side === 'front'
  ? /\.(ts|html)$/i.test(filePath) && /[\\/]src[\\/]/i.test(filePath)
  : /\.(java|ya?ml|sql|kts)$/i.test(filePath);
if (!relevant) process.exit(0);

const state = readState(input.session_id);
state[side] = true;
writeState(input.session_id, state);
