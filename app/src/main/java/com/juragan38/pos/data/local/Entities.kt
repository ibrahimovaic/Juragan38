package com.juragan38.pos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * id = UID Firebase Auth sekaligus document id di Firestore.
 * Password TIDAK disimpan di sini (dikelola Firebase Auth).
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = "",
    val username: String,
    val fullName: String,
    val role: String,
    val active: Boolean = true
)

/** Outlet / cabang. Bisa ditambah dari menu Pengaturan oleh admin. */
@Entity(tableName = "outlets")
data class Outlet(
    @PrimaryKey val id: String = "",
    val name: String,
    val address: String = "",
    val active: Boolean = true
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String = "",
    val name: String,
    val category: String = "Umum",
    val price: Double = 0.0,
    val cost: Double = 0.0,
    val stock: Int = 0,
    /**
     * Gambar produk. Isinya bisa:
     * - URL `https://...`
     * - data URI base64 `data:image/jpeg;base64,...` (hasil pilih dari galeri)
     * - kosong = tanpa gambar
     */
    val image: String = "",
    /**
     * Outlet pemilik produk. Kosong = produk tampil di semua outlet
     * (produk lama sebelum fitur ini ada).
     */
    val outletId: String = ""
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String = "",
    val invoiceNo: String,
    val timestamp: Long,
    val cashier: String,
    val total: Double,
    val costTotal: Double,
    val profit: Double,
    val paymentMethod: String,
    val paidAmount: Double,
    val changeAmount: Double,
    val outlet: String = "",
    val shift: String = ""
)

@Entity(tableName = "transaction_items")
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: String,
    val productId: String,
    val productName: String,
    val price: Double,
    val cost: Double,
    val qty: Int,
    val subtotal: Double
)

/**
 * Satu shift kerja. [id] dipakai sebagai `shiftKey` di sesi login dan disimpan
 * di transaksi, jadi id tidak boleh berubah walau nama/jamnya diedit.
 */
data class Shift(
    val id: String,
    val name: String,
    val start: String = "08:00",
    val end: String = "16:00"
) {
    /** Contoh: "Shift 1 (08:00-16:00)". */
    fun label(): String = "$name ($start-$end)"
}

/** Encode/decode daftar shift ke JSON agar cukup satu kolom di Room & Firestore. */
object ShiftCodec {

    fun encode(shifts: List<Shift>): String {
        val array = org.json.JSONArray()
        shifts.forEach { shift ->
            array.put(
                org.json.JSONObject().apply {
                    put("id", shift.id)
                    put("name", shift.name)
                    put("start", shift.start)
                    put("end", shift.end)
                }
            )
        }
        return array.toString()
    }

    fun decode(json: String): List<Shift> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = org.json.JSONArray(json)
            (0 until array.length()).mapNotNull { index ->
                val obj = array.optJSONObject(index) ?: return@mapNotNull null
                val name = obj.optString("name").trim()
                if (name.isEmpty()) return@mapNotNull null
                Shift(
                    id = obj.optString("id").ifBlank { "SHIFT${index + 1}" },
                    name = name,
                    start = obj.optString("start").ifBlank { "08:00" },
                    end = obj.optString("end").ifBlank { "16:00" }
                )
            }
        }.getOrDefault(emptyList())
    }
}

/**
 * Pengaturan toko yang dipakai bersama semua HP (disimpan di Firestore
 * `settings/app` dan di-cache di Room).
 */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val storeName: String = "JURAGAN 38",
    val shift1Name: String = "Shift 1",
    val shift1Start: String = "08:00",
    val shift1End: String = "16:00",
    val shift2Name: String = "Shift 2",
    val shift2Start: String = "16:00",
    val shift2End: String = "23:59",
    val watermarkEnabled: Boolean = true,
    val watermarkText: String = "JURAGAN 38",
    val watermarkOpacity: Float = 0.06f,
    /** Baris penutup struk. Header struk otomatis memakai nama outlet. */
    val receiptFooter: String = "Terima kasih sudah berbelanja",
    /** Daftar shift bebas (JSON). Kosong = pakai shift 1 & 2 lama di atas. */
    val shiftsJson: String = ""
) {
    /** Daftar shift aktif. Admin bisa menambah/menghapus kapan saja. */
    fun shifts(): List<Shift> {
        val custom = ShiftCodec.decode(shiftsJson)
        if (custom.isNotEmpty()) return custom
        return listOf(
            Shift(SHIFT_1, shift1Name, shift1Start, shift1End),
            Shift(SHIFT_2, shift2Name, shift2Start, shift2End)
        )
    }

    /** Simpan daftar shift baru sekaligus menjaga field lama tetap sinkron. */
    fun withShifts(list: List<Shift>): AppSettings {
        val safe = list.ifEmpty { shifts() }
        return copy(
            shiftsJson = ShiftCodec.encode(safe),
            shift1Name = safe.getOrNull(0)?.name ?: shift1Name,
            shift1Start = safe.getOrNull(0)?.start ?: shift1Start,
            shift1End = safe.getOrNull(0)?.end ?: shift1End,
            shift2Name = safe.getOrNull(1)?.name ?: safe.getOrNull(0)?.name ?: shift2Name,
            shift2Start = safe.getOrNull(1)?.start ?: safe.getOrNull(0)?.start ?: shift2Start,
            shift2End = safe.getOrNull(1)?.end ?: safe.getOrNull(0)?.end ?: shift2End
        )
    }

    /** Label panjang untuk ditampilkan, mis. "Shift 1 (08:00-16:00)". */
    fun shiftLabel(key: String): String =
        shifts().firstOrNull { it.id == key }?.label() ?: "-"

    fun shiftShortName(key: String): String =
        shifts().firstOrNull { it.id == key }?.name ?: "-"

    companion object {
        const val SHIFT_1 = "SHIFT1"
        const val SHIFT_2 = "SHIFT2"
    }
}

data class CartItem(
    val productId: String,
    val name: String,
    val price: Double,
    val cost: Double,
    val qty: Int,
    val image: String = ""
) {
    val subtotal: Double get() = price * qty
}
