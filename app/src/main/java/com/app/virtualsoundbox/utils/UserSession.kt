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
    }

    // Simpan Data saat Login Sukses
    fun saveSession(token: String, uid: String, email: String, storeName: String) {
        val editor = prefs.edit()
        editor.putString(KEY_TOKEN, token)
        editor.putString(KEY_UID, uid)
        editor.putString(KEY_EMAIL, email)
        editor.putString(KEY_STORE_NAME, storeName)
        editor.apply()
    }

    // Ambil Token
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    // Ambil User ID
    fun getUserId(): String? {
        return prefs.getString(KEY_UID, null)
    }

    // Cek apakah User sudah Login
    fun isUserLoggedIn(): Boolean {
        return getToken() != null && getUserId() != null
    }

    // Logout (Hapus Data)
    fun logout() {
        prefs.edit().clear().apply()
    }
}