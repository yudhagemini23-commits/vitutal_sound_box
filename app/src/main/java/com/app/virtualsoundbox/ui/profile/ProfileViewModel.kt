package com.app.virtualsoundbox.ui.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.virtualsoundbox.data.remote.RetrofitClient
import com.app.virtualsoundbox.data.remote.model.LoginRequest
import com.app.virtualsoundbox.utils.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userSession = UserSession(application)

    // State untuk UI (Loading, Success, Error)
    private val _setupState = MutableStateFlow<SetupState>(SetupState.Idle)
    val setupState = _setupState.asStateFlow()

    fun registerUser(storeName: String, email: String, phone: String, category: String) {
        _setupState.value = SetupState.Loading

        viewModelScope.launch {
            try {
                // 1. Generate UID Unik untuk Instalasi ini
                // (Nanti bisa diganti dengan UID dari Google Sign In jika sudah ada)
                val newUid = UUID.randomUUID().toString()
                val request = LoginRequest(
                    uid = newUid,
                    email = email,
                    storeName = storeName,
                    phoneNumber = phone,
                    category = category
                )

                // 2. Panggil API Backend
                Log.d("ProfileVM", "Mencoba Register: $request")
                val response = RetrofitClient.instance.loginUser(request)

                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()!!.token

                    // 3. Simpan Session ke HP (Permanen)
                    userSession.saveSession(
                        token = token,
                        uid = newUid,
                        email = email,
                        storeName = storeName
                    )

                    Log.d("ProfileVM", "✅ Register Sukses! Token: $token")
                    _setupState.value = SetupState.Success
                } else {
                    Log.e("ProfileVM", "❌ Gagal: ${response.errorBody()?.string()}")
                    _setupState.value = SetupState.Error("Gagal Register: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", "🔥 Error Koneksi: ${e.message}")
                _setupState.value = SetupState.Error("Koneksi Error: Pastikan Server Nyala")
            }
        }
    }
}

// Helper State
sealed class SetupState {
    object Idle : SetupState()
    object Loading : SetupState()
    object Success : SetupState()
    data class Error(val message: String) : SetupState()
}