package com.juragan38.pos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        User::class,
        Outlet::class,
        Product::class,
        Transaction::class,
        TransactionItem::class,
        AppSettings::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun outletDao(): OutletDao
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun settingsDao(): SettingsDao
}
