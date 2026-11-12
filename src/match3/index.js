// Extracted from TripleGame.tsx/useMiniRound.ts. This is tray-based Triple Match,
// not a swap/cascade Match-3 engine. UI, timers and persistence belong to the host.
export const TRAY_CAPACITY = 7;
export const ROUND_SECONDS = 120;
export const TRIO_SCORE = 150;
export function randomize(items, rng = Math.random) {
  const out = [...items];
  for (let i = out.length - 1; i > 0; i--) {
    const j = Math.floor(rng() * (i + 1));
    [out[i], out[j]] = [out[j], out[i]];
  }
  return out;
}
export function createStacks(tileIds, rng = Math.random) {
  const ids = randomize(tileIds, rng).slice(0, 6);
  if (ids.length < 6 || new Set(ids).size !== 6) throw new Error('Triple Match needs at least six distinct tile IDs.');
  const layers = [0,1,2].map(n => randomize(Array.from({length:4}, (_,i) => ids[(i+n)%6]).flatMap(id => [id,id,id]), rng));
  return Array.from({length:12}, (_,i) => layers.map(layer => layer[i]));
}
export function createTripleRound(tileIds, {rng = Math.random, seconds = ROUND_SECONDS, stacks} = {}) {
  if (!Number.isFinite(seconds) || seconds <= 0) throw new Error('seconds must be positive.');
  return {stacks: stacks ? structuredClone(stacks) : createStacks(tileIds, rng), tray: [], cleared: 0,
    phase: 'ready', paused: false, time: seconds, duration: seconds, undo: null, undoUsed: false};
}
export const remainingTiles = state => state.stacks.reduce((n,stack) => n + stack.length, 0);
export const tripleScore = state => state.cleared * TRIO_SCORE;
function finish(state) {
  const remaining = remainingTiles(state);
  if (remaining === 0 || state.tray.length >= TRAY_CAPACITY || state.time === 0) {
    return {...state, phase: remaining === 0 && state.tray.length === 0 ? 'won' : 'lost'};
  }
  return state;
}
export const startTripleRound = state => state.phase === 'ready' ? finish({...state, phase: 'playing'}) : state;
export const pauseTripleRound = (state, paused = true) => state.phase === 'playing' ? {...state, paused} : state;
export function pickTile(state, stackIndex) {
  if (state.phase !== 'playing' || state.paused || state.tray.length >= TRAY_CAPACITY || !Number.isInteger(stackIndex) || !state.stacks[stackIndex]?.length) return state;
  const undo = {stacks: structuredClone(state.stacks), tray: [...state.tray], cleared: state.cleared};
  const stacks = structuredClone(state.stacks), id = stacks[stackIndex].pop();
  const items = [...state.tray, id], matched = items.filter(tile => tile === id).length === 3;
  return finish({...state, stacks, tray: matched ? items.filter(tile => tile !== id) : items,
    cleared: state.cleared + Number(matched), undo});
}
export function undoPick(state) {
  if (!state.undo || state.undoUsed || state.phase !== 'playing' || state.paused) return state;
  return {...state, ...structuredClone(state.undo), undoUsed: true};
}
export function tickTripleRound(state, seconds = 1) {
  if (state.phase !== 'playing' || state.paused) return state;
  if (!Number.isFinite(seconds) || seconds < 0) throw new Error('seconds must be finite and nonnegative.');
  return finish({...state, time: Math.max(0, state.time - seconds)});
}
export const tripleResult = state => ({phase: state.phase, score: tripleScore(state), matches: state.cleared,
  mistakes: Number(state.undoUsed), timeElapsed: (state.duration-state.time)*1000});
