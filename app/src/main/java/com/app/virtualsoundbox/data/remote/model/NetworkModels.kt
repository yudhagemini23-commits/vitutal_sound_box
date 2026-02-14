package com.app.virtualsoundbox.data.remote.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("uid") val uid: String,
    @SerializedName("email") val email: String,
    @SerializedName("store_name") val storeName: String,
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("category") val category: String
)

data class AuthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserProfileDto? // <-- TAMBAHKAN INI
)

// Model untuk menampung data user dari MySQL
data class UserProfileDto(
    @SerializedName("uid") val uid: String,
    @SerializedName("store_name") val storeName: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("phone_number") val phoneNumber: String?,
    @SerializedName("category") val category: String?
)

data class TransactionDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("source_app") val sourceApp: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("raw_message") val rawMessage: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("is_trial_limited") val isTrialLimited: Boolean
)