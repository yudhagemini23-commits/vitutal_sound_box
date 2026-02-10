package com.app.virtualsoundbox.data.repository

import com.app.virtualsoundbox.data.local.TransactionDao
import com.app.virtualsoundbox.model.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    // Fungsi Lama
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun insert(transaction: Transaction) {
        transactionDao.insert(transaction)
    }

    // --- FUNGSI BARU (JEMBATAN) ---

    // 1. Ambil transaksi sesuai range tanggal
    fun getTransactionsByDateRange(start: Long, end: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(start, end)
    }

    // 2. Ambil total uang sesuai range tanggal
    fun getTotalAmountByDateRange(start: Long, end: Long): Flow<Double?> {
        return transactionDao.getTotalAmountByDateRange(start, end)
    }

    // 3. Hapus transaksi
    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }
}