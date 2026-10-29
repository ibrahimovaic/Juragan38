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
    val image: String = ""
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
    val watermarkOpacity: Float = 0.06f
) {
    /** Label panjang untuk ditampilkan, mis. "Shift 1 (08:00-16:00)". */
    fun shiftLabel(key: String): String = when (key) {
        SHIFT_1 -> "$shift1Name ($shift1Start-$shift1End)"
        SHIFT_2 -> "$shift2Name ($shift2Start-$shift2End)"
        else -> "-"
    }

    fun shiftShortName(key: String): String = when (key) {
        SHIFT_1 -> shift1Name
        SHIFT_2 -> shift2Name
        else -> "-"
    }

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
