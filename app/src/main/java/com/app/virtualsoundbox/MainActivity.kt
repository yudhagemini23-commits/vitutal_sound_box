package com.app.virtualsoundbox

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel

import com.app.virtualsoundbox.service.NotificationListener
import com.app.virtualsoundbox.ui.auth.GoogleAuthClient
import com.app.virtualsoundbox.ui.dashboard.DashboardScreen
import com.app.virtualsoundbox.ui.login.LoginScreen
import com.app.virtualsoundbox.ui.login.SignInState
import com.app.virtualsoundbox.ui.onboarding.OnboardingScreen
import com.app.virtualsoundbox.ui.profile.ProfileSetupScreen
import com.app.virtualsoundbox.ui.profile.ProfileViewModel
import com.app.virtualsoundbox.ui.theme.VirtualSoundboxTheme
import com.app.virtualsoundbox.utils.UserSession
import com.app.virtualsoundbox.data.remote.RetrofitClient
import com.app.virtualsoundbox.data.remote.model.LoginRequest
import kotlinx.coroutines.launch
import com.google.firebase.FirebaseApp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.app.virtualsoundbox.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val googleAuthUiClient by lazy {
        GoogleAuthClient(applicationContext)
    }

    @SuppressLint("UseKtx")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()

        val userSession = UserSession(this)
        val sharedPref = getSharedPreferences("SoundHoreePrefs", Context.MODE_PRIVATE)

        setContent {
            VirtualSoundboxTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                val profileViewModel: ProfileViewModel = viewModel()

                var isNotifEnabled by remember { mutableStateOf(isNotificationServiceEnabled()) }

                // --- GATE 1: ONBOARDING ---
                var showOnboarding by rememberSaveable {
                    mutableStateOf(sharedPref.getBoolean("isFirstRun", true))
                }

                // --- GATE 2: LOGIN STATE (Gunakan UserSession, bukan Google Client) ---
                var isLoggedIn by remember { mutableStateOf(userSession.isUserLoggedIn()) }

                // --- GATE 3: PROFILE SETUP ---
                var isProfileSetup by remember { mutableStateOf(userSession.isUserLoggedIn()) }

                var state by remember { mutableStateOf(SignInState()) }
                var userData by remember { mutableStateOf(googleAuthUiClient.getSignedInUser()) }

                // --- LIFECYCLE OBSERVER ---
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isNotifEnabled = isNotificationServiceEnabled()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                // --- GOOGLE SIGN IN LAUNCHER ---
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult(),
                    onResult = { result ->
                        if (result.resultCode == RESULT_OK) {
                            lifecycleScope.launch {
                                state = state.copy(isLoading = true)
                                val signInResult = googleAuthUiClient.signInWithIntent(
                                    intent = result.data ?: return@launch
                                )

                                val user = signInResult.data
                                if (user != null) {
                                    userData = user
                                    checkUserOnBackend(
                                        googleUid = user.userId,
                                        email = user.email ?: "",
                                        onResult = { isExist, storeName, token ->
                                            if (isExist) {
                                                // Simpan session permanen
                                                userSession.saveSession(token, user.userId, user.email ?: "", storeName)
                                                // Sync data transaksi
                                                profileViewModel.registerOrLogin(storeName, user.email ?: "", "", "", user.userId)

                                                isLoggedIn = true
                                                isProfileSetup = true
                                                Toast.makeText(applicationContext, "Selamat Datang Kembali!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                isProfileSetup = false
                                                // User login Google sukses tapi profil belum ada di MySQL
                                                isLoggedIn = true
                                            }
                                            state = state.copy(isLoading = false, isSuccess = true)
                                        },
                                        onError = { error ->
                                            state = state.copy(isLoading = false, signInError = error)
                                        }
                                    )
                                } else {
                                    state = state.copy(isLoading = false, signInError = signInResult.errorMessage)
                                }
                            }
                        } else {
                            state = state.copy(isLoading = false)
                        }
                    }
                )

                // --- AUTO START SERVICE ---
                LaunchedEffect(isNotifEnabled, showOnboarding, isLoggedIn, isProfileSetup) {
                    if (isNotifEnabled && !showOnboarding && isLoggedIn && isProfileSetup) {
                        startSoundService()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when {
                        showOnboarding -> {
                            OnboardingScreen(
                                isNotificationEnabled = isNotifEnabled,
                                onFinished = {
                                    sharedPref.edit().putBoolean("isFirstRun", false).apply()
                                    showOnboarding = false
                                },
                                onOpenNotifSettings = { openNotificationSettings() },
                                onOptimizeBattery = { requestChinesePhonePermissions(this@MainActivity) }
                            )
                        }
                        !isLoggedIn -> {
                            LoginScreen(
                                state = state,
                                onSignInClick = {
                                    state = state.copy(isLoading = true)
                                    lifecycleScope.launch {
                                        val signInIntentSender = googleAuthUiClient.signIn()
                                        if (signInIntentSender != null) {
                                            launcher.launch(IntentSenderRequest.Builder(signInIntentSender).build())
                                        } else {
                                            state = state.copy(isLoading = false, signInError = "Gagal Login")
                                        }
                                    }
                                }
                            )
                        }
                        !isProfileSetup -> {
                            ProfileSetupScreen(
                                onProfileSaved = { storeName ->
                                    isProfileSetup = true
                                }
                            )
                        }
                        else -> {
                            // Dashboard Utama
                            DashboardScreen(
                                userName = userSession.getUserId() ?: "Juragan",
                                isNotificationEnabled = isNotifEnabled,
                                onOpenNotificationSettings = { openNotificationSettings() },
                                onOptimizeBattery = { requestChinesePhonePermissions(this@MainActivity) },
                                onLogout = {
                                    lifecycleScope.launch {
                                        googleAuthUiClient.signOut()
                                        userSession.logout()

                                        withContext(Dispatchers.IO) {
                                            val db = AppDatabase.getDatabase(this@MainActivity)
                                            db.transactionDao().deleteAllTransactions()
                                            db.userProfileDao().deleteAllProfiles()
                                        }

                                        userData = null
                                        isLoggedIn = false
                                        isProfileSetup = false
                                        state = SignInState()

                                        Toast.makeText(applicationContext, "Berhasil Keluar", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Sinkronisasi data premium dan trial dari MySQL Backend
     */
    private fun checkUserOnBackend(
        googleUid: String,
        email: String,
        onResult: (Boolean, String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        val userSession = UserSession(this)
        lifecycleScope.launch {
            try {
                val request = LoginRequest(uid = googleUid, email = email, storeName = "", phoneNumber = "", category = "")
                val response = RetrofitClient.instance.loginUser(request)

                if (response.isSuccessful && response.body() != null) {
                    val authBody = response.body()!!
                    val profile = authBody.user
                    val sub = authBody.subscription

                    // --- [SINKRONISASI KRUSIAL] ---
                    // Simpan status premium dan trial asli dari MySQL ke SharedPreferences lokal
                    if (sub != null) {
                        userSession.savePremiumStatus(
                            isPremium = sub.isPremium,
                            remainingTrial = sub.remainingTrial
                        )
                    }

                    val isExist = !profile?.storeName.isNullOrBlank()
                    onResult(isExist, profile?.storeName ?: "", authBody.token)
                } else {
                    onError("Gagal koneksi server: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AKD_AUTH", "Error Backend: ${e.message}")
                onError("Kesalahan Jaringan")
            }
        }
    }

    private fun openNotificationSettings() {
        try { startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
        catch (e: Exception) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }

    private fun startSoundService() {
        val intent = Intent(this, NotificationListener::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }

    private fun requestChinesePhonePermissions(context: Context) {
        val intent = Intent()
        val manufacturer = Build.MANUFACTURER.lowercase()
        try {
            when {
                manufacturer.contains("xiaomi") -> intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                manufacturer.contains("oppo") -> intent.component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
                manufacturer.contains("vivo") -> intent.component = ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
                else -> intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.fromParts("package", packageName, null)
            context.startActivity(intent)
        }
    }
}