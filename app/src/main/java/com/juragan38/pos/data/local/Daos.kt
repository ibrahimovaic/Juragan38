package com.juragan38.pos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY role ASC, fullName ASC")
    fun all(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE active = 1 ORDER BY role ASC, fullName ASC")
    fun allActive(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun byUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(users: List<User>)

    @Query("DELETE FROM users WHERE id NOT IN (:ids)")
    suspend fun deleteMissing(ids: List<String>)

    @Query("DELETE FROM users")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND active = 1")
    suspend fun adminCount(): Int
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun all(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun byId(id: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("UPDATE products SET stock = :stock WHERE id = :id")
    suspend fun updateStock(id: String, stock: Int)

    @Query("DELETE FROM products WHERE id NOT IN (:ids)")
    suspend fun deleteMissing(ids: List<String>)

    @Query("DELETE FROM products")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun all(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(transactions: List<Transaction>)

    @Insert
    suspend fun insertItems(items: List<TransactionItem>)

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun itemsOf(transactionId: String): List<TransactionItem>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}
