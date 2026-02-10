package com.app.virtualsoundbox.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.app.virtualsoundbox.model.Transaction
import com.app.virtualsoundbox.model.UserProfile

// PERBAIKAN 1: Naikkan version dari 1 ke 2
@Database(entities = [Transaction::class, UserProfile::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "soundhoree_db")
                    // PERBAIKAN 2: Tambahkan ini agar tidak crash saat struktur tabel berubah
                    // Ini akan menghapus data lama dan membuat database baru yang bersih
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}