package com.app.virtualsoundbox.utils

import android.util.Log
import java.util.regex.Pattern

object NotificationParser {

    // Regex ini mencari simbol Rp (opsional), spasi (opsional),
    // lalu menangkap angka yang bisa mengandung titik ribuan.
    private val AMOUNT_PATTERN = Pattern.compile(
        "(?i)(?:Rp|IDR)?\\s?([\\d]+(?:\\.[\\d]{3})*)"
    )

    fun extractAmount(text: String): Double {
        // 1. CLEANING TOTAL: Kita buang semua karakter yang bukan bagian dari pesan manusia
        // Ini untuk menghilangkan "android.app.Notification$BigTextStyle" dsb.
        val noisePattern = Pattern.compile("([a-zA-Z0-9]+\\.[a-zA-Z0-9.]+[$][a-zA-Z0-9]+)|(androidx?\\.[a-zA-Z0-9.]+)")
        val cleanText = noisePattern.matcher(text).replaceAll(" ")

        Log.d("AKD_PARSER", "CLEANED TEXT: $cleanText")

        val matcher = AMOUNT_PATTERN.matcher(cleanText)

        if (matcher.find()) {
            val raw = matcher.group(1) ?: return 0.0
            Log.d("AKD_PARSER", "MATCH FOUND: $raw")

            // 2. NORMALISASI: Hapus titik ribuan (1.000 -> 1000)
            val normalized = raw.replace(".", "")
            Log.d("AKD_PARSER", "FINAL NORMALIZED: $normalized")

            return normalized.toDoubleOrNull() ?: 0.0
        }

        return 0.0
    }

    fun getAppName(pkg: String): String {
        return when {
            pkg.contains("bca") -> "BCA Mobile"
            pkg.contains("dana") -> "DANA"
            pkg.contains("gojek") -> "GoPay"
            pkg.contains("mandiri") -> "Livin' Mandiri"
            pkg.contains("shell") -> "ADB Test"
            pkg.contains("virtualsoundbox") -> "Sound Horee Test"
            else -> "Unknown App"
        }
    }
}