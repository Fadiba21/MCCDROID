# MCC Droid

Aplikasi Android untuk menjalankan **Minecraft Console Client (MCC)** secara native (.NET `linux-bionic-arm64`) dengan GUI: profil banyak akun, terminal, editor konfigurasi visual (slider, saklar, dropdown), otomasi, notifikasi (kick, putus, login, whisper, dll.), manajer file, dan skrip C#.

APK dibangun oleh GitHub Actions. Semua bisa dikerjakan dari HP.

## Cara build (dari HP)
1. Buat repo baru di GitHub (app GitHub / github.com mode desktop), lalu unggah seluruh isi folder ini (Add file → Upload files; pastikan folder `.github` ikut).
2. Buka tab **Actions** → workflow **Build APK** → **Run workflow** (opsional isi `mcc_ref`, default `master`).
3. Tunggu selesai (±15–30 menit pertama). APK ada di **Releases** (`MCC-Droid-v1.0.<nomor>.apk`) dan di *Artifacts*.
4. Pasang APK (izinkan sumber tidak dikenal). Build awal memakai kunci debug/otomatis; untuk kunci tetap isi secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (opsional).

Jika build gagal: buka run yang gagal → salin log langkah yang merah (atau unduh artifact `build-logs`) dan kirim ke saya.

## Pemakaian
- **Beranda**: buat profil (1 profil = 1 akun/server), Start/Stop/Restart. Runtime MCC diekstrak otomatis saat pertama dibuka.
- **Konfig**: isi akun & server, ketuk saklar bot, atur slider; mode `</>` untuk edit mentah. Simpan atau "Simpan & /reload".
- **Login Microsoft**: MCC menampilkan kode perangkat → kartu dengan tombol Salin kode / Buka tautan.
- **Terminal**: perintah MCC, saran `/perintah`, favorit, katalog; tab Shell untuk sh Android.
- **Otomasi**: aturan "jika teks/kejadian X → kirim/notif/restart" dengan uji regex.
- **Lainnya → File**: jelajah/edit/impor/ekspor, dan "Buka di aplikasi File" (folder MCC muncul di panel samping app File).

## Batasan jujur
- Kode Android **belum pernah dikompilasi** di lingkungan pembuatnya (tidak ada akses SDK/Maven). Kemungkinan ada error kompilasi kecil di build pertama; kirim lognya dan akan diperbaiki.
- Langkah pembuatan runtime (OpenSSL + publish .NET untuk bionic) bergantung pada ketersediaan paket di runner; bila gagal, APK tetap jadi tetapi runtime akan "Tidak ada" di Pengaturan.
- `targetSdk 28` dipakai agar binari bisa dieksekusi dari penyimpanan aplikasi (seperti Termux); tidak untuk Google Play.
- Android 12+ bisa mematikan proses anak (phantom process killer) — lihat tips di Pengaturan.
- Tidak ada PTY: terminal MCC memakai mode pipa polos (tanpa UI TUI).
- **Mod Fabric tidak bisa berjalan di dalam MCC** (MCC bukan Minecraft penuh). Aplikasi menyimpan .jar dan membuka launcher Pojav/Zalith/FCL bila terpasang; perilaku kustom lewat skrip C# MCC.
