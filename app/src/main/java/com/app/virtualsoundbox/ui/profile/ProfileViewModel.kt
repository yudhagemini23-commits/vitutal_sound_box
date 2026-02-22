package com.app.virtualsoundbox.ui.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.virtualsoundbox.data.local.AppDatabase
import com.app.virtualsoundbox.data.remote.RetrofitClient
import com.app.virtualsoundbox.data.remote.model.LoginRequest
import com.app.virtualsoundbox.data.remote.model.UpgradeRequest
import com.app.virtualsoundbox.model.Transaction
import com.app.virtualsoundbox.utils.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userSession = UserSession(application)

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
                    val user = authData.user // Objek User dari Backend

                    // 1. Simpan Session Dasar
                    userSession.saveSession(token, googleUid, email, storeName)

                    // 2. [SINKRONISASI] Simpan status Premium + Expiry Date dari Server
                    val sub = authData.subscription
                    userSession.savePremiumStatus(
                        isPremium = sub?.isPremium ?: false,
                        remainingTrial = sub?.remainingTrial ?: 0,
                        // PERBAIKAN: Gunakan ?. dan berikan nilai default 0L
                        expiresAt = user?.premiumExpiresAt ?: 0L
                    )

                    fetchNotificationRules()
                    pullTransactionsFromServer(googleUid, token)

                    _setupState.value = SetupState.Success

                } else {
                    _setupState.value = SetupState.Error("Gagal sinkronisasi dengan server")
                }
            } catch (e: Exception) {
                _setupState.value = SetupState.Error("Koneksi bermasalah: ${e.message}")
            }
        }
    }

    fun syncProfileFromServer(uid: String) {
        // 1. Ambil token yang tersimpan saat login
        val token = userSession.getToken() ?: ""

        viewModelScope.launch {
            try {
                // 2. Kirim token dengan format "Bearer <token>"
                val response = RetrofitClient.instance.getProfile("Bearer $token", uid)

                if (response.isSuccessful && response.body() != null) {
                    val profileData = response.body()!!.data

                    userSession.savePremiumStatus(
                        isPremium = profileData.isPremium,
                        remainingTrial = userSession.getRemainingTrial(),
                        expiresAt = profileData.premiumExpiresAt
                    )

                    _setupState.value = SetupState.Success
                    Log.d("Sync", "Profil berhasil diperbarui otomatis")
                } else if (response.code() == 401) {
                    Log.e("Sync", "Token expired atau tidak valid")
                }
            } catch (e: Exception) {
                Log.e("Sync", "Gagal sync profil: ${e.message}")
            }
        }
    }

    private suspend fun fetchNotificationRules() {
        try {
            val response = RetrofitClient.instance.getNotificationRules()
            if (response.isSuccessful && response.body() != null) {
                val rulesJson = Gson().toJson(response.body())
                userSession.saveNotificationRules(rulesJson)
            }
        } catch (e: Exception) {
            Log.e("AKD_RULES", "Gagal tarik aturan: ${e.message}")
        }
    }

    private suspend fun pullTransactionsFromServer(uid: String, token: String) {
        withContext(Dispatchers.IO) { // Pastikan jalan di Background Thread
            try {
                val response = RetrofitClient.instance.getTransactions(
                    token = "Bearer $token",
                    userId = uid,
                    start = 0,
                    end = System.currentTimeMillis()
                )

                if (response.isSuccessful) {
                    val serverData = response.body() ?: emptyList()
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
                    }
                }
            } catch (e: Exception) {
                Log.e("Sync", "Gagal tarik data: ${e.message}")
            }
        }
    }

    fun upgradePremium(planType: String, googleUid: String, purchaseToken: String = "", orderId: String = "") {
        _setupState.value = SetupState.Loading
        viewModelScope.launch {
            try {
                val token = userSession.getToken() ?: ""
                val requestBody = UpgradeRequest(
                    userId = googleUid,
                    planType = planType.lowercase(),
                    iapPurchaseToken = purchaseToken,
                    iapOrderId = orderId
                )

                val response = RetrofitClient.instance.upgradeToPremium(
                    token = "Bearer $token",
                    request = requestBody
                )

                if (response.isSuccessful && response.body() != null) {
                    // [SINKRONISASI] Ambil expiry_date asli dari response backend
                    val expiryDate = response.body()!!.expiryDate

                    userSession.savePremiumStatus(
                        isPremium = true,
                        remainingTrial = 0,
                        expiresAt = expiryDate // <--- UPDATE DENGAN DATA DARI GOOGLE
                    )
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

sealed class SetupState {
    object Idle : SetupState()
    object Loading : SetupState()
    object Success : SetupState()
    data class Error(val message: String) : SetupState()
}