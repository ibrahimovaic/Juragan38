package com.juragan38.pos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val password: String,
    val fullName: String,
    val role: String
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Umum",
    val price: Double = 0.0,
    val cost: Double = 0.0,
    val stock: Int = 0
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val transactionId: Long,
    val productId: Long,
    val productName: String,
    val price: Double,
    val cost: Double,
    val qty: Int,
    val subtotal: Double
)

data class CartItem(
    val productId: Long,
    val name: String,
    val price: Double,
    val cost: Double,
    val qty: Int
) {
    val subtotal: Double get() = price * qty
}
