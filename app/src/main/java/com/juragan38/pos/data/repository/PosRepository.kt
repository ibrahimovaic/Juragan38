package com.juragan38.pos.data.repository

import com.juragan38.pos.data.local.AppDatabase
import com.juragan38.pos.data.local.CartItem
import com.juragan38.pos.data.local.Product
import com.juragan38.pos.data.local.Transaction
import com.juragan38.pos.data.local.TransactionItem
import com.juragan38.pos.data.local.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PosRepository @Inject constructor(private val database: AppDatabase) {

    private val userDao = database.userDao()
    private val productDao = database.productDao()
    private val transactionDao = database.transactionDao()

    val products: Flow<List<Product>> = productDao.all()
    val transactions: Flow<List<Transaction>> = transactionDao.all()
    val users: Flow<List<User>> = userDao.all()

    suspend fun seedIfNeeded() {
        if (userDao.count() == 0) {
            userDao.insertAll(
                listOf(
                    User(username = "admin", password = "admin123", fullName = "Administrator", role = "ADMIN"),
                    User(username = "kasir", password = "kasir123", fullName = "Kasir 1", role = "KASIR"),
                    User(username = "manajer", password = "manajer123", fullName = "Manajer", role = "MANAJER")
                )
            )
        }
        if (productDao.count() == 0) {
            productDao.insertAll(
                listOf(
                    Product(name = "Kopi Hitam", category = "Minuman", price = 8000.0, cost = 3000.0, stock = 50),
                    Product(name = "Es Teh Manis", category = "Minuman", price = 5000.0, cost = 1500.0, stock = 80),
                    Product(name = "Nasi Goreng", category = "Makanan", price = 20000.0, cost = 11000.0, stock = 30),
                    Product(name = "Mie Ayam", category = "Makanan", price = 15000.0, cost = 8000.0, stock = 25),
                    Product(name = "Roti Bakar", category = "Snack", price = 12000.0, cost = 6000.0, stock = 20),
                    Product(name = "Air Mineral", category = "Minuman", price = 4000.0, cost = 2000.0, stock = 100)
                )
            )
        }
    }

    suspend fun login(username: String, password: String): User? = userDao.login(username, password)

    /**
     * Menyimpan akun baru atau mengubah akun yang sudah ada.
     * @return pesan error, atau null bila berhasil.
     */
    suspend fun saveUser(user: User): String? {
        val username = user.username.trim()
        if (user.fullName.isBlank()) return "Nama lengkap wajib diisi"
        if (username.isBlank()) return "Username wajib diisi"
        if (username.contains(' ')) return "Username tidak boleh mengandung spasi"
        if (user.password.isBlank()) return "Password wajib diisi"
        if (user.password.length < 5) return "Password minimal 5 karakter"
        if (user.role !in listOf("ADMIN", "KASIR", "MANAJER")) return "Role tidak valid"

        val existing = userDao.byUsername(username)
        if (existing != null && existing.id != user.id) return "Username \"$username\" sudah dipakai"

        if (user.id != 0L) {
            val old = userDao.byId(user.id)
            if (old != null && old.role == "ADMIN" && user.role != "ADMIN" && userDao.adminCount() <= 1) {
                return "Minimal harus ada 1 akun admin"
            }
        }

        userDao.upsert(user.copy(username = username, fullName = user.fullName.trim()))
        return null
    }

    /**
     * @return pesan error, atau null bila berhasil.
     */
    suspend fun deleteUser(user: User): String? {
        if (user.role == "ADMIN" && userDao.adminCount() <= 1) {
            return "Minimal harus ada 1 akun admin"
        }
        userDao.deleteById(user.id)
        return null
    }

    suspend fun saveProduct(product: Product) {
        productDao.upsert(product)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.delete(product)
    }

    suspend fun checkout(
        items: List<CartItem>,
        cashier: String,
        paymentMethod: String,
        paidAmount: Double
    ): Transaction {
        val total = items.sumOf { it.subtotal }
        val costTotal = items.sumOf { it.cost * it.qty }
        val profit = total - costTotal
        val now = System.currentTimeMillis()

        val transaction = Transaction(
            invoiceNo = "INV-" + now,
            timestamp = now,
            cashier = cashier,
            total = total,
            costTotal = costTotal,
            profit = profit,
            paymentMethod = paymentMethod,
            paidAmount = paidAmount,
            changeAmount = if (paidAmount > total) paidAmount - total else 0.0
        )

        val id = transactionDao.insert(transaction)

        transactionDao.insertItems(
            items.map {
                TransactionItem(
                    transactionId = id,
                    productId = it.productId,
                    productName = it.name,
                    price = it.price,
                    cost = it.cost,
                    qty = it.qty,
                    subtotal = it.subtotal
                )
            }
        )

        items.forEach { item ->
            val current = productDao.byId(item.productId)
            if (current != null) {
                productDao.updateStock(item.productId, (current.stock - item.qty).coerceAtLeast(0))
            }
        }

        return transaction.copy(id = id)
    }

    suspend fun itemsOf(transactionId: Long): List<TransactionItem> =
        transactionDao.itemsOf(transactionId)
}
