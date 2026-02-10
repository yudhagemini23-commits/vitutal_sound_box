package com.app.virtualsoundbox.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val uid: String, // Kita pakai UID dari Google sebagai ID Unik
    val email: String,
    val storeName: String,
    val phoneNumber: String,
    val category: String,
    val joinedAt: Long,
    val isSynced: Boolean = false // Flag untuk sinkronisasi ke Golang nanti
)