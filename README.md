# Onet + Triple Match — mentahan

Kumpulan aset gambar dan logic game dari [`arisynn/connectify-new`](https://github.com/arisynn/connectify-new), untuk dasar game baru. Repo ini **bukan salinan aplikasi Connectify** dan tidak memuat UI, login, server, toko, ekonomi/reward, database, multiplayer, atau game lainnya.

**Match 3 di sumber adalah Triple Match berbasis tray:** ambil tile teratas dari 12 tumpukan, kumpulkan tiga gambar sama, dan hindari memenuhi tujuh slot tray. Bukan model swap/cascade ala Candy Crush.

## Isi

| Folder | Isi |
| --- | --- |
| `src/onet/` | Papan, jalur maksimal dua belokan, arah panah, tile pecah, hint, shuffle, kesulitan, skor/combo, timer dan transisi ronde |
| `src/match3/` | Generator tumpukan, tray, pencocokan trio, skor, undo satu kali, pause dan timer |
| `src/shared/` | ID tile, pemetaan gambar, ukuran papan dan seeded random |
| `assets/tiles/` | 273 tile siap dipakai, termasuk 64 PNG hasil penghapusan latar |
| `assets/atlases/` | Empat atlas transparan hasil edit sebelum dipotong per tile |
| `assets/originals/` | Tile/atlas lama dan source sheet asli yang dipertahankan tanpa perubahan |
| `assets/surfaces/`, `assets/effects/`, `assets/tools/` | Aset permukaan tile, efek, ikon Onet/Triple, hint dan shuffle |
| `docs/` | Asal file, checksum, catatan ekstraksi dan perubahan gambar |

316 file gambar asli dipertahankan byte-for-byte, termasuk aset yang sudah transparan. File yang sudah transparan ditempatkan langsung pada folder aset terkait. Empat set lama (`candy`, `ocean`, `forest`, `space`) mendapat **64 PNG turunan transparan** melalui image editing; mentahan aslinya tetap di `assets/originals/public/art/`. Hasil edit dapat memiliki perbedaan detail dibanding sumber dan tidak diklaim identik piksel. Source sheet sengaja tetap berisi latar asli; gunakan `assets/tiles/` saat merender game.

## Pemakaian logic

ES modules JavaScript, tanpa dependency runtime, React, DOM, atau request jaringan. Node.js 20+ untuk menjalankan pemeriksaan; modul logic juga dapat digunakan dari bundler browser/game engine.

```js
import {onet, match3, tileIdsFor, tileAssetFor, seededRandom} from './src/index.js';

let round = onet.createOnetRound({theme: 'candy', level: 4, rng: seededRandom('level-4')});
const hint = onet.hintFor(round);
round = onet.selectTile(round, hint.p1.r, hint.p1.c, 0);
round = onet.selectTile(round, hint.p2.r, hint.p2.c, 100);
// round.event menyediakan path dan durasi animasi opsional (480 ms).
// Render animasinya sendiri, kemudian selesaikan match:
round = onet.resolveMatch(round);
round = onet.tickOnetRound(round, 1);
if (onet.needsShuffle(round)) round = onet.shuffleRound(round);

let triple = match3.createTripleRound(tileIdsFor('candy'), {rng: seededRandom('triple')});
triple = match3.startTripleRound(triple);
triple = match3.pickTile(triple, 0);
triple = match3.undoPick(triple);
const result = match3.tripleResult(triple);

const imagePath = tileAssetFor('candy_0'); // assets/tiles/candy/0.png
```

Host mengatur jadwal tick, pause saat aplikasi tidak aktif, animasi, suara, penyimpanan, dan rendering. Ronde Onet mengunci input selama `pending` hingga `resolveMatch()`. Helper `needsShuffle()` memberi sinyal papan buntu; host memutuskan kapan memanggil shuffle. Tidak ada pemotongan inventori atau permintaan reward ke layanan lama. Jalur gambar relatif terhadap root repo; sesuaikan base URL pada proyek pemakai.

## Pemeriksaan cepat

Tidak perlu `npm install`.

```sh
npm test
npm run check:assets
```

Tes memeriksa aturan inti dan transisi hasil ekstraksi. Pemeriksaan aset memvalidasi checksum semua gambar asli/turunan dan memastikan setiap ID memiliki file. Tidak ada aplikasi/demo UI atau QA HP dalam repo ini.

## Sumber

Diekstrak dari commit [`57160aa2c78fc6ec2fbfbdfbcc362dd3e6837471`](https://github.com/arisynn/connectify-new/tree/57160aa2c78fc6ec2fbfbdfbcc362dd3e6837471). Rincian ada di [catatan ekstraksi](docs/extraction.md), [provenance](docs/provenance.json), dan [transparansi](docs/transparency.json). Tidak menambahkan lisensi baru atas kode/aset sumber.
