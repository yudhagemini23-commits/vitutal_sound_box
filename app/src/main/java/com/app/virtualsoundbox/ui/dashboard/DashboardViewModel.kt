package com.app.virtualsoundbox.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.virtualsoundbox.data.repository.TransactionRepository
import com.app.virtualsoundbox.model.Transaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val _dateRange = MutableStateFlow(getTodayRange())

    private val _filterLabel = MutableStateFlow("Hari Ini")
    val filterLabel: StateFlow<String> = _filterLabel.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<Transaction>> = _dateRange
        .flatMapLatest { (start, end) ->
            repository.getTransactionsByDateRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- PERBAIKAN: Hanya hitung yang TIDAK TERKUNCI ---
    val totalIncome: StateFlow<Double> = transactions.map { list ->
        list.filter { !it.isTrialLimited }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- FUNGSI BARU: Panggil ini saat sukses Upgrade Premium ---
    fun unlockHistory() {
        viewModelScope.launch {
            repository.unlockAll()
        }
    }

    // --- LOGIC FILTER (TETAP SAMA) ---

    fun setFilterToday() {
        _dateRange.value = getTodayRange()
        _filterLabel.value = "Hari Ini"
    }

    fun setFilterThisMonth() {
        _dateRange.value = getMonthRange(Calendar.getInstance())
        _filterLabel.value = "Bulan Ini"
    }

    fun setFilterCustom(startMillis: Long, endMillis: Long?) {
        val end = endMillis ?: startMillis
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val finalStart = calendar.timeInMillis

        calendar.timeInMillis = end
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val finalEnd = calendar.timeInMillis

        _dateRange.value = Pair(finalStart, finalEnd)
        val dateFormat = SimpleDateFormat("dd MMM", Locale("id", "ID"))
        _filterLabel.value = "${dateFormat.format(Date(finalStart))} - ${dateFormat.format(Date(finalEnd))}"
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val start = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val end = calendar.timeInMillis
        return Pair(start, end)
    }

    private fun getMonthRange(cal: Calendar): Pair<Long, Long> {
        val startCal = cal.clone() as Calendar
        startCal.set(Calendar.DAY_OF_MONTH, 1)
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        val endCal = cal.clone() as Calendar
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    init {
        cleanUpExpiredTransactions()
    }

    private fun cleanUpExpiredTransactions() {
        viewModelScope.launch {
            val thirtyMinutesAgo = System.currentTimeMillis() - (30 * 60 * 1000)
            repository.deleteExpiredLimited(thirtyMinutesAgo)
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