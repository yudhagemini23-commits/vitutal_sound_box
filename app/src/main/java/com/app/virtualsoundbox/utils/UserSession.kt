package com.app.virtualsoundbox.utils

import android.content.Context
import android.content.SharedPreferences

class UserSession(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("SoundHoreeSession", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_UID = "user_uid"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_STORE_NAME = "store_name"
        private const val KEY_IS_PREMIUM = "isPremium"
        private const val KEY_REMAINING_TRIAL = "remaining_trial"
        private const val KEY_NOTIF_RULES = "notif_rules"
        private const val KEY_EXPIRES_AT = "premium_expires_at" // Field Baru
    }

    fun saveSession(token: String, uid: String, email: String, storeName: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_UID, uid)
            putString(KEY_EMAIL, email)
            putString(KEY_STORE_NAME, storeName)
            apply()
        }
    }

    // UPDATE: Sekarang menerima expiry time dari server
    fun savePremiumStatus(isPremium: Boolean, remainingTrial: Int = 0, expiresAt: Long = 0L) {
        prefs.edit().apply {
            putBoolean(KEY_IS_PREMIUM, isPremium)
            putInt(KEY_REMAINING_TRIAL, remainingTrial)
            putLong(KEY_EXPIRES_AT, expiresAt)
            apply()
        }
    }

    fun saveNotificationRules(rulesJson: String) {
        prefs.edit().putString(KEY_NOTIF_RULES, rulesJson).apply()
    }

    fun getNotificationRules(): String? = prefs.getString(KEY_NOTIF_RULES, null)
    fun isPremium(): Boolean = prefs.getBoolean(KEY_IS_PREMIUM, false)
    fun getRemainingTrial(): Int = prefs.getInt(KEY_REMAINING_TRIAL, 0)
    fun getPremiumExpiresAt(): Long = prefs.getLong(KEY_EXPIRES_AT, 0L) // Getter Baru
    fun getStoreName(): String? = prefs.getString(KEY_STORE_NAME, null)
    fun getUserEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getUserId(): String? = prefs.getString(KEY_UID, null)
    fun isUserLoggedIn(): Boolean = getToken() != null && getUserId() != null

    fun logout() {
        prefs.edit().clear().apply()
    }
}