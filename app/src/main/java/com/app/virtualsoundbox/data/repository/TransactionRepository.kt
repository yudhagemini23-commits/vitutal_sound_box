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

    // --- KODE LAMA MAS (TETAP AMAN) ---

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(start, end)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteExpiredLimited(time: Long) {
        transactionDao.deleteExpiredLimitedTransactions(time)
    }

    // --- FUNGSI INSERT YANG DI-UPGRADE ---

    /**
     * Insert data ke database HP.
     * Jika ada internet + token + userId, otomatis coba kirim ke server Golang.
     */
    suspend fun insert(transaction: Transaction, userToken: String? = null, userId: String? = null) {
        // 1. Simpan ke Local Room dulu (Wajib sukses biar data aman di HP)
        transactionDao.insert(transaction)

        // 2. Coba Sync ke Backend (Fire & Forget)
        if (userToken != null && userId != null) {
            syncToBackend(transaction, userToken, userId)
        }
    }

    // Fungsi Private: Logika kirim ke Retrofit
    private suspend fun syncToBackend(trx: Transaction, token: String, userId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Convert Model Room -> Model Network (DTO)
                val dto = TransactionDto(
                    userId = userId,
                    sourceApp = trx.sourceApp,
                    amount = trx.amount,
                    rawMessage = trx.rawMessage,
                    timestamp = trx.timestamp,
                    isTrialLimited = trx.isTrialLimited
                )

                // Panggil API (Kirim sebagai List)
                val response = RetrofitClient.instance.syncTransactions(
                    token = "Bearer $token",
                    transactions = listOf(dto)
                )

                if (response.isSuccessful) {
                    Log.d("SoundHoreeSync", "✅ Sync Berhasil: Rp ${trx.amount}")
                } else {
                    Log.e("SoundHoreeSync", "❌ Gagal Sync: Code ${response.code()}")
                }
            } catch (e: Exception) {
                // Error koneksi jangan sampai bikin app crash
                Log.e("SoundHoreeSync", "🔥 Offline / Error Network: ${e.message}")
            }
        }
    }
}