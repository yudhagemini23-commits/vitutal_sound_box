package com.app.virtualsoundbox.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.app.virtualsoundbox.model.Transaction
import com.app.virtualsoundbox.model.UserProfile // Import tabel baru

@Database(
    entities = [Transaction::class, UserProfile::class], // Tambahkan UserProfile di sini
    version = 2, // Naikkan versinya
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun userProfileDao(): UserProfileDao // Tambahkan akses DAO baru

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "soundhoree_database"
                )
                    .fallbackToDestructiveMigration() // Hapus data lama jika versi berubah (Dev only)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}