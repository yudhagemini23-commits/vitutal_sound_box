package com.app.virtualsoundbox.data.local

import androidx.room.*
import com.app.virtualsoundbox.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // Fungsi Insert Tunggal (Sudah ada)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    // TAMBAHKAN INI: Fungsi untuk memasukkan List sekaligus (Sync dari Server)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE isTrialLimited = 1 AND timestamp < :time")
    suspend fun deleteExpiredLimitedTransactions(time: Long)

    // Query filter range (Jika Mas sudah ada sebelumnya)
    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<Transaction>>
}