#!/usr/bin/env python3
"""Losslessly transcode static WebP tile art for native Pixmap loading; preserve originals."""
from pathlib import Path
import json, hashlib
from PIL import Image
root=Path(__file__).resolve().parents[1]
manifest=[]
for source in sorted((root/'assets/tiles').rglob('*.webp')):
 image=Image.open(source).convert('RGBA')
 output=root/'assets/tiles-native'/source.relative_to(root/'assets/tiles').with_suffix('.png')
 output.parent.mkdir(parents=True,exist_ok=True)
 image.save(output,optimize=True)
 with Image.open(output) as check: assert check.tobytes()==image.tobytes()
 manifest.append({'source':str(source.relative_to(root)), 'path':str(output.relative_to(root)),
  'sourceSha256':hashlib.sha256(source.read_bytes()).hexdigest(), 'sha256':hashlib.sha256(output.read_bytes()).hexdigest()})
(root/'assets/tiles-native/manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
print('Lossless native tile transcodes:',len(manifest))
