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
import com.app.virtualsoundbox.ui.profile.ProfileSetupScreen // Import Screen Baru
import com.app.virtualsoundbox.ui.theme.VirtualSoundboxTheme
import kotlinx.coroutines.launch
import com.google.firebase.FirebaseApp

// Import Wajib untuk "by remember"
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private val googleAuthUiClient by lazy {
        GoogleAuthClient(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()

        val sharedPref = getSharedPreferences("SoundHoreePrefs", Context.MODE_PRIVATE)

        setContent {
            VirtualSoundboxTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                val scope = rememberCoroutineScope()

                // --- STATES ---
                var isNotifEnabled by remember { mutableStateOf(isNotificationServiceEnabled()) }

                // State Navigasi
                var showOnboarding by rememberSaveable {
                    mutableStateOf(sharedPref.getBoolean("isFirstRun", true))
                }

                // State: Apakah Profil Toko sudah diisi?
                var isProfileSetup by rememberSaveable {
                    mutableStateOf(sharedPref.getBoolean("isProfileSetup", false))
                }

                var state by remember { mutableStateOf(SignInState()) }
                var userData by remember { mutableStateOf(googleAuthUiClient.getSignedInUser()) }

                // --- LIFECYCLE ---
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
                                val user = signInResult.data
                                state = state.copy(
                                    isSuccess = user != null,
                                    signInError = signInResult.errorMessage,
                                    isLoading = false
                                )

                                if (user != null) {
                                    userData = user
                                    // Simpan sesi login sementara
                                    with(sharedPref.edit()) {
                                        putString("tempUserName", user.userName)
                                        putString("userEmail", user.email)
                                        putString("userId", user.userId)
                                        apply()
                                    }
                                    Toast.makeText(applicationContext, "Login Google Berhasil!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            state = state.copy(isLoading = false)
                        }
                    }
                )

                // --- START SERVICE ---
                LaunchedEffect(isNotifEnabled, showOnboarding, userData, isProfileSetup) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    // Service jalan hanya jika sudah LOGIN + PROFIL LENGKAP
                    if (isNotifEnabled && !showOnboarding && userData != null && isProfileSetup) {
                        startSoundService()
                    }
                }

                // --- UI NAVIGATION FLOW ---
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        // 1. Onboarding
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

                        // 2. Login (Jika User belum ada)
                        userData == null -> {
                            LoginScreen(
                                state = state,
                                onSignInClick = {
                                    state = state.copy(isLoading = true)
                                    lifecycleScope.launch {
                                        val signInIntentSender = googleAuthUiClient.signIn()
                                        if (signInIntentSender != null) {
                                            launcher.launch(
                                                IntentSenderRequest.Builder(signInIntentSender).build()
                                            )
                                        } else {
                                            state = state.copy(isLoading = false, signInError = "Gagal membuka akun Google")
                                        }
                                    }
                                }
                            )
                        }

                        // 3. Profile Setup (User ADA, tapi Profile Belum Setup)
                        // INI BAGIAN PENTING UNTUK DATA STORE
                        !isProfileSetup -> {
                            ProfileSetupScreen(
                                onProfileSaved = { storeName ->
                                    // Simpan status bahwa profil sudah diisi
                                    with(sharedPref.edit()) {
                                        putBoolean("isProfileSetup", true)
                                        putString("userName", storeName) // Update nama tampilan jadi Nama Toko
                                        apply()
                                    }
                                    isProfileSetup = true // Trigger pindah ke Dashboard
                                }
                            )
                        }

                        // 4. Dashboard (Semua syarat terpenuhi)
                        else -> {
                            // Ambil nama toko dari SharedPref, kalau null pakai nama Google
                            val displayStoreName = sharedPref.getString("userName", userData?.userName) ?: "Juragan"

                            DashboardScreen(
                                userName = displayStoreName,
                                isNotificationEnabled = isNotifEnabled,
                                onOpenNotificationSettings = { openNotificationSettings() },
                                onOptimizeBattery = { requestChinesePhonePermissions(this@MainActivity) },
                                onLogout = {
                                    lifecycleScope.launch {
                                        googleAuthUiClient.signOut()
                                        userData = null
                                        state = SignInState()

                                        // Reset Session
                                        sharedPref.edit().clear().apply()
                                        // Kembalikan flag onboarding agar user baru tidak kaget (opsional, bisa dihapus)
                                        // sharedPref.edit().putBoolean("isFirstRun", false).apply()

                                        isProfileSetup = false
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