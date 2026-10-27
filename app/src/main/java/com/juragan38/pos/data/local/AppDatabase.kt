package com.juragan38.pos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [User::class, Product::class, Transaction::class, TransactionItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
}
