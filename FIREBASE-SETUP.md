# Setup Firebase — JURAGAN 38

Status saat ini (dicek otomatis dari `app/google-services.json`):

| Komponen | Status |
|---|---|
| Project ID | `juragan38` |
| Package | `com.juragan38.pos` ✔ cocok |
| Firestore Database | sudah dibuat ✔ |
| Authentication Email/Password | sudah aktif ✔ |
| `google-services.json` | sudah ada di `app/` ✔ |

---

## 1. Aktifkan login Email/Password

1. Buka https://console.firebase.google.com/project/juragan38/authentication
2. Klik **Get started**
3. Tab **Sign-in method** → **Email/Password** → aktifkan toggle pertama → **Save**

> Jangan aktifkan "Email link (passwordless)".

## 2. Pasang aturan akses Firestore

1. Buka https://console.firebase.google.com/project/juragan38/firestore/rules
2. Hapus isi editor, tempel isi file **`firestore.rules`** di root project ini
3. **Publish**

> Versi ini menambah koleksi `outlets` dan `settings` serta izin hapus riwayat
> untuk admin. Kalau sebelumnya sudah pernah publish, **publish ulang** aturan
> terbaru agar fitur outlet/pengaturan/hapus riwayat bisa dipakai di semua HP.

## 3. Buat akun admin pertama (sekali saja)

Akun pertama tidak bisa dibuat dari aplikasi (memang disengaja, supaya tidak ada
orang luar yang bisa mendaftar sendiri sebagai admin).

**3a.** Buat kredensial login:
- Buka https://console.firebase.google.com/project/juragan38/authentication/users
- **Add user**
- Email: `admin@juragan38.local`
- Password: `admin123` (ganti setelah bisa login)
- **Add user**
- Catat **User UID** yang muncul (contoh: `aBcD1234...`)

**3b.** Buat profilnya di Firestore:
- Buka https://console.firebase.google.com/project/juragan38/firestore/data
- **Start collection** → Collection ID: `users` → Next
- **Document ID**: tempel UID dari langkah 3a → Save
- Tambahkan field:

| Field | Type | Value |
|---|---|---|
| `username` | string | `admin` |
| `fullName` | string | `Administrator` |
| `role` | string | `ADMIN` |
| `active` | boolean | `true` |

## 4. Jalankan aplikasi

- Login `admin` / `admin123`
- Pilih **outlet** dan **shift** (Shift 1 / Shift 2) pada layar "Mulai Shift".
- Saat login pertama, aplikasi otomatis:
  - mengisi 6 produk contoh (kalau koleksi `products` masih kosong)
  - membuat outlet `Outlet Utama` dan pengaturan shift default
  - membuat akun `kasir / kasir123` dan `manajer / manajer123`
- Setelah itu semua akun baru dibuat dari menu **Manajemen Akun** dan langsung
  bisa dipakai login di HP lain.
- Menu **Pengaturan** (admin) dipakai untuk menambah outlet, mengatur nama/jam
  shift, mengubah watermark & nama toko, serta menghapus riwayat penjualan.

---

## Struktur data

```
users/{uid}
  username    : string   (unik, tidak bisa diubah)
  fullName    : string
  role        : "ADMIN" | "MANAJER" | "KASIR"
  active      : boolean
  createdAt   : timestamp

outlets/{outletId}
  name, address, active, createdAt

settings/app
  storeName, shift1Name, shift1Start, shift1End,
  shift2Name, shift2Start, shift2End,
  watermarkEnabled, watermarkText, watermarkOpacity, updatedAt

products/{productId}
  name, category, price, cost, stock, image, updatedAt
  image = URL https atau data URI base64 (gambar galeri, sudah diperkecil)

transactions/{transactionId}
  invoiceNo, timestamp, cashier, cashierUid, total, costTotal, profit,
  paymentMethod, paidAmount, changeAmount, outlet, shift, createdAt
  items/{itemId}: productId, productName, price, cost, qty, subtotal
```

Password **tidak pernah** disimpan di Firestore — dikelola Firebase Authentication.

## Batasan yang perlu diketahui

- **Login pertama wajib online.** Setelah itu aplikasi bisa dibuka & transaksi
  tetap jalan tanpa internet (data masuk antrean, terkirim otomatis saat online).
- **Username tidak bisa diubah** karena dipakai sebagai identitas login
  (`username@juragan38.local`).
- **Password tidak bisa diubah oleh admin** untuk akun orang lain (batasan
  Firebase client). Kalau karyawan lupa password: nonaktifkan akun lama, buat
  akun baru dengan username berbeda.
- Akun dinonaktifkan (bukan dihapus) agar riwayat transaksinya tetap utuh.
