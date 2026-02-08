package com.app.virtualsoundbox.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceApp: String,   // misal: "BCA Mobile"
    val amount: Double,      // misal: 50000.0
    val rawMessage: String,  // Teks asli notifikasi
    val timestamp: Long = System.currentTimeMillis()
)