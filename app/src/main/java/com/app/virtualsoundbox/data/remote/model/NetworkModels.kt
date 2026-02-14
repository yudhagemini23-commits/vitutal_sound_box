package com.app.virtualsoundbox.data.remote.model

import com.google.gson.annotations.SerializedName

// Request untuk Login
data class LoginRequest(
    @SerializedName("uid") val uid: String,
    @SerializedName("email") val email: String,
    @SerializedName("store_name") val storeName: String,
    @SerializedName("phone_number") val phoneNumber: String
)

// Response Login (Dapat Token)
data class AuthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("token") val token: String
)

// DTO untuk kirim Transaksi (Beda dengan Entity Room)
data class TransactionDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("source_app") val sourceApp: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("raw_message") val rawMessage: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("is_trial_limited") val isTrialLimited: Boolean
)