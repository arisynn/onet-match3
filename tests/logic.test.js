import test from 'node:test';
import assert from 'node:assert/strict';
import {onet,match3,tileIdsFor,seededRandom} from '../src/index.js';
const blank = () => Array.from({length:12},()=>Array(8).fill(0));

test('Onet generation is deterministic, paired, and solvable at representative levels',()=>{
 for(const level of [1,4,8,15]) {
  const b=onet.generateBoard('candy',level,seededRandom(`level-${level}`));
  assert.deepEqual(b,onet.generateBoard('candy',level,seededRandom(`level-${level}`)));
  assert.equal(onet.countRemaining(b),60);
  assert.ok(onet.findHint(b));assert.equal(onet.isBoardSolvable(b),true);
 }
});
test('Onet routes use at most two turns; wrong IDs and blocked endpoints fail',()=>{
 const b=blank();b[1][1]={id:'candy_0'};b[1][3]={id:'candy_0'};
 const path=onet.getPath(b,1,1,1,3).path;assert.ok(path.length<=4);
 b[1][3].openSide='down';b[2][3]={id:'obstacle'};assert.equal(onet.getPath(b,1,1,1,3).error,'NO_PATH');
 b[1][3]={id:'candy_1'};assert.equal(onet.getPath(b,1,1,1,3),null);
});
test('Onet split preserves footprints, sibling distinction, and counterpart pairing',()=>{
 const b=onet.generateBoard('candy',4,seededRandom('split'));
 const a={r:1,c:1},z={r:1,c:5};assert.equal(onet.getMatchAction(b,a,z),'split_to_1x2');
 const next=onet.clearMatchedGroups(b,a,z,'candy');
 assert.equal(onet.countRemaining(next),60);
 assert.notEqual(next[1][1].id,next[2][1].id);
 assert.equal(next[1][1].id,next[1][5].id);assert.equal(next[2][1].id,next[2][5].id);
 assert.equal(onet.groupSize(b[1][1]).rows*onet.groupSize(b[1][1]).cols,4);
});
test('Onet matching locks during animation and resolves without mutating previous state',()=>{
 const board=blank();board[1][1]={id:'candy_0'};board[1][2]={id:'candy_0'};
 let s=onet.createOnetRound({board,theme:'candy'});const before=structuredClone(s);
 s=onet.selectTile(s,1,1,0);s=onet.selectTile(s,1,2,100);
 assert.ok(s.pending);assert.equal(onet.selectTile(s,1,1),s);
 s=onet.resolveMatch(s);assert.equal(s.phase,'won');assert.equal(s.score,100);assert.equal(s.matches,1);
 assert.deepEqual(before.board,board);
});
test('Onet pause stops timer; timeout is terminal; shuffle retains a legal move',()=>{
 let s=onet.createOnetRound({theme:'ocean',rng:seededRandom('shuffle')});
 const paused=onet.pauseOnetRound(s);assert.equal(onet.tickOnetRound(paused),paused);
 assert.ok(onet.hintFor(onet.shuffleRound(s,seededRandom('shuffle-again'))));
 s=onet.tickOnetRound(s,s.time);assert.equal(s.phase,'lost');assert.equal(onet.selectTile(s,1,1),s);
});
test('Triple generator preserves source 12x3 stack layout and multiples of three',()=>{
 const stacks=match3.createStacks(tileIdsFor('candy'),seededRandom('triple'));
 assert.equal(stacks.length,12);assert.ok(stacks.every(s=>s.length===3));
 const counts={};for(const id of stacks.flat())counts[id]=(counts[id]||0)+1;
 assert.equal(Object.keys(counts).length,6);assert.ok(Object.values(counts).every(n=>n%3===0));
 assert.deepEqual(stacks,match3.createStacks(tileIdsFor('candy'),seededRandom('triple')));
});
test('Triple third tile clears immediately, scores 150, and wins with empty tray',()=>{
 let s=match3.createTripleRound([], {stacks:[['a'],['a'],['a']]});assert.equal(match3.pickTile(s,0),s);
 s=match3.startTripleRound(s);const before=structuredClone(s);
 for(const i of [0,1,2])s=match3.pickTile(s,i);
 assert.equal(s.phase,'won');assert.equal(s.tray.length,0);assert.equal(match3.tripleScore(s),150);
 assert.deepEqual(before.stacks,[['a'],['a'],['a']]);
});
test('Triple seventh unmatched tile loses; a trio on seventh pick frees the tray first',()=>{
 let s=match3.startTripleRound(match3.createTripleRound([], {stacks:['a','b','c','d','e','f','g'].map(x=>[x])}));
 for(let i=0;i<7;i++)s=match3.pickTile(s,i);assert.equal(s.phase,'lost');
 s=match3.startTripleRound(match3.createTripleRound([], {stacks:['a','a','b','c','d','e','a','b'].map(x=>[x])}));
 for(let i=0;i<7;i++)s=match3.pickTile(s,i);assert.equal(s.phase,'playing');assert.equal(s.tray.length,4);
});
test('Triple undo is single-use and does not refund elapsed time; pause blocks picks',()=>{
 let s=match3.startTripleRound(match3.createTripleRound([], {stacks:[['a','b'],['c']]}));
 const before=structuredClone(s);s=match3.pickTile(s,0);s=match3.tickTripleRound(s,5);s=match3.undoPick(s);
 assert.deepEqual(s.stacks,before.stacks);assert.equal(s.time,115);assert.equal(s.undoUsed,true);
 assert.equal(match3.undoPick(s),s);const paused=match3.pauseTripleRound(s);assert.equal(match3.pickTile(paused,0),paused);
 assert.equal(match3.tickTripleRound(paused),paused);assert.equal(match3.tickTripleRound(s,115).phase,'lost');
});
