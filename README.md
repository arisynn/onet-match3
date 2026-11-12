# Onet + Triple Match

Game Android/desktop berbasis Kotlin + libGDX, beserta aset gambar dan logic JavaScript dari [`arisynn/connectify-new`](https://github.com/arisynn/connectify-new), untuk dasar game baru. Repo ini **bukan salinan aplikasi Connectify**: UI, login, server, database, multiplayer, dan game lain dari sumber tidak disertakan. Aplikasi Kotlin di repo ini memiliki menu dan layar game sendiri; lihat bagian Android/desktop di bawah.

**Match 3 di sumber adalah Triple Match berbasis tray:** ambil tile teratas dari 12 tumpukan, kumpulkan tiga gambar sama, dan hindari memenuhi tujuh slot tray. Bukan model swap/cascade ala Candy Crush.


## Aplikasi Android dan desktop

JDK 21 dan Android SDK 35 diperlukan untuk build Gradle. Android minimum API 24. Tambahkan `sdk.dir=/path/to/android-sdk` ke `local.properties` atau set `ANDROID_HOME`.

```sh
./gradlew :core:test :headless:test
./gradlew :android:assembleDebug
./gradlew :desktop:run
# Dengan emulator/perangkat terhubung:
./gradlew :android:connectedDebugAndroidTest
```

APK debug: `android/build/outputs/apk/debug/android-debug.apk`. Build release bertanda tangan menggunakan keystore dan empat variabel `ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` melalui workflow yang sudah ada. Jangan commit keystore atau token.

Menu, level, papan Onet, Triple Match, pause, dan pengaturan dirender pada Android maupun desktop. Candy dan Ocean dapat dipilih di koleksi. Audio dan pembelian belum tersedia; progres sesi belum disimpan ke disk. Detail audit dan batas pengujian: [audit startup Android](docs/ANDROID_STARTUP_AUDIT.md).

Contoh workflow di `docs/android-smoke-workflow.yml` dapat dipasang sebagai `.github/workflows/android-smoke.yml` untuk menjalankan APK pada emulator API 29, memeriksa konten visual, kedua mode, pause/pengaturan/resume, restart, dan background/foreground. Jika workflow itu diaktifkan, screenshot dan laporan tersedia sebagai artifact `android-startup-smoke-evidence`.

## Isi

| Folder | Isi |
| --- | --- |
| `src/onet/` | Papan, jalur maksimal dua belokan, arah panah, tile pecah, hint, shuffle, kesulitan, skor/combo, timer dan transisi ronde |
| `src/match3/` | Generator tumpukan, tray, pencocokan trio, skor, undo satu kali, pause dan timer |
| `src/shared/` | ID tile, pemetaan gambar, ukuran papan dan seeded random |
| `assets/tiles/` | 273 tile siap dipakai, termasuk 64 PNG dasar dan 50 WebP Reward hasil perbaikan transparansi |
| `assets/atlases/` | Sembilan atlas transparan hasil edit sebelum dipotong per tile |
| `assets/originals/` | Tile/atlas lama dan source sheet asli yang dipertahankan tanpa perubahan |
| `assets/surfaces/`, `assets/effects/`, `assets/tools/` | Aset permukaan tile, efek, ikon Onet/Triple, hint dan shuffle |
| `docs/` | Asal file, checksum, catatan ekstraksi dan perubahan gambar |

316 file gambar asli dipertahankan byte-for-byte, termasuk aset yang sudah transparan. File yang sudah transparan ditempatkan langsung pada folder aset terkait. Empat set lama (`candy`, `ocean`, `forest`, `space`) mendapat **64 PNG turunan transparan** melalui image editing; mentahan aslinya tetap di `assets/originals/public/art/`. Seluruh 297 aset siap pakai juga ditinjau. Lima set Reward (Sakura/Bakery/Galaxy/Meadow/Fruit) mendapat 50 WebP hasil regenerasi untuk menghilangkan checkerboard yang tercetak, backplate, dan sisa potongan. Nomor file/ID tetap sama; ukuran 512×512, objek terpusat dengan ruang tepi yang seragam. File Reward lama dipindah ke `assets/originals/public/art/`. Hasil edit dapat memiliki perbedaan detail dibanding sumber dan tidak diklaim identik piksel. Source sheet sengaja tetap berisi latar asli; gunakan `assets/tiles/` saat merender game.

Pratinjau perbaikan Reward: [latar terang](docs/previews/reward-clean-light.jpg) · [latar gelap](docs/previews/reward-clean-dark.jpg). [Catatan audit seluruh aset](docs/asset-audit.json).

## Paket UI dan visual tambahan

Tersedia **Cozy Ceramic UI Kit**: 122 file siap pakai (termasuk 32 frame), empat strip animasi alternatif, dan sumber atlas. Berisi badan tile empat bentuk × delapan warna, tray/slot, panel kosong, dekorasi, ilustrasi hasil ronde, delapan latar, serta empat animated WebP. Tidak menambahkan ikon objek pasangan atau aplikasi UI.

[Lihat daftar dan cara pakai](docs/ui-kit.md) · [Pratinjau aset](docs/previews/ui-kit-light.jpg) · [Pratinjau latar](docs/previews/ui-kit-backgrounds.jpg) · [Frame efek](docs/previews/ui-kit-effects.jpg).

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

Tes memeriksa aturan inti dan transisi hasil ekstraksi. Pemeriksaan aset memvalidasi checksum semua gambar asli/turunan dan memastikan setiap ID memiliki file. Aplikasi native tersedia di modul Kotlin; pengujian Android ada di `android/src/androidTest/`.

## Sumber

Diekstrak dari commit [`57160aa2c78fc6ec2fbfbdfbcc362dd3e6837471`](https://github.com/arisynn/connectify-new/tree/57160aa2c78fc6ec2fbfbdfbcc362dd3e6837471). Rincian ada di [catatan ekstraksi](docs/extraction.md), [provenance](docs/provenance.json), dan [transparansi](docs/transparency.json). Tidak menambahkan lisensi baru atas kode/aset sumber.
