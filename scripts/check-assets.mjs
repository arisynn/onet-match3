import {readFile, access} from 'node:fs/promises';
import {createHash} from 'node:crypto';
import {TILE_SETS,tileAssetFor} from '../src/shared/tiles.js';
const root = new URL('../', import.meta.url);
const provenance=JSON.parse(await readFile(new URL('docs/provenance.json',root)));
const edited=JSON.parse(await readFile(new URL('docs/transparency.json',root)));
for (const asset of [...provenance.assets,...edited.tiles,...(edited.atlases || [])]) {
  const bytes=await readFile(new URL(asset.path,root));
  if(createHash('sha256').update(bytes).digest('hex')!==asset.sha256) throw new Error(`Hash mismatch: ${asset.path}`);
}
const ids = new Set(Object.values(TILE_SETS).flat());
for(const id of ids){ const path=tileAssetFor(id); if(!path)throw new Error(`Missing mapping: ${id}`); await access(new URL(path,root)); }
console.log(JSON.stringify({originalAssetsVerified:provenance.assets.length,transparentDerivatives:edited.tiles.length,atlasesVerified:edited.atlases?.length || 0,tileIdsResolved:ids.size}));
