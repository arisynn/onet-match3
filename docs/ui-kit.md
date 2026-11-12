# Cozy Ceramic UI Kit

Paket aset UI dan visual pendukung untuk Onet dan Triple Match. Ini tambahan mentahan, bukan implementasi layar game dan bukan penambahan ikon objek pasangan. Logic game serta seluruh ID tile yang sudah ada tetap sama.

| Jenis | Jumlah | Format siap pakai |
| --- | ---: | --- |
| Badan tile: 1×1, 2×1, 1×2, 2×2 × delapan warna | 32 | WebP transparan |
| Dasar tray kosong | 8 | WebP transparan |
| Cekungan slot terpisah | 8 | WebP transparan |
| Panel kosong | 8 | WebP transparan |
| Dekorasi pinggir/corner | 16 | WebP transparan |
| Ilustrasi: victory, reward, time-up, tray-full, pause, new-level | 6 | WebP transparan |
| Latar: empat portrait, empat landscape | 8 | WebP opaque |
| Animasi: match-burst, hint-pulse, tile-pop-dust, victory-confetti | 4 | Animated WebP |
| Frame individual dari empat animasi | 32 | PNG transparan |
| **Total file siap pakai** | **122** | Termasuk frame individual |

Ada juga **empat strip frame horizontal** (format alternatif, tidak dihitung lagi dalam 122) dan 18 sumber gambar/atlas. Warna: ivory, mint, peach, lavender, sky, rose, sand dan slate. Sumber dibuat dengan image tool, dibersihkan jika perlu, kemudian dipotong/pusatkan tanpa menggambar ulang lewat script. Pemisahan beberapa atlas mengikuti jarak nyata antarobjek agar tile/tray/panel tidak terpotong oleh asumsi grid yang meleset.

## Pratinjau

- [Tile, tray, slot, panel, ilustrasi dan dekorasi — terang](previews/ui-kit-light.jpg)
- [Aset yang sama — gelap](previews/ui-kit-dark.jpg)
- [Delapan latar](previews/ui-kit-backgrounds.jpg)
- [Urutan frame efek](previews/ui-kit-effects.jpg)

File berada di `assets/ui-kit/`. [manifest.json](../assets/ui-kit/manifest.json) memuat kategori, ID, ukuran, SHA-256, sumber, crop rectangle, area ikon yang disarankan dan pengaturan animasi. `source-atlases/` dan `source-backgrounds/` menyimpan sumber; ambil berkas dari folder kategori untuk rendering.

## Memakai aset

```js
import {uiAssetFor, UI_PALETTES, UI_ANIMATIONS} from '../src/index.js';

const body = uiAssetFor('square-ivory');
const domino = uiAssetFor('horizontal-mint');
const tray = uiAssetFor('trays-mint');
const slot = uiAssetFor('slots-mint');
const panel = uiAssetFor('panels-ivory');
const victory = uiAssetFor('results-victory');
const background = uiAssetFor('zen-garden-portrait');
const effect = UI_ANIMATIONS['match-burst'];
// effect.path: animated WebP; effect.frames: 8 PNG; effect.spriteStrip: satu strip.
```

Semua path relatif terhadap root repo. Tambahkan base URL atau gunakan mekanisme bundler proyek baru. Teks, angka, ikon tombol yang sudah ada, area sentuh, status selected/disabled dan layout tetap dirender host. Tile tidak punya ikon tertanam: tumpuk badan tile, ikon objek dari `tileAssetFor()`, lalu highlight/efek sebagai lapisan terpisah. Untuk kesan tumpukan Triple Match, render beberapa badan tile dengan offset kecil; tidak perlu gambar baru untuk tiap kedalaman.

Tray sengaja kosong tanpa jumlah slot tertanam. Host mengulang cekungan slot sesuai kapasitasnya (logic Triple tetap tujuh). Garis sambungan Onet sebaiknya digambar secara dinamis dari path hasil logic, sehingga mengikuti posisi tile dan ukuran layar.

`logicalUnits` adalah `[lebar, tinggi]`. Area ikon dan inset panel pada manifest adalah **saran** yang perlu disesuaikan di frontend; tidak menjamin nine-slice sempurna pada setiap ukuran. Gunakan scale-to-fit untuk panel dengan ornamen sudut. Latar memiliki ruang tengah yang tenang; saran area papan tidak menjamin semua crop portrait/landscape aman. Ukuran output bukan klaim bahwa detail sumber menjadi lebih tajam setelah resize.

## Animasi

Keempat animasi memakai delapan frame, 80 ms per frame (640 ms satu urutan). Frame dipotong dengan transform/canvas tetap, tanpa trim tiap frame, sehingga ukuran efek yang membesar/mengecil tetap terbaca. `hint-pulse` berulang; efek lainnya sekali putar. Animated WebP memerlukan dukungan decoder host; engine yang tidak mendukungnya dapat memakai PNG/strip dan `frameDurationMs` dari manifest. Jangan meregangkan frame animasi secara terpisah. Untuk reduced motion, host dapat menampilkan frame statis atau menonaktifkan efek.

## Pemeriksaan

Seluruh 122 file siap pakai dibuka/di-decode, ukuran dan alpha diperiksa, serta empat animated WebP diverifikasi berisi delapan frame. Aset statis ditinjau lewat contact sheet di latar terang/gelap; semua background dan urutan efek juga ditinjau. `npm run check:assets` memeriksa checksum, kelengkapan katalog dan path frame/strip. Ini pemeriksaan aset, bukan QA gameplay atau performa di perangkat.
