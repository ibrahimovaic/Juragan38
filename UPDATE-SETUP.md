# Pembaruan Aplikasi (Auto Update)

Aplikasi JURAGAN 38 bisa mengecek versi terbaru sendiri, mengunduh APK, lalu
membuka layar instalasi Android. Tidak perlu lagi menyalin APK manual ke tiap HP
(selama HP sudah memakai versi 1.3.0 atau lebih baru).

## Bagaimana cara kerjanya

1. Saat aplikasi dibuka, aplikasi membaca file `update.json` dari repositori
   GitHub (maksimal sekali per 6 jam). Bisa juga dicek manual lewat tombol
   **Cek Pembaruan** di bagian paling bawah menu utama.
2. Kalau `versionCode` di `update.json` lebih besar dari versi terpasang,
   muncul dialog **Pembaruan tersedia**.
3. Tekan **Unduh & Pasang** → APK diunduh ke cache aplikasi → layar instalasi
   Android terbuka → tekan **Instal**.
4. Jika Android meminta izin "Instal aplikasi tidak dikenal", izinkan untuk
   JURAGAN 38 lalu tekan **Pasang Pembaruan** lagi.

Catatan penting:

- Android **selalu** menampilkan konfirmasi instalasi untuk aplikasi di luar
  Play Store. Tidak ada pemasangan 100% diam-diam tanpa akses sistem/Play Store.
- APK baru harus ditandatangani dengan **kunci yang sama** dengan yang
  terpasang (di mesin build ini memakai debug keystore yang sama).
- HP yang masih memakai versi **1.2.0 atau lebih lama** belum punya fitur ini,
  jadi sekali saja harus dipasang manual.

## Cara merilis versi baru

1. Ubah versi di `app/build.gradle.kts`:

   ```kotlin
   versionCode = 7          // harus naik terus
   versionName = "1.3.1"
   ```

2. Build APK:

   ```powershell
   $env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
   $env:Path="$env:JAVA_HOME\bin;$env:Path"
   .\gradlew assembleDebug --console=plain
   ```

3. Salin APK ke folder rilis:

   ```powershell
   Copy-Item app\build\outputs\apk\debug\app-debug.apk apk\JURAGAN-38.apk -Force
   ```

4. Perbarui `update.json` (di root proyek):

   ```json
   {
     "versionCode": 7,
     "versionName": "1.3.1",
     "apkUrl": "https://raw.githubusercontent.com/ibrahimovaic/Juragan38/main/apk/JURAGAN-38.apk",
     "apkUrlFallback": "https://github.com/ibrahimovaic/Juragan38/raw/main/apk/JURAGAN-38.apk",
     "notes": "Perbaikan ...",
     "force": false,
     "publishedAt": "2026-11-01"
   }
   ```

5. Commit dan push:

   ```powershell
   git add update.json apk/JURAGAN-38.apk app/build.gradle.kts
   git commit -m "release: v1.3.1"
   git push origin main
   ```

Semua HP yang online akan melihat notifikasi pembaruan pada pembukaan
aplikasi berikutnya (atau saat menekan Cek Pembaruan).

## Kalau GitHub tidak bisa diakses dari HP

`raw.githubusercontent.com` kadang diblokir oleh operator tertentu. Solusi:

1. Unggah APK ke Firebase Storage (console Firebase → Storage) atau Google
   Drive (link unduhan langsung).
2. Ganti `apkUrl` di `update.json` dengan link tersebut, commit dan push.

Aplikasi juga mencoba `apkUrlFallback` jika `apkUrl` gagal diunduh.

## Komponen teknis

- `utils/UpdateChecker.kt` – baca manifest, unduh APK, pasang lewat FileProvider.
- `AppViewModel` – state `UpdateState` (cek, progress unduh, error).
- `DashboardScreen` – kartu info versi di dasar menu + dialog pembaruan.
- `AndroidManifest.xml` – izin `REQUEST_INSTALL_PACKAGES` + FileProvider
  (`res/xml/file_paths.xml`).
