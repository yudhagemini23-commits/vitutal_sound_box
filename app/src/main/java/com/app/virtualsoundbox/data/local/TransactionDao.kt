package com.app.virtualsoundbox.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.virtualsoundbox.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    // Query default (Semua data)
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // 1. Ambil transaksi berdasarkan rentang tanggal
    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    // 2. Hitung total uang berdasarkan rentang tanggal
    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp BETWEEN :startDate AND :endDate")
    fun getTotalAmountByDateRange(startDate: Long, endDate: Long): Flow<Double?>
}