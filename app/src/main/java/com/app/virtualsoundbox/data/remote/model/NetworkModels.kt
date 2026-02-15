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
    @SerializedName("user") val user: UserProfileDto?,
    @SerializedName("subscription") val subscription: SubscriptionDto?
)

data class UserProfileDto(
    @SerializedName("uid") val uid: String,
    @SerializedName("email") val email: String?,
    @SerializedName("store_name") val storeName: String?,
    @SerializedName("phone_number") val phoneNumber: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("joined_at") val joinedAt: Long, // Tambahkan ini (WAJIB Long)
    @SerializedName("is_premium") val isPremium: Boolean // Tambahkan ini
)

data class TransactionDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("source_app") val sourceApp: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("raw_message") val rawMessage: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("is_trial_limited") val isTrialLimited: Boolean
)

data class SubscriptionDto(
    @SerializedName("is_premium") val isPremium: Boolean,
    @SerializedName("trial_limit") val trialLimit: Int,
    @SerializedName("trial_usage") val trialUsage: Int,
    @SerializedName("remaining_trial") val remainingTrial: Int
)

data class UpgradeRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("plan_type") val planType: String
)

data class UpgradeResponse(
    @SerializedName("status") val status: String,
    @SerializedName("expiry_date") val expiryDate: Long
)