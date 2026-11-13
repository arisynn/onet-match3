#!/usr/bin/env python3
"""Losslessly transcode existing static WebP art to libGDX-compatible PNGs.

No resizing, recoloring or composition is performed. Alpha bounds are metadata
for trimming transparent margins at render time. Requires Pillow only to rerun;
outputs are checked in so Android/desktop builds do not need Python.
"""
from pathlib import Path
import hashlib
import json
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / 'assets' / 'ui-native'
SOURCES = {
    'tile-ivory': 'ui-kit/tile-bodies/square-ivory.webp',
    'tile-mint': 'ui-kit/tile-bodies/square-mint.webp',
    'tile-peach': 'ui-kit/tile-bodies/square-peach.webp',
    'tile-rose': 'ui-kit/tile-bodies/square-rose.webp',
    'tile-sky': 'ui-kit/tile-bodies/square-sky.webp',
    'tile-lavender': 'ui-kit/tile-bodies/square-lavender.webp',
    'tile-slate': 'ui-kit/tile-bodies/square-slate.webp',
    'tile-sand': 'ui-kit/tile-bodies/square-sand.webp',

    'button-mint': 'ui-kit/tile-bodies/horizontal-mint.webp',
    'panel-ivory': 'ui-kit/panels/ivory.webp',
    'panel-mint': 'ui-kit/panels/mint.webp',
    'slot-mint': 'ui-kit/slots/mint.webp',
    'spa': 'ui-kit/backgrounds/spa-portrait.webp',
    'victory': 'ui-kit/results/victory.webp',
    'pause': 'ui-kit/results/pause.webp',
    'time-up': 'ui-kit/results/time-up.webp',
    'tray-full': 'ui-kit/results/tray-full.webp',
    'new-level': 'ui-kit/results/new-level.webp',
    'reward': 'ui-kit/results/reward.webp',
    'fern': 'ui-kit/decorations/fern-stones.webp',
    'succulent': 'ui-kit/decorations/succulent-cluster.webp',
    'hint': 'tools/hint.webp',
    'shuffle': 'tools/shuffle.webp',
    'onet': 'tools/onet.webp',
    'triple': 'tools/triple.webp',
}
OUTPUT.mkdir(exist_ok=True)
manifest = {}
for name, source in SOURCES.items():
    original = ROOT / 'assets' / source
    image = Image.open(original).convert('RGBA')
    output = OUTPUT / (name + '.png')
    image.save(output, optimize=True)
    with Image.open(output) as decoded:
        assert decoded.tobytes() == image.tobytes(), name
    x0, y0, x1, y1 = image.getchannel('A').getbbox()
    manifest[name] = {
        'path': 'ui-native/' + output.name,
        'source': 'assets/' + source,
        'sourceSha256': hashlib.sha256(original.read_bytes()).hexdigest(),
        'sha256': hashlib.sha256(output.read_bytes()).hexdigest(),
        'bounds': [x0, y0, x1 - x0, y1 - y0],
    }
(OUTPUT / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
print(f'Converted {len(manifest)} UI assets without changing decoded pixels.')
