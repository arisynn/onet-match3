import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtemp, mkdir, writeFile, rm} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {createHash} from 'node:crypto';
import {checkRights} from '../scripts/check-commercial-rights.mjs';
const hash = text => createHash('sha256').update(text).digest('hex');

async function fixture(run) {
  const root = await mkdtemp(join(tmpdir(), 'onet-rights-'));
  try {
    await mkdir(join(root,'assets'));
    await mkdir(join(root,'docs'));
    await writeFile(join(root,'assets','fixture.txt'),'fixture');
    await writeFile(join(root,'docs','license.txt'),'Explicit commercial permission for fixture');
    const data = {schemaVersion:1,assets:{'assets/fixture.txt':{status:'confirmed',commercialUseAllowed:true,sha256:hash('fixture'),license:'fixture license',source:'self-authored test fixture',evidencePath:'docs/license.txt',evidenceSha256:hash('Explicit commercial permission for fixture')}}};
    const save = () => writeFile(join(root,'docs','commercial-asset-rights.json'),JSON.stringify(data));
    await save();
    await run(root,data,save);
  } finally { await rm(root,{recursive:true}); }
}

test('public gate accepts only matching asset and explicit local license evidence',async()=>fixture(async root=>{
  assert.equal((await checkRights(root)).blocked,0);
}));
test('public gate blocks unknown and newly bundled files',async()=>fixture(async(root,data,save)=>{
  data.assets['assets/fixture.txt'].status='unknown'; await save();
  await writeFile(join(root,'assets','unexpected.txt'),'new');
  assert.equal((await checkRights(root)).blocked,2);
}));
test('public gate blocks changed asset and missing or changed license evidence',async()=>fixture(async(root,data,save)=>{
  await writeFile(join(root,'assets','fixture.txt'),'changed');
  assert.equal((await checkRights(root)).blocked,1);
  data.assets['assets/fixture.txt'].sha256=hash('changed'); await save();
  await writeFile(join(root,'docs','license.txt'),'different terms');
  assert.equal((await checkRights(root)).blocked,1);
  await rm(join(root,'docs','license.txt'));
  assert.equal((await checkRights(root)).blocked,1);
}));
