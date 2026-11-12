# Batas ekstraksi

Sumber: `arisynn/connectify-new`, branch `main`, commit `57160aa2c78fc6ec2fbfbdfbcc362dd3e6837471`.

- `src/core/board.js` dipindah ke `src/onet/board.js`; hanya jalur import yang diadaptasi. Algoritma path, pembentukan papan, group/slave, pemecahan tile, panah, hint dan shuffle tetap berasal dari sumber.
- `src/core/difficulty.js` dan `src/core/random.js` disalin tanpa perubahan. Config yang dibawa hanya ROWS/COLS.
- `useOnetGame.ts` diekstrak menjadi transisi state murni di `src/onet/round.js`: pilihan tile, skor 100/150/200, combo 4 detik, tambahan skor combo, durasi animasi 480ms, pause, timeout dan hasil ronde. Auto timer, audio/haptics, React, profil, inventori berbayar, daily progression, API reward dan navigation dikeluarkan. Waktu match disediakan host agar bisa deterministik. Papan buntu diekspos sebagai `needsShuffle`.
- `TripleGame.tsx` dan `useMiniRound.ts` diekstrak menjadi `src/match3/index.js`. Generator tetap memakai enam ID, tiga layer × dua belas tile, tray tujuh slot, trio bernilai 150, waktu default 120 detik dan undo satu kali. State berbentuk data, bukan React hook. Host mengatur tick/pause. Undo tidak mengembalikan waktu dan tidak dapat dipakai setelah ronde berakhir, mengikuti sumber.
- `tileCatalog.js` dan registry tema dipangkas menjadi ID/path gambar; metadata harga/reward dan provider tema tidak disertakan. Fallback vanilla/sweets mempertahankan ID tile pantai.
- Gambar yang dibawa: semua tile tema yang dipakai Onet/Triple (tema dasar, premium, seasonal, reward dan pantai), permukaan tile, efek, serta empat ikon tools/game. Brand Connectify, avatar, card Memory, Fruit Merge, basket, game balok dan Infinity/Star Patrol dikeluarkan.
- File asli dicatat beserta SHA-256 di `provenance.json`. Sheet sumber yang cocok dengan tema ikut dibawa sebagai mentahan; atlas dasar asli juga disimpan. Tidak menyalin source history, `.env`, kredensial, backend, app screens atau konfigurasi deployment.
- 64 tile nontransparan pada empat tema dasar diedit melalui image tool menggunakan atlas 4×4; hasil atlas dipotong deterministik, urutan nomor sama. `transparency.json` membedakan turunan hasil edit dari file asli. Hasil edit bukan jaminan rekonstruksi piksel persis.

Repo baru tidak menyediakan UI/game lengkap. Logiknya dapat dihubungkan ke frontend baru secara bebas dari aplikasi lama. Tidak menambahkan model Match-3 swap/cascade karena sumber tidak memiliki game tersebut.
