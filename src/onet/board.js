import { ROWS, COLS } from '../shared/config.js';
import { getOnetDifficulty } from './difficulty.js';
import { tileIdsFor } from '../shared/tiles.js';

export const groupSize = tile => ({ rows: tile?.groupRows || (tile?.isSplit && tile.splitOrientation === 'vertical' ? 2 : 1), cols: tile?.groupCols || (tile?.isSplit && tile.splitOrientation === 'horizontal' ? 2 : 1) });
export const getTileCells = (r, c, tile) => {
  const { rows, cols } = groupSize(tile);
  return Array.from({ length: rows * cols }, (_, i) => ({ r: r + Math.floor(i / cols), c: c + i % cols }));
};
export const canMatch = (a,b) => !!a && !!b && !a.isSlave && !b.isSlave && a.id === b.id && groupSize(a).rows === groupSize(b).rows && groupSize(a).cols === groupSize(b).cols;
const shuffle = (items, rng = Math.random) => {
  const out = [...items];
  for (let i = out.length - 1; i > 0; i--) { const j = Math.floor(rng() * (i+1)); [out[i], out[j]] = [out[j], out[i]]; }
  return out;
};
export const getPath = (board, r1, c1, r2, c2, ignoredTiles = []) => {
  const a = board?.[r1]?.[c1], b = board?.[r2]?.[c2];
  if (!canMatch(a,b) || (r1 === r2 && c1 === c2)) return null;
  const starts = getTileCells(r1,c1,a), ends = getTileCells(r2,c2,b);
  const endpoint = new Set(ends.map(p => `${p.r},${p.c}`));
  const allowed = new Set([...starts,...ends,...ignoredTiles.flatMap(p => getTileCells(p.r,p.c,board[p.r][p.c]))].map(p => `${p.r},${p.c}`));
  const dirs = [[-1,0,'up','down'],[0,1,'right','left'],[1,0,'down','up'],[0,-1,'left','right']];
  for (const start of starts) {
    const queue = [{...start, dir:-1, turns:0, points:[start]}], seen = new Map();
    for (let index = 0; index < queue.length; index++) {
      const cur = queue[index];
      for (let dir=0; dir<4; dir++) {
        const [dr,dc,name,opposite] = dirs[dir];
        if (cur.dir === -1 && a.openSide && a.openSide !== name) continue;
        const turns = cur.turns + (cur.dir !== -1 && cur.dir !== dir ? 1 : 0);
        if (turns > 2) continue;
        const r=cur.r+dr, c=cur.c+dc;
        if (r<0 || c<0 || r>=board.length || c>=board[0].length || (board[r][c] !== 0 && !allowed.has(`${r},${c}`))) continue;
        const points = cur.dir === dir ? [...cur.points.slice(0,-1),{r,c}] : [...cur.points,{r,c}];
        if (endpoint.has(`${r},${c}`) && (!b.openSide || b.openSide === opposite)) return {path: points};
        const key = `${r},${c},${dir}`;
        if ((seen.get(key) ?? 3) <= turns) continue;
        seen.set(key,turns); queue.push({r,c,dir,turns,points});
      }
    }
  }
  return { error:'NO_PATH' };
};
export const findHint = board => {
  const tiles=[];
  for (let r=1;r<=ROWS;r++) for(let c=1;c<=COLS;c++) if(board[r]?.[c] && !board[r][c].isSlave) tiles.push({r,c});
  for(let i=0;i<tiles.length;i++) for(let j=i+1;j<tiles.length;j++) {
    const a=tiles[i],b=tiles[j];
    if(!canMatch(board[a.r][a.c],board[b.r][b.c])) continue;
    const result=getPath(board,a.r,a.c,b.r,b.c);
    if(result?.path) return {p1:a,p2:b,path:result.path};
  }
  return null;
};
export const countRemaining = board => board?.flat().filter(Boolean).length || 0;
export const getMatchAction = (board,a) => {
  const {rows,cols}=groupSize(board?.[a?.r]?.[a?.c]);
  return rows*cols===4?'split_to_1x2':rows*cols===2?'split_to_1x1':'clear';
};
const placeGroup = (board,r,c,tile) => {
  for(const p of getTileCells(r,c,tile)) board[p.r][p.c]=p.r===r&&p.c===c?tile:{id:tile.id,isSlave:true,masterR:r,masterC:c};
};
// Corresponding children of BOTH parents share IDs; siblings always differ.
// No clock/randomness here: replays, daily seeds and multiplayer stay deterministic.
export const clearMatchedGroups = (board,a,b,themeKey) => {
  const next=board.map(row=>[...row]);
  const tile=board[a.r]?.[a.c];
  if(!canMatch(tile,board[b.r]?.[b.c])) return next;
  const {rows,cols}=groupSize(tile), area=rows*cols;
  if(area===1){ next[a.r][a.c]=0;next[b.r][b.c]=0;return next; }
  const theme=themeKey || tile.themeKey || (String(tile.id).match(/^(candy|ocean|forest|space)_/)?.[1]) || 'vanilla';
  const pool=tileIdsFor(theme), parentIndex=Math.max(0,pool.indexOf(tile.id));
  const depth=(tile.splitDepth||0)+1;
  const horizontal=area===4 ? tile.splitAxis!=='vertical' : cols>1;
  for(const p of [a,b]) for(let i=0;i<2;i++) {
    const childRows=area===4?(horizontal?1:2):1, childCols=area===4?(horizontal?2:1):1;
    const r=p.r+(horizontal&&area===4 || !horizontal&&area===2 ? i:0);
    const c=p.c+(!horizontal&&area===4 || horizontal&&area===2 ? i:0);
    const id=pool[(parentIndex+1+depth*2+i*(depth+2))%pool.length];
    placeGroup(next,r,c,{id,themeKey:theme,groupRows:childRows,groupCols:childCols,isSplit:area===4,openSide:null,
      splitDepth:depth,splitAxis:tile.splitAxis,justSplit:true,splitAnimKey:`${tile.id}-${depth}-${r}-${c}`,
      splitFromX:horizontal&&area===2 || !horizontal&&area===4 ? (i===0?1:-1):0,
      splitFromY:horizontal&&area===4 || !horizontal&&area===2 ? (i===0?1:-1):0});
  }
  return next;
};
export const isBoardSolvable = input => {
  let board=input;
  for(let step=0;step<150;step++) {
    if(!countRemaining(board))return true;
    const hint=findHint(board);if(!hint)return false;
    board=clearMatchedGroups(board,hint.p1,hint.p2);
  }
  return false;
};
export const generateBoard = (themeKey,level=1,rng=Math.random) => {
  const ids=shuffle(tileIdsFor(themeKey),rng), difficulty=getOnetDifficulty(level);
  const board=Array.from({length:ROWS+2},()=>Array(COLS+2).fill(0));
  // Paired footprints on exposed edges: progressively separated, never paywalled.
  for(let n=0;n<difficulty.groups;n++) {
    const rows=level>=3&&n===0?2:level%2===0?2:1, cols=rows===2&&level>=3&&n===0?2:rows===2?1:2;
    const r=n===0?1:ROWS-rows+1,c=1,other=level>=4?COLS-cols+1:c+cols;
    for(const col of [c,other])placeGroup(board,r,col,{id:ids[n],themeKey,groupRows:rows,groupCols:cols,isSplit:true,openSide:null,splitAxis:rng()<.5?'horizontal':'vertical'});
    // Pair must split along the same axis.
    board[r][other].splitAxis=board[r][c].splitAxis;
  }
  const positions=[];
  for(let r=1;r<=ROWS;r++)for(let c=1;c<=COLS;c++)if(!board[r][c])positions.push({r,c});
  const tiles=[];
  for(let i=0;i<positions.length/2;i++)for(let j=0;j<2;j++)tiles.push({id:ids[(i+difficulty.groups)%ids.length],themeKey,openSide:null});
  let candidate=board;
  for(let attempt=0;attempt<4;attempt++) {
    candidate=constructPairs(board,positions,tiles,rng,difficulty.separation);
    if(isBoardSolvable(candidate))break;
  }
  return addDirectionTiles(candidate,difficulty.arrowPairs);
};
// Restrict only along an actual complete legal solution; arrows can never invalidate it.
const addDirectionTiles=(candidate,arrowPairs)=>{
  if(arrowPairs&&isBoardSolvable(candidate)){
    let simulation=candidate,added=0;
    for(let step=0;step<100&&added<arrowPairs;step++){
      const h=findHint(simulation);if(!h)break;
      const a=simulation[h.p1.r][h.p1.c],b=simulation[h.p2.r][h.p2.c];
      if(groupSize(a).rows*groupSize(a).cols===1 && [h.p1,h.p2].every(p=>candidate[p.r][p.c]&&!candidate[p.r][p.c].isSlave&&groupSize(candidate[p.r][p.c]).rows*groupSize(candidate[p.r][p.c]).cols===1) && candidate[h.p1.r][h.p1.c]?.id===a.id && candidate[h.p2.r][h.p2.c]?.id===b.id){
        const first=h.path[0],next=h.path[1],last=h.path[h.path.length-1],prev=h.path[h.path.length-2];
        const side=(p,q)=>q.r<p.r?'up':q.r>p.r?'down':q.c<p.c?'left':'right';
        candidate[h.p1.r][h.p1.c]={...candidate[h.p1.r][h.p1.c],openSide:side(first,next)};
        candidate[h.p2.r][h.p2.c]={...candidate[h.p2.r][h.p2.c],openSide:side(last,prev)};added++;
      }
      simulation=clearMatchedGroups(simulation,h.p1,h.p2);
    }
  }
  return candidate;
};
// Build a known sequence by peeling legal pairs from the outside, favoring long routes.
const constructPairs=(input,positions,tiles,rng,distance)=>{
  const result=input.map(row=>row.map(t=>t?{...t}:0));
  const probe=Array.from({length:ROWS+2},()=>Array(COLS+2).fill(0));
  positions.forEach(p=>{probe[p.r][p.c]={id:'probe'};});
  let remaining=[...positions],pair=0;
  while(remaining.length>1){
    const order=shuffle(remaining,rng);let best=null,bestScore=-1;
    search:for(const a of order.slice(0,8))for(const b of order){
      if(a===b)continue;
      const d=Math.abs(a.r-b.r)+Math.abs(a.c-b.c);
      if(d<=bestScore)continue;
      const path=getPath(probe,a.r,a.c,b.r,b.c);
      if(path?.path){best={a,b};bestScore=d;if(d>=distance)break search;}
    }
    if(!best){const h=findHint(probe);if(!h)break;best={a:h.p1,b:h.p2};}
    for(const p of [best.a,best.b]){result[p.r][p.c]={...tiles[pair*2],openSide:null,justSplit:false};probe[p.r][p.c]=0;}
    remaining=remaining.filter(p=>probe[p.r][p.c]);pair++;
  }
  return result;
};
export const guaranteedShuffle = (input,rng=Math.random,level=1) => {
  const board=input.map(row=>row.map(t=>t?{...t,openSide:null}:0));
  const positions=[];
  for(let r=1;r<=ROWS;r++)for(let c=1;c<=COLS;c++)if(board[r][c]&&!board[r][c].isSlave&&groupSize(board[r][c]).rows*groupSize(board[r][c]).cols===1)positions.push({r,c});
  const tiles=positions.map(p=>board[p.r][p.c]);
  for(let i=0;i<5;i++) {
    shuffle(tiles,rng).forEach((tile,i)=>{const p=positions[i];board[p.r][p.c]=tile;});
    if(isBoardSolvable(board))return addDirectionTiles(board,getOnetDifficulty(level).arrowPairs);
  }
  const countsById={};tiles.forEach(t=>{countsById[t.id]=(countsById[t.id]||0)+1;});
  const ordered=shuffle(Object.keys(countsById),rng).flatMap(id=>Array(countsById[id]).fill(tiles.find(t=>t.id===id)));
  const rebuilt=constructPairs(board,positions,ordered,rng,getOnetDifficulty(level).separation);
  if(findHint(rebuilt))return addDirectionTiles(rebuilt,getOnetDifficulty(level).arrowPairs);
  // Emergency recovery only: convert footprints to even unit pairs, then peel legal paths.
  const occupied=[],counts={};
  for(let r=1;r<=ROWS;r++)for(let c=1;c<=COLS;c++)if(board[r][c]){occupied.push({r,c});counts[board[r][c].id]=(counts[board[r][c].id]||0)+1;}
  const pairs=shuffle(Object.entries(counts).flatMap(([id,n])=>Array(Math.floor(n/2)).fill(id)),rng);
  let probe=board.map(row=>row.map(t=>t?{id:'probe'}:0));
  for(const id of pairs){const h=findHint(probe);if(!h)break;for(const p of [h.p1,h.p2])board[p.r][p.c]={id,openSide:null};probe=clearMatchedGroups(probe,h.p1,h.p2);}
  return board;
};
export const placeTilesSmartly = (tiles,currentBoard,positions) => {
  const b=currentBoard?currentBoard.map(row=>[...row]):Array.from({length:ROWS+2},()=>Array(COLS+2).fill(0));
  const pos=positions||Array.from({length:ROWS*COLS},(_,i)=>({r:Math.floor(i/COLS)+1,c:i%COLS+1}));
  shuffle(tiles).forEach((t,i)=>{if(pos[i])b[pos[i].r][pos[i].c]=t;});return guaranteedShuffle(b);
};
