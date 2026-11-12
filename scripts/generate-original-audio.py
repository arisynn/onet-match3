#!/usr/bin/env python3
"""Original procedural audio for this project; no sampled audio or external melodies.
Authored for arisynn/onet-match3 by the Codex development agent, 2026-11-12.
Output is PCM16 mono WAV, 22050Hz. No dependencies outside Python stdlib.
"""
from pathlib import Path
import hashlib, json, math, struct, wave
RATE = 22050
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'assets/audio'
OUT.mkdir(exist_ok=True)

def render(name, notes, duration, level=.22):
    data = [0.] * round(duration * RATE)
    for start, pitch, length, gain in notes:
        frequency = 440 * 2 ** ((pitch - 69) / 12)
        for n in range(round(length * RATE)):
            t = n / RATE
            index = round(start * RATE) + n
            if index >= len(data): break
            attack = min(1., t / .012)
            tail = min(1., max(0., (length-t) / .06))
            envelope = attack * math.exp(-3.8*t/length) * tail
            tone = math.sin(2*math.pi*frequency*t) + .20*math.sin(4*math.pi*frequency*t) + .055*math.sin(6*math.pi*frequency*t)
            data[index] += gain * level * envelope * tone
    # Music loop has silence at both boundaries, avoiding a looping discontinuity.
    pcm = b''.join(struct.pack('<h', round(max(-.85,min(.85,v))*32767)) for v in data)
    path = OUT / (name + '.wav')
    with wave.open(str(path), 'wb') as f:
        f.setnchannels(1); f.setsampwidth(2); f.setframerate(RATE); f.writeframes(pcm)
    return {'path':'assets/audio/'+path.name,'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'frames':len(data),'rate':RATE,'channels':1,'peak':max(abs(v) for v in data)}

sounds = {
 'select':[(0,79,.14,1)], 'match':[(0,76,.35,1),(.09,83,.4,.9)],
 'invalid':[(0,53,.18,.7),(.06,52,.18,.5)],
 'combo':[(0,76,.28,.8),(.08,79,.32,.85),(.16,86,.45,.9)],
 'victory':[(0,72,.5,.8),(.14,76,.5,.8),(.28,79,.5,.8),(.48,84,.9,.9)],
 'defeat':[(0,64,.45,.8),(.2,60,.7,.8)],
 'claim':[(0,79,.25,.7),(.1,84,.45,.8)],
 'achievement':[(0,72,.4,.7),(.1,79,.4,.8),(.2,88,.8,.75)],
 'preview':[(0,81,.25,.7),(.13,86,.45,.7)],
 'level-up':[(0,72,.4,.7),(.12,76,.4,.7),(.24,79,.4,.7),(.36,84,.7,.8)],
 'undo':[(0,76,.2,.7),(.09,72,.3,.7)],
 'shuffle':[(0,72,.2,.6),(.06,76,.2,.6),(.12,79,.3,.7)],
}
manifest=[render(name,notes,max(a+c for a,b,c,d in notes)+.1) for name,notes in sounds.items()]
# Original generative ambient pattern built from a pentatonic voicing. No song quotation.
notes=[]
voicings=[(48,60,67,74),(45,57,64,72),(41,53,60,69),(43,55,62,72)]
for bar, chord in enumerate(voicings*2):
    for index,pitch in enumerate(chord):
        notes.append((bar*4+.18+index*.48,pitch,2.7,.48 if index==0 else .33))
manifest.append(render('garden-loop',notes,32.,.16))
(OUT/'manifest.json').write_text(json.dumps({'provenance':'Original mathematical synthesis; no recordings or third-party musical work used.','generator':'scripts/generate-original-audio.py','license':'CC0-1.0','assets':manifest},indent=2)+'\n')
print('Generated',len(manifest),'original audio files.')
