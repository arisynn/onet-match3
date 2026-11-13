#!/usr/bin/env python3
"""Build a static weight-600 instance of the bundled OFL font; keep the original.
Requires fonttools. Renaming avoids any ambiguity about modified font names.
"""
from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
ROOT = Path(__file__).resolve().parents[1]
font = instantiateVariableFont(TTFont(ROOT/'assets/fonts/Nunito.ttf', recalcTimestamp=False), {'wght':600}, inplace=False)
font.recalcTimestamp = False
names = {1:'Puzzle Sans',3:'PuzzleSans-SemiBold-1.0',4:'Puzzle Sans SemiBold',6:'PuzzleSans-SemiBold',16:'Puzzle Sans',17:'SemiBold'}
for entry in font['name'].names:
    if entry.nameID in names: entry.string = names[entry.nameID].encode(entry.getEncoding(), errors='replace')
font.save(ROOT/'assets/fonts/PuzzleSans-SemiBold.ttf')
print('Static semibold instance generated; original and license preserved.')
