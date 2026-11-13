#!/usr/bin/env node
/** Fail-closed public-release check. This is an evidence inventory, not legal advice. */
import {readFile, readdir, realpath} from 'node:fs/promises';
import {createHash} from 'node:crypto';
import {resolve, relative, sep} from 'node:path';
import {fileURLToPath} from 'node:url';

const sha256 = bytes => createHash('sha256').update(bytes).digest('hex');
const within = (root, file) => { const rel = relative(root, file); return rel !== '..' && !rel.startsWith('..' + sep) && !rel.startsWith(sep); };

export async function checkRights(root, manifestFile = 'docs/commercial-asset-rights.json') {
  root = await realpath(root);
  const manifest = JSON.parse(await readFile(resolve(root, manifestFile), 'utf8'));
  if (manifest.schemaVersion !== 1 || !manifest.assets || typeof manifest.assets !== 'object') throw new Error('Unsupported rights manifest');
  const assetsRoot = resolve(root, 'assets');
  const paths = [];
  async function walk(dir) {
    for (const item of await readdir(dir, {withFileTypes:true})) {
      const path = resolve(dir, item.name);
      if (item.isDirectory()) await walk(path);
      else paths.push(path);
    }
  }
  await walk(assetsRoot);
  const failures = [];
  let confirmed = 0;
  for (const path of paths.sort()) {
    const key = relative(root, path).split(sep).join('/');
    const entry = manifest.assets[key];
    const resolved = await realpath(path);
    if (!within(assetsRoot, resolved)) { failures.push({path:key, reason:'asset escapes assets directory'}); continue; }
    if (!entry || entry.status !== 'confirmed' || entry.commercialUseAllowed !== true) {
      failures.push({path:key, reason:'commercial rights not confirmed'}); continue;
    }
    if (entry.sha256 !== sha256(await readFile(path))) { failures.push({path:key, reason:'asset hash differs from reviewed file'}); continue; }
    if (!entry.license || !entry.evidencePath || !entry.evidenceSha256 || !entry.source) {
      failures.push({path:key, reason:'confirmation has incomplete evidence'}); continue;
    }
    try {
      const evidence = await realpath(resolve(root, entry.evidencePath));
      if (!within(root, evidence) || entry.evidenceSha256 !== sha256(await readFile(evidence))) throw new Error('invalid evidence');
    } catch {
      failures.push({path:key, reason:'license evidence absent, changed or outside project'}); continue;
    }
    confirmed++;
  }
  return {schemaVersion:1, scope:'all files currently bundled from assets/', checked:paths.length, confirmed, blocked:failures.length, failures};
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const args = process.argv.slice(2);
    const root = args[0] || fileURLToPath(new URL('../', import.meta.url));
    const result = await checkRights(root, args[1]);
    console.log(JSON.stringify({...result, failures: result.failures.slice(0, 15)}, null, 2));
    if (result.blocked) {
      console.error(`PUBLIC RELEASE BLOCKED: ${result.blocked} bundled files lack matching commercial-use evidence. Originals are preserved. Internal debug validation is separate.`);
      process.exitCode = 1;
    }
  } catch (error) {
    console.error('PUBLIC RELEASE BLOCKED: ' + error.message);
    process.exitCode = 1;
  }
}
