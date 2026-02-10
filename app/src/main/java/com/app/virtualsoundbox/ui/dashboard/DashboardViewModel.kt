package com.app.virtualsoundbox.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.virtualsoundbox.data.repository.TransactionRepository
import com.app.virtualsoundbox.model.Transaction
import com.app.virtualsoundbox.utils.DateFilter
import com.app.virtualsoundbox.utils.DateUtils
import com.app.virtualsoundbox.utils.NotificationParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: TransactionRepository) : ViewModel() {

    // 1. State Filter (Default: Hari Ini)
    private val _selectedFilter = MutableStateFlow(DateFilter.TODAY)
    val selectedFilter: StateFlow<DateFilter> = _selectedFilter.asStateFlow()

    // 2. Transaksi (Berubah sesuai Filter)
    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<Transaction>> = _selectedFilter
        .flatMapLatest { filter ->
            val (start, end) = DateUtils.getRange(filter)
            // PERBAIKAN: Panggil fungsi repo, bukan dao langsung
            repository.getTransactionsByDateRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Total Uang (Berubah sesuai Filter)
    @OptIn(ExperimentalCoroutinesApi::class)
    val totalAmount: StateFlow<Double> = _selectedFilter
        .flatMapLatest { filter ->
            val (start, end) = DateUtils.getRange(filter)
            // PERBAIKAN: Panggil fungsi repo, bukan dao langsung
            repository.getTotalAmountByDateRange(start, end)
        }
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // 4. Data Statistik untuk Pie Chart
    val appStats: StateFlow<Map<String, Float>> = transactions.map { list ->
        if (list.isEmpty()) return@map emptyMap()

        val total = list.sumOf { it.amount }.toFloat()
        list.groupBy { NotificationParser.getAppName(it.sourceApp) }
            .mapValues { entry ->
                val appTotal = entry.value.sumOf { it.amount }.toFloat()
                (appTotal / total) * 100f // Hitung Persentase
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Fungsi Ganti Filter
    fun setFilter(filter: DateFilter) {
        _selectedFilter.value = filter
    }

    // Fungsi Hapus Transaksi
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            // PERBAIKAN: Panggil fungsi repo, bukan dao langsung
            repository.deleteTransaction(transaction)
        }
    }
}

class DashboardViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}