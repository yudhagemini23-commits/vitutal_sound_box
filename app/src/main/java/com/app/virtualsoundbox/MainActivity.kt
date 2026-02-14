package com.app.virtualsoundbox

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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

import com.app.virtualsoundbox.service.NotificationListener
import com.app.virtualsoundbox.ui.auth.GoogleAuthClient
import com.app.virtualsoundbox.ui.dashboard.DashboardScreen
import com.app.virtualsoundbox.ui.login.LoginScreen
import com.app.virtualsoundbox.ui.login.SignInState
import com.app.virtualsoundbox.ui.onboarding.OnboardingScreen
import com.app.virtualsoundbox.ui.profile.ProfileSetupScreen
import com.app.virtualsoundbox.ui.theme.VirtualSoundboxTheme
import kotlinx.coroutines.launch
import com.google.firebase.FirebaseApp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.app.virtualsoundbox.utils.UserSession

class MainActivity : ComponentActivity() {

    private val googleAuthUiClient by lazy {
        GoogleAuthClient(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()

        // Inisialisasi Session & Prefs
        val sharedPref = getSharedPreferences("SoundHoreePrefs", Context.MODE_PRIVATE)
        val userSession = UserSession(this)

        setContent {
            VirtualSoundboxTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                val scope = rememberCoroutineScope()

                // --- STATES ---
                var isNotifEnabled by remember { mutableStateOf(isNotificationServiceEnabled()) }

                // 1. Cek Onboarding
                var showOnboarding by rememberSaveable {
                    mutableStateOf(sharedPref.getBoolean("isFirstRun", true))
                }

                // 2. Cek apakah sudah Login Google
                var userData by remember { mutableStateOf(googleAuthUiClient.getSignedInUser()) }

                // 3. Cek apakah sudah Register ke Backend Golang (Punya Token)
                var isProfileSetup by remember { mutableStateOf(userSession.isUserLoggedIn()) }

                var state by remember { mutableStateOf(SignInState()) }

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

                // --- LAUNCHERS ---
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { }

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult(),
                    onResult = { result ->
                        if (result.resultCode == RESULT_OK) {
                            lifecycleScope.launch {
                                val signInResult = googleAuthUiClient.signInWithIntent(
                                    intent = result.data ?: return@launch
                                )
                                if (signInResult.data != null) {
                                    userData = signInResult.data
                                    // Simpan info sementara untuk nama di Setup Screen
                                    sharedPref.edit().putString("userName", userData?.userName).apply()
                                    Toast.makeText(applicationContext, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                                }
                                state = state.copy(
                                    isSuccess = signInResult.data != null,
                                    signInError = signInResult.errorMessage,
                                    isLoading = false
                                )
                            }
                        } else {
                            state = state.copy(isLoading = false)
                        }
                    }
                )

                // --- AUTO START SERVICE ---
                LaunchedEffect(isNotifEnabled, showOnboarding, isProfileSetup) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    // Service Aktif hanya jika: Notif Izin Aktif + Bukan Onboarding + Sudah Setup Profil (Punya Token)
                    if (isNotifEnabled && !showOnboarding && isProfileSetup) {
                        startSoundService()
                    }
                }

                // --- UI NAVIGATION FLOW ---
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        // LAYER 1: Onboarding (Cuma sekali seumur hidup aplikasi)
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

                        // LAYER 2: Google Login (Syarat awal)
                        userData == null -> {
                            LoginScreen(
                                state = state,
                                onSignInClick = {
                                    state = state.copy(isLoading = true)
                                    lifecycleScope.launch {
                                        val signInIntentSender = googleAuthUiClient.signIn()
                                        if (signInIntentSender != null) {
                                            launcher.launch(IntentSenderRequest.Builder(signInIntentSender).build())
                                        } else {
                                            state = state.copy(isLoading = false, signInError = "Gagal")
                                        }
                                    }
                                }
                            )
                        }

                        // LAYER 3: Profile Setup (Register ke Backend Golang)
                        !isProfileSetup -> {
                            ProfileSetupScreen(
                                onProfileSaved = { storeName ->
                                    // Update nama di SharedPref untuk display Dashboard
                                    sharedPref.edit().putString("userName", storeName).apply()
                                    // Trigger pindah ke Dashboard
                                    isProfileSetup = true
                                }
                            )
                        }

                        // LAYER 4: Dashboard (Sudah Login & Punya Token Golang)
                        else -> {
                            val displayStoreName = userSession.getUserId()?.let {
                                sharedPref.getString("userName", userData?.userName)
                            } ?: userData?.userName ?: "Juragan"

                            DashboardScreen(
                                userName = displayStoreName,
                                isNotificationEnabled = isNotifEnabled,
                                onOpenNotificationSettings = { openNotificationSettings() },
                                onOptimizeBattery = { requestChinesePhonePermissions(this@MainActivity) },
                                onLogout = {
                                    lifecycleScope.launch {
                                        // 1. Logout Google
                                        googleAuthUiClient.signOut()
                                        // 2. Logout Session Golang (Hapus Token & UID)
                                        userSession.logout()

                                        // Reset Local States
                                        userData = null
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

    // --- HELPER METHODS ---

    private fun openNotificationSettings() {
        try {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun startSoundService() {
        val intent = Intent(this, NotificationListener::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
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