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
    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    suspend fun login(username: String, password: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<User>)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun all(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun byId(id: Long): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("UPDATE products SET stock = :stock WHERE id = :id")
    suspend fun updateStock(id: Long, stock: Int)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun all(): Flow<List<Transaction>>

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Insert
    suspend fun insertItems(items: List<TransactionItem>)

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun itemsOf(transactionId: Long): List<TransactionItem>
}
