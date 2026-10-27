package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StoreSettingsEntity::class,
        ProductEntity::class,
        ProductCostEntity::class,
        CustomerOrderEntity::class,
        OfflineSaleEntity::class,
        ExpenseEntity::class,
        InventoryMovementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ShivamMartDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao

    companion object {
        @Volatile
        private var INSTANCE: ShivamMartDatabase? = null

        fun getInstance(context: Context): ShivamMartDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShivamMartDatabase::class.java,
                    "shivam_riya_mart.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun createInMemory(context: Context): ShivamMartDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                ShivamMartDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
