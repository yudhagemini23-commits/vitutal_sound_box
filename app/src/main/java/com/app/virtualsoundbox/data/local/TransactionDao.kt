package com.app.virtualsoundbox.data.local

import androidx.room.*
import com.app.virtualsoundbox.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE isTrialLimited = 1 AND timestamp < :time")
    suspend fun deleteExpiredLimitedTransactions(time: Long)

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<Transaction>>

    // --- TAMBAHAN BARU ---

    /**
     * Menghitung total income yang hanya isTrialLimited = 0 (Tidak Terkunci)
     */
    @Query("SELECT SUM(amount) FROM transactions WHERE isTrialLimited = 0 AND timestamp BETWEEN :start AND :end")
    fun getTotalIncomeByRange(start: Long, end: Long): Flow<Double?>

    /**
     * Membuka semua gembok transaksi (Panggil setelah sukses upgrade premium)
     */
    @Query("UPDATE transactions SET isTrialLimited = 0")
    suspend fun unlockAllTransactions()

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}