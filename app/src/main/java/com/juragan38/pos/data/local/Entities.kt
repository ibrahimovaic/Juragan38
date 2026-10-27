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

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String = "",
    val name: String,
    val category: String = "Umum",
    val price: Double = 0.0,
    val cost: Double = 0.0,
    val stock: Int = 0
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
    val changeAmount: Double
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

data class CartItem(
    val productId: String,
    val name: String,
    val price: Double,
    val cost: Double,
    val qty: Int
) {
    val subtotal: Double get() = price * qty
}
