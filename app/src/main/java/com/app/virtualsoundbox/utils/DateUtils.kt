package com.app.virtualsoundbox.utils

import java.util.Calendar

enum class DateFilter(val label: String) {
    TODAY("Hari Ini"),
    YESTERDAY("Kemarin"),
    LAST_7_DAYS("7 Hari"),
    THIS_MONTH("Bulan Ini")
}

object DateUtils {
    // Helper untuk mendapatkan awal hari (00:00:00)
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    // Helper untuk mendapatkan akhir hari (23:59:59)
    private fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    // Logika Inti: Menentukan Start & End date berdasarkan filter
    fun getRange(filter: DateFilter): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val end = getEndOfDay(now) // Default end adalah sekarang/akhir hari ini
        val start: Long

        when (filter) {
            DateFilter.TODAY -> {
                start = getStartOfDay(now)
            }
            DateFilter.YESTERDAY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                start = getStartOfDay(calendar.timeInMillis)
                val endYesterday = getEndOfDay(calendar.timeInMillis)
                return Pair(start, endYesterday) // Khusus kemarin, end-nya bukan hari ini
            }
            DateFilter.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                start = getStartOfDay(calendar.timeInMillis)
            }
            DateFilter.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                start = getStartOfDay(calendar.timeInMillis)
            }
        }
        return Pair(start, end)
    }
}