package com.app.virtualsoundbox.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.app.virtualsoundbox.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<Transaction>>

    // --- FITUR BARU: HAPUS TRANSAKSI LIMIT YANG SUDAH BASI (>30 MENIT) ---
    // Menghapus jika isTrialLimited = 1 (True) DAN waktunya kurang dari (Sekarang - 30 menit)
    @Query("DELETE FROM transactions WHERE isTrialLimited = 1 AND timestamp < :thresholdTime")
    suspend fun deleteExpiredLimitedTransactions(thresholdTime: Long)
}