package com.app.virtualsoundbox.data.repository

import com.app.virtualsoundbox.data.local.TransactionDao
import com.app.virtualsoundbox.model.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    // Ambil semua (Default)
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    // Ambil berdasarkan Range Tanggal (Untuk Filter Bulan)
    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(start, end)
    }

    suspend fun insert(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }
}