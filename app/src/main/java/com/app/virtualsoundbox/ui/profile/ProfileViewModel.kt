package com.app.virtualsoundbox.ui.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.virtualsoundbox.data.local.AppDatabase
import com.app.virtualsoundbox.data.remote.RetrofitClient
import com.app.virtualsoundbox.data.remote.model.LoginRequest
import com.app.virtualsoundbox.data.remote.model.TransactionDto
import com.app.virtualsoundbox.model.Transaction
import com.app.virtualsoundbox.utils.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
                    uid = googleUid, // Gunakan UID Google yang konsisten
                    email = email,
                    storeName = storeName,
                    phoneNumber = phone,
                    category = category
                )

                val response = RetrofitClient.instance.loginUser(request)

                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()!!.token

                    // 1. Simpan Session
                    userSession.saveSession(token, googleUid, email, storeName)

                    // 2. [KRUSIAL] Tarik data transaksi lama dari server ke Room
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
}

// Helper State
sealed class SetupState {
    object Idle : SetupState()
    object Loading : SetupState()
    object Success : SetupState()
    data class Error(val message: String) : SetupState()
}