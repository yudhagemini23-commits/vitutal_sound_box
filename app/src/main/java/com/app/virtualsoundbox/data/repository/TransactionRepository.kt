package com.app.virtualsoundbox.data.repository

import android.util.Log
import com.app.virtualsoundbox.data.local.TransactionDao
import com.app.virtualsoundbox.data.remote.RetrofitClient
import com.app.virtualsoundbox.data.remote.model.TransactionDto
import com.app.virtualsoundbox.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(start, end)
    }

    // --- FUNGSI BARU ---

    fun getTotalIncomeByRange(start: Long, end: Long): Flow<Double?> {
        return transactionDao.getTotalIncomeByRange(start, end)
    }

    suspend fun unlockAll() {
        transactionDao.unlockAllTransactions()
    }

    // --- KODE LAMA TETAP AMAN ---

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteExpiredLimited(time: Long) {
        transactionDao.deleteExpiredLimitedTransactions(time)
    }

    suspend fun insert(transaction: Transaction, userToken: String? = null, userId: String? = null) {
        transactionDao.insert(transaction)
        if (userToken != null && userId != null) {
            syncToBackend(transaction, userToken, userId)
        }
    }

    private suspend fun syncToBackend(trx: Transaction, token: String, userId: String) {
        withContext(Dispatchers.IO) {
            try {
                val dto = TransactionDto(
                    userId = userId,
                    sourceApp = trx.sourceApp,
                    amount = trx.amount,
                    rawMessage = trx.rawMessage,
                    timestamp = trx.timestamp,
                    isTrialLimited = trx.isTrialLimited
                )
                val response = RetrofitClient.instance.syncTransactions(
                    token = "Bearer $token",
                    transactions = listOf(dto)
                )
                if (response.isSuccessful) {
                    Log.d("SoundHoreeSync", "✅ Sync Berhasil")
                }
            } catch (e: Exception) {
                Log.e("SoundHoreeSync", "🔥 Error Network: ${e.message}")
            }
        }
    }
}