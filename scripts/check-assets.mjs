import {readFile, access} from 'node:fs/promises';
import {createHash} from 'node:crypto';
import {TILE_SETS,tileAssetFor} from '../src/shared/tiles.js';
import {UI_ASSETS,UI_ANIMATIONS} from '../src/shared/visuals.js';
const root = new URL('../', import.meta.url);
const provenance=JSON.parse(await readFile(new URL('docs/provenance.json',root)));
const edited=JSON.parse(await readFile(new URL('docs/transparency.json',root)));
for (const asset of [...provenance.assets,...edited.tiles,...(edited.atlases || [])]) {
  const bytes=await readFile(new URL(asset.path,root));
  if(createHash('sha256').update(bytes).digest('hex')!==asset.sha256) throw new Error(`Hash mismatch: ${asset.path}`);
}
const ui=JSON.parse(await readFile(new URL('assets/ui-kit/manifest.json',root)));
for(const asset of [...ui.assets,...ui.sources,...ui.animations.map(a=>({path:a.spriteStrip,sha256:a.spriteStripSha256}))]) {
 const bytes=await readFile(new URL(asset.path,root));
 if(createHash('sha256').update(bytes).digest('hex')!==asset.sha256)throw new Error(`UI asset hash mismatch: ${asset.path}`);
}
if(Object.keys(UI_ASSETS).length!==ui.readyFileCount)throw new Error('UI catalog count mismatch');
for(const asset of ui.assets)if(UI_ASSETS[asset.id]!==asset.path)throw new Error(`UI mapping mismatch: ${asset.id}`);
for(const animation of Object.values(UI_ANIMATIONS))for(const path of [...animation.frames,animation.spriteStrip,animation.path])await access(new URL(path,root));
const ids = new Set(Object.values(TILE_SETS).flat());
for(const id of ids){ const path=tileAssetFor(id); if(!path)throw new Error(`Missing mapping: ${id}`); await access(new URL(path,root)); }
console.log(JSON.stringify({originalAssetsVerified:provenance.assets.length,transparentDerivatives:edited.tiles.length,atlasesVerified:edited.atlases?.length || 0,tileIdsResolved:ids.size,uiAssetsVerified:ui.assets.length,uiAnimations:ui.animations.length}));
