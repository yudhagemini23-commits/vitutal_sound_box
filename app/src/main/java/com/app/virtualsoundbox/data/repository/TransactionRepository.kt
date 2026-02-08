package com.app.virtualsoundbox.data.repository


import com.app.virtualsoundbox.data.local.TransactionDao
import com.app.virtualsoundbox.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TransactionRepository(private val dao: TransactionDao) {

    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactions()

    fun getTotalToday(): Flow<Double?> {
        // Hitung milidetik awal hari ini (Jam 00:00:00)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return dao.getTotalToday(calendar.timeInMillis)
    }

    suspend fun saveTransaction(trx: Transaction) {
        dao.insert(trx)
    }
}