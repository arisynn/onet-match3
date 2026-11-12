// Standalone state transitions extracted from useOnetGame.ts, without React,
// account inventories, rewards, audio, navigation or background network requests.
import {generateBoard, getPath, findHint, countRemaining, guaranteedShuffle, clearMatchedGroups, canMatch, getMatchAction} from './board.js';
import {getOnetDifficulty} from './difficulty.js';
export const MATCH_ANIMATION_MS = 480;
export const COMBO_WINDOW_MS = 4000;
export function createOnetRound({theme = 'sweets', level = 1, rng = Math.random, board, seconds} = {}) {
  level = Math.max(1, Math.floor(Number(level)||1));
  const duration = seconds ?? getOnetDifficulty(level).time;
  if (!Number.isFinite(duration) || duration <= 0) throw new Error('seconds must be positive.');
  return {theme, level, board: board ? structuredClone(board) : generateBoard(theme, level, rng), phase: 'playing', paused: false,
    selected: null, pending: null, score: 0, combo: 0, highestCombo: 0, lastMatchAt: null, matches: 0, mistakes: 0, splits: 0,
    time: duration, duration, event: null};
}
function finish(state) {
  if (state.pending) return state;
  if (countRemaining(state.board) === 0) return {...state, phase: 'won', selected: null};
  if (state.time === 0) return {...state, phase: 'lost', selected: null};
  return state;
}
export const pauseOnetRound = (state, paused = true) => state.phase === 'playing' ? {...state, paused} : state;
export function selectTile(state, r, c, now = Date.now()) {
  if (state.phase !== 'playing' || state.paused || state.pending || !Number.isInteger(r) || !Number.isInteger(c) || !state.board[r]?.[c]) return state;
  let tile = state.board[r][c];
  if (tile.isSlave) { r = tile.masterR; c = tile.masterC; tile = state.board[r][c]; }
  const b = {r,c}, a = state.selected;
  if (!a) return {...state, selected: b, event: null};
  if (a.r === r && a.c === c) return {...state, selected: null, event: null};
  if (!canMatch(state.board[a.r]?.[a.c], tile)) return {...state, selected: b, event: null};
  const route = getPath(state.board, a.r, a.c, r, c);
  if (!route?.path) return {...state, selected: null, mistakes: state.mistakes+1, event: {type:'blocked', tiles:[a,b]}};
  const combo = state.lastMatchAt !== null && now-state.lastMatchAt < COMBO_WINDOW_MS ? state.combo+1 : 1;
  const action = getMatchAction(state.board, a, b);
  const baseScore = action === 'split_to_1x2' ? 200 : action === 'split_to_1x1' ? 150 : 100;
  const pending = {a,b,action,path:route.path,points:baseScore+(combo-1)*10};
  return {...state, selected:null, pending, combo, highestCombo:Math.max(state.highestCombo,combo), lastMatchAt:now,
    matches:state.matches+1, splits:state.splits+Number(action!=='clear'), event:{type:'match',...pending,duration:MATCH_ANIMATION_MS}};
}
// Call after the optional 480ms UI animation; keeping pending locks further picks.
export function resolveMatch(state) {
  if (!state.pending || state.phase !== 'playing') return state;
  const {a,b,points} = state.pending;
  return finish({...state, board:clearMatchedGroups(state.board,a,b,state.theme), score:state.score+points, pending:null, event:null});
}
export function tickOnetRound(state, seconds = 1) {
  if (state.phase !== 'playing' || state.paused) return state;
  if (!Number.isFinite(seconds) || seconds < 0) throw new Error('seconds must be finite and nonnegative.');
  return finish({...state, time:Math.max(0,state.time-seconds)});
}
export function hintFor(state) {
  if (state.phase !== 'playing' || state.paused || state.pending) return null;
  return findHint(state.board);
}
export function shuffleRound(state, rng = Math.random) {
  if (state.phase !== 'playing' || state.paused || state.pending) return state;
  return {...state, board:guaranteedShuffle(state.board,rng,state.level), selected:null, event:{type:'shuffle'}};
}
export const needsShuffle = state => state.phase === 'playing' && !state.pending && countRemaining(state.board)>0 && !findHint(state.board);
export const onetResult = state => ({phase:state.phase,level:state.level,score:state.score,matches:state.matches,splits:state.splits,
  mistakes:state.mistakes,isFlawless:state.phase==='won'&&state.mistakes===0,highestCombo:state.highestCombo,
  remainingTime:state.time,timeElapsed:(state.duration-state.time)*1000});
