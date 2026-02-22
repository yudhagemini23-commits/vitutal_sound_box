package com.app.virtualsoundbox.data.remote.model

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: UserProfileDto // UserProfileDto yang sudah kita buat tadi
)

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
    @SerializedName("user") val user: UserProfileDto?, // UserProfileDto harus punya field baru
    @SerializedName("subscription") val subscription: SubscriptionDto?
)

data class UserProfileDto(
    @SerializedName("uid") val uid: String,
    @SerializedName("email") val email: String?,
    @SerializedName("store_name") val storeName: String?,
    @SerializedName("phone_number") val phoneNumber: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("joined_at") val joinedAt: Long,
    @SerializedName("is_premium") val isPremium: Boolean,

    // TAMBAHKAN INI agar ProfileViewModel bisa membaca data dari server
    @SerializedName("premium_expires_at") val premiumExpiresAt: Long
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
    @SerializedName("user_id") // Jika backend Go masih butuh user_id di body (opsional)
    val userId: String,

    @SerializedName("plan_type")
    val planType: String,

    @SerializedName("iap_purchase_token")
    val iapPurchaseToken: String = "",

    @SerializedName("iap_order_id")
    val iapOrderId: String = ""
)

data class UpgradeResponse(
    @SerializedName("status") val status: String,
    @SerializedName("expiry_date") val expiryDate: Long)