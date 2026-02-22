package com.app.virtualsoundbox.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    @SerializedName("uid")
    val uid: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("store_name")
    val storeName: String,

    @SerializedName("phone_number")
    val phoneNumber: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("joined_at")
    val joinedAt: Long,

    @SerializedName("is_premium")
    val isPremium: Boolean = false, // TAMBAHKAN INI

    @SerializedName("premium_expires_at")
    val premiumExpiresAt: Long = 0L, // TAMBAHKAN INI (Gunakan nama yang sama dengan yang dipanggil di ViewModel)

    val isSynced: Boolean = false
)