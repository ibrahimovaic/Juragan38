package com.juragan38.pos.data.repository

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.juragan38.pos.data.local.AppDatabase
import com.juragan38.pos.data.local.CartItem
import com.juragan38.pos.data.local.Product
import com.juragan38.pos.data.local.Transaction
import com.juragan38.pos.data.local.TransactionItem
import com.juragan38.pos.data.local.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room = cache lokal (offline-first), Firestore = sumber data bersama antar HP,
 * Firebase Auth = penyimpan kredensial login.
 */
@Singleton
class PosRepository @Inject constructor(
    private val database: AppDatabase,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {

    private val userDao = database.userDao()
    private val productDao = database.productDao()
    private val transactionDao = database.transactionDao()

    val products: Flow<List<Product>> = productDao.all()
    val transactions: Flow<List<Transaction>> = transactionDao.all()
    val users: Flow<List<User>> = userDao.allActive()

    private var productReg: ListenerRegistration? = null
    private var transactionReg: ListenerRegistration? = null
    private var userReg: ListenerRegistration? = null

    private val accounts = firestore.collection("users")
    private val productCol = firestore.collection("products")
    private val transactionCol = firestore.collection("transactions")

    // ---------------------------------------------------------------- auth

    fun hasSession(): Boolean = auth.currentUser != null

    fun currentUserId(): String? = auth.currentUser?.uid

    private fun emailOf(username: String): String =
        username.trim().lowercase() + "@juragan38.local"

    /**
     * Login lewat Firebase Auth (username dipetakan ke email internal),
     * lalu profil + role diambil dari Firestore.
     */
    suspend fun login(username: String, password: String): User {
        val email = emailOf(username)
        try {
            auth.signInWithEmailAndPassword(email, password).await()
        } catch (e: FirebaseAuthInvalidUserException) {
            throw IllegalStateException("Username atau password salah")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            throw IllegalStateException("Username atau password salah")
        } catch (e: FirebaseNetworkException) {
            throw IllegalStateException("Tidak ada koneksi internet. Login pertama butuh internet.")
        } catch (e: Exception) {
            throw IllegalStateException(e.friendlyMessage())
        }

        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Gagal masuk. Coba lagi.")

        val doc = try {
            accounts.document(uid).get().await()
        } catch (e: Exception) {
            throw IllegalStateException(e.friendlyMessage())
        }

        if (!doc.exists()) {
            auth.signOut()
            throw IllegalStateException("Akun ini belum terdaftar di server. Hubungi admin.")
        }
        if (doc.getBoolean("active") == false) {
            auth.signOut()
            throw IllegalStateException("Akun ini dinonaktifkan oleh admin")
        }

        val user = User(
            id = uid,
            username = doc.getString("username") ?: username.trim(),
            fullName = doc.getString("fullName") ?: username.trim(),
            role = doc.getString("role") ?: "KASIR",
            active = true
        )
        userDao.upsert(user)
        return user
    }

    suspend fun logout() {
        stopRealtimeSync()
        runCatching { auth.signOut() }
    }

    // ------------------------------------------------------------ realtime

    /** Mulai mendengarkan perubahan Firestore dan menuliskannya ke cache Room. */
    @Synchronized
    fun startRealtimeSync(scope: CoroutineScope, role: String) {
        if (auth.currentUser == null) return
        if (productReg != null) return

        productReg = productCol.addSnapshotListener { snapshot, _ ->
            if (snapshot == null) return@addSnapshotListener
            val docs = snapshot.documents
            val fromCache = snapshot.metadata.isFromCache
            val mapped = docs.mapNotNull { it.toProduct() }
            scope.launch {
                if (mapped.isNotEmpty()) productDao.upsertAll(mapped)
                if (!fromCache) {
                    val ids = docs.map { it.id }
                    if (ids.isEmpty()) productDao.clear() else productDao.deleteMissing(ids)
                }
            }
        }

        transactionReg = transactionCol
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(500)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val mapped = snapshot.documents.mapNotNull { it.toTransaction() }
                if (mapped.isNotEmpty()) {
                    scope.launch { transactionDao.upsertAll(mapped) }
                }
            }

        if (role == "ADMIN") {
            userReg = accounts.addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val docs = snapshot.documents
                val fromCache = snapshot.metadata.isFromCache
                val mapped = docs.mapNotNull { it.toUser() }
                scope.launch {
                    if (mapped.isNotEmpty()) userDao.upsertAll(mapped)
                    if (!fromCache) {
                        val ids = docs.map { it.id }
                        if (ids.isEmpty()) userDao.clear() else userDao.deleteMissing(ids)
                    }
                }
            }
        }
    }

    @Synchronized
    fun stopRealtimeSync() {
        productReg?.remove()
        transactionReg?.remove()
        userReg?.remove()
        productReg = null
        transactionReg = null
        userReg = null
    }

    // ------------------------------------------------------------- seeding

    /**
     * Dipanggil sekali setelah admin pertama berhasil login:
     * - isi produk contoh kalau koleksi produk masih kosong
     * - buat akun demo kasir & manajer kalau baru ada 1 akun (admin)
     */
    suspend fun prepareFirstAdminData() {
        runCatching {
            val products = productCol.limit(1).get().await()
            if (products.isEmpty) {
                val batch = firestore.batch()
                defaultProducts().forEach { product ->
                    val id = UUID.randomUUID().toString()
                    batch.set(productCol.document(id), product.toMap())
                    productDao.upsert(product.copy(id = id))
                }
                batch.commit()
            }

            val accountsSnap = accounts.limit(2).get().await()
            if (accountsSnap.size() <= 1) {
                listOf(
                    Triple("kasir", "Kasir 1", "kasir123") to "KASIR",
                    Triple("manajer", "Manajer", "manajer123") to "MANAJER"
                ).forEach { (data, role) ->
                    val (username, fullName, password) = data
                    runCatching { createAccount(username, fullName, password, role) }
                }
            }
        }
    }

    private fun defaultProducts(): List<Product> = listOf(
        Product(name = "Kopi Hitam", category = "Minuman", price = 8000.0, cost = 3000.0, stock = 50),
        Product(name = "Es Teh Manis", category = "Minuman", price = 5000.0, cost = 1500.0, stock = 80),
        Product(name = "Nasi Goreng", category = "Makanan", price = 20000.0, cost = 11000.0, stock = 30),
        Product(name = "Mie Ayam", category = "Makanan", price = 15000.0, cost = 8000.0, stock = 25),
        Product(name = "Roti Bakar", category = "Snack", price = 12000.0, cost = 6000.0, stock = 20),
        Product(name = "Air Mineral", category = "Minuman", price = 4000.0, cost = 2000.0, stock = 100)
    )

    // ------------------------------------------------------------ products

    suspend fun saveProduct(product: Product) {
        val id = product.id.ifBlank { UUID.randomUUID().toString() }
        val saved = product.copy(id = id)
        productDao.upsert(saved)
        // tanpa await: tetap jalan walau offline, Firestore mengantre otomatis
        productCol.document(id).set(saved.toMap())
    }

    suspend fun deleteProduct(product: Product) {
        productDao.delete(product)
        productCol.document(product.id).delete()
    }

    // --------------------------------------------------------------- users

    /**
     * @return pesan error, atau null bila berhasil.
     */
    suspend fun saveUser(user: User, password: String?): String? {
        val username = user.username.trim()
        val fullName = user.fullName.trim()

        if (fullName.isBlank()) return "Nama lengkap wajib diisi"
        if (username.isBlank()) return "Username wajib diisi"
        if (username.contains(' ')) return "Username tidak boleh mengandung spasi"

        if (user.id.isBlank()) {
            if (password.isNullOrBlank()) return "Password wajib diisi"
            if (password.length < 5) return "Password minimal 5 karakter"
            if (isUsernameTaken(username, "")) return "Username \"$username\" sudah dipakai"
            return createAccount(username, fullName, password, user.role)
        }

        // Akun lama: username tidak bisa diubah (terikat email login)
        return try {
            accounts.document(user.id).set(
                mapOf(
                    "username" to username,
                    "fullName" to fullName,
                    "role" to user.role,
                    "active" to user.active
                ),
                SetOptions.merge()
            ).await()
            userDao.upsert(user.copy(username = username, fullName = fullName))
            null
        } catch (e: Exception) {
            e.friendlyMessage()
        }
    }

    private suspend fun isUsernameTaken(username: String, exceptId: String): Boolean =
        runCatching {
            val snap = accounts.whereEqualTo("username", username).limit(2).get().await()
            snap.documents.any { it.id != exceptId }
        }.getOrDefault(false)

    private suspend fun createAccount(
        username: String,
        fullName: String,
        password: String,
        role: String
    ): String? {
        val helper = secondaryAuth()
        val created = try {
            helper.createUserWithEmailAndPassword(emailOf(username), password).await()
        } catch (e: FirebaseAuthUserCollisionException) {
            return "Username \"$username\" sudah dipakai"
        } catch (e: Exception) {
            return "Gagal membuat akun: ${e.friendlyMessage()}"
        } finally {
            runCatching { helper.signOut() }
        }

        val uid = created.user?.uid ?: return "Gagal membuat akun"
        val user = User(
            id = uid,
            username = username,
            fullName = fullName,
            role = role,
            active = true
        )
        return try {
            accounts.document(uid).set(user.toMap()).await()
            userDao.upsert(user)
            null
        } catch (e: Exception) {
            "Akun login dibuat, tapi profil gagal disimpan: ${e.friendlyMessage()}"
        }
    }

    /**
     * @return pesan error, atau null bila berhasil.
     */
    suspend fun deleteUser(user: User): String? {
        if (user.id == currentUserId()) return "Tidak bisa menghapus akun sendiri"
        if (user.role == "ADMIN" && activeAdminCount() <= 1) {
            return "Minimal harus ada 1 akun admin aktif"
        }
        return try {
            accounts.document(user.id).update("active", false).await()
            userDao.upsert(user.copy(active = false))
            null
        } catch (e: Exception) {
            e.friendlyMessage()
        }
    }

    private suspend fun activeAdminCount(): Int = runCatching {
        accounts.whereEqualTo("role", "ADMIN").get().await()
            .documents
            .count { it.getBoolean("active") != false }
    }.getOrDefault(0)

    private suspend fun secondaryAuth(): FirebaseAuth {
        val existing = FirebaseApp.getApps(context).firstOrNull { it.name == HELPER_APP }
        val app = existing ?: FirebaseApp.initializeApp(
            context,
            FirebaseApp.getInstance().options,
            HELPER_APP
        ) ?: throw IllegalStateException("Gagal menyiapkan layanan akun")
        return FirebaseAuth.getInstance(app)
    }

    // ---------------------------------------------------------- transaksi

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
        val id = UUID.randomUUID().toString()

        val transaction = Transaction(
            id = id,
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

        // 1) simpan lokal dulu (selalu berhasil walau offline)
        transactionDao.insert(transaction)
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

        // 2) kirim ke server (batch atomic, diantre kalau offline)
        val batch = firestore.batch()
        batch.set(
            transactionCol.document(id),
            mapOf(
                "invoiceNo" to transaction.invoiceNo,
                "timestamp" to transaction.timestamp,
                "cashier" to transaction.cashier,
                "cashierUid" to (currentUserId() ?: ""),
                "total" to transaction.total,
                "costTotal" to transaction.costTotal,
                "profit" to transaction.profit,
                "paymentMethod" to transaction.paymentMethod,
                "paidAmount" to transaction.paidAmount,
                "changeAmount" to transaction.changeAmount,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        items.forEach { item ->
            val itemRef = transactionCol.document(id).collection("items").document()
            batch.set(
                itemRef,
                mapOf(
                    "productId" to item.productId,
                    "productName" to item.name,
                    "price" to item.price,
                    "cost" to item.cost,
                    "qty" to item.qty,
                    "subtotal" to item.subtotal
                )
            )
            batch.update(
                productCol.document(item.productId),
                "stock",
                FieldValue.increment(-item.qty.toLong())
            )
        }
        runCatching { batch.commit() }

        return transaction
    }

    suspend fun itemsOf(transactionId: String): List<TransactionItem> =
        transactionDao.itemsOf(transactionId)

    // ------------------------------------------------------------ mapping

    private fun Product.toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "category" to category,
        "price" to price,
        "cost" to cost,
        "stock" to stock.toLong(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun User.toMap(): Map<String, Any> = mapOf(
        "username" to username,
        "fullName" to fullName,
        "role" to role,
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun DocumentSnapshot.toProduct(): Product? {
        return try {
            Product(
                id = id,
                name = getString("name") ?: return null,
                category = getString("category") ?: "Umum",
                price = getDouble("price") ?: 0.0,
                cost = getDouble("cost") ?: 0.0,
                stock = (getLong("stock") ?: 0L).toInt()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.toTransaction(): Transaction? {
        return try {
            val timestamp = getLong("timestamp") ?: return null
            Transaction(
                id = id,
                invoiceNo = getString("invoiceNo") ?: "INV-$timestamp",
                timestamp = timestamp,
                cashier = getString("cashier") ?: "-",
                total = getDouble("total") ?: 0.0,
                costTotal = getDouble("costTotal") ?: 0.0,
                profit = getDouble("profit") ?: 0.0,
                paymentMethod = getString("paymentMethod") ?: "TUNAI",
                paidAmount = getDouble("paidAmount") ?: 0.0,
                changeAmount = getDouble("changeAmount") ?: 0.0
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.toUser(): User? {
        return try {
            val username = getString("username") ?: return null
            User(
                id = id,
                username = username,
                fullName = getString("fullName") ?: username,
                role = getString("role") ?: "KASIR",
                active = getBoolean("active") != false
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun Exception.friendlyMessage(): String = when (this) {
        is FirebaseNetworkException -> "Tidak ada koneksi internet"
        is FirebaseAuthUserCollisionException -> "Username sudah dipakai"
        else -> message ?: "Terjadi kesalahan, coba lagi"
    }

    private companion object {
        const val HELPER_APP = "juragan38-helper"
    }
}
