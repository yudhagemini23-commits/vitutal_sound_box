package com.app.virtualsoundbox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.virtualsoundbox.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    // Menggunakan Flow agar UI otomatis update saat ada data baru
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp >= :startOfDay")
    fun getTotalToday(startOfDay: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)
}