package com.app.virtualsoundbox.ui.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.virtualsoundbox.data.local.AppDatabase
import com.app.virtualsoundbox.data.remote.RetrofitClient
import com.app.virtualsoundbox.data.remote.model.LoginRequest
import com.app.virtualsoundbox.data.remote.model.TransactionDto
import com.app.virtualsoundbox.data.remote.model.UpgradeRequest
import com.app.virtualsoundbox.model.Transaction
import com.app.virtualsoundbox.utils.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.gson.Gson

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userSession = UserSession(application)

    // State untuk UI (Loading, Success, Error)
    private val _setupState = MutableStateFlow<SetupState>(SetupState.Idle)
    val setupState = _setupState.asStateFlow()

    fun registerOrLogin(storeName: String, email: String, phone: String, category: String, googleUid: String) {
        _setupState.value = SetupState.Loading
        viewModelScope.launch {
            try {
                val request = LoginRequest(
                    uid = googleUid,
                    email = email,
                    storeName = storeName,
                    phoneNumber = phone,
                    category = category
                )

                val response = RetrofitClient.instance.loginUser(request)

                if (response.isSuccessful && response.body() != null) {
                    val authData = response.body()!!
                    val token = authData.token

                    // 1. Simpan Session Dasar (UID, Email, Token)
                    userSession.saveSession(token, googleUid, email, storeName)

                    // 2. [SOLUSI] Update status Premium & Trial dari hasil login
                    val sub = authData.subscription
                    if (sub != null) {
                        // Simpan data ke SharedPreferences agar Dashboard langsung update
                        userSession.savePremiumStatus(
                            isPremium = sub.isPremium,
                            remainingTrial = sub.remainingTrial
                        )
                    }

                    fetchNotificationRules()
                    pullTransactionsFromServer(googleUid, token)

                    _setupState.value = SetupState.Success

                } else {
                    _setupState.value = SetupState.Error("Gagal sinkronisasi dengan server")
                }
            } catch (e: Exception) {
                _setupState.value = SetupState.Error("Koneksi bermasalah")
            }
        }
    }

    // --- FUNGSI BARU UNTUK TARIK CONFIG ---
    private suspend fun fetchNotificationRules() {
        try {
            val response = RetrofitClient.instance.getNotificationRules()
            if (response.isSuccessful && response.body() != null) {
                val rulesJson = Gson().toJson(response.body())
                userSession.saveNotificationRules(rulesJson)
                Log.d("AKD_RULES", "Berhasil sinkronisasi aturan notifikasi dari server")
            }
        } catch (e: Exception) {
            Log.e("AKD_RULES", "Gagal tarik aturan notifikasi: ${e.message}")
        }
    }

    private suspend fun pullTransactionsFromServer(uid: String, token: String) {
        try {
            // Panggil endpoint GET /transactions
            val response = RetrofitClient.instance.getTransactions(
                token = "Bearer $token",
                userId = uid,
                start = 0, // Ambil semua data sejarah
                end = System.currentTimeMillis()
            )

            if (response.isSuccessful) {
                val serverData = response.body() ?: emptyList() // Gunakan Elvis operator biar aman dari null

                if (serverData.isNotEmpty()) {
                    val localEntities = serverData.map { dto ->
                        Transaction(
                            sourceApp = dto.sourceApp,
                            amount = dto.amount,
                            rawMessage = dto.rawMessage,
                            timestamp = dto.timestamp,
                            isTrialLimited = dto.isTrialLimited
                        )
                    }

                    AppDatabase.getDatabase(getApplication()).transactionDao().insertAll(localEntities)
                    Log.d("Sync", "✅ Berhasil menarik ${localEntities.size} transaksi dari server")
                }
            }

        } catch (e: Exception) {
            Log.e("Sync", "Gagal tarik data: ${e.message}")
        }
    }

    fun upgradePremium(planType: String, googleUid: String) {
        _setupState.value = SetupState.Loading
        viewModelScope.launch {
            try {
                val token = userSession.getToken() ?: ""
                val response = RetrofitClient.instance.upgradeToPremium(
                    token = "Bearer $token",
                    request = UpgradeRequest(userId = googleUid, planType = planType.lowercase())
                )

                if (response.isSuccessful) {
                    // --- PERBAIKAN: Parameter sekarang sudah sesuai dengan UserSession ---
                    userSession.savePremiumStatus(isPremium = true, remainingTrial = 0)
                    _setupState.value = SetupState.Success
                } else {
                    _setupState.value = SetupState.Error("Verifikasi server gagal")
                }
            } catch (e: Exception) {
                _setupState.value = SetupState.Error("Koneksi bermasalah")
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