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
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.app.virtualsoundbox.service.NotificationListener
import com.app.virtualsoundbox.ui.dashboard.DashboardScreen
import com.app.virtualsoundbox.ui.login.LoginScreen
import com.app.virtualsoundbox.ui.onboarding.OnboardingScreen
import com.app.virtualsoundbox.ui.theme.VirtualSoundboxTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedPref = getSharedPreferences("SoundHoreePrefs", Context.MODE_PRIVATE)

        setContent {
            VirtualSoundboxTheme {
                val lifecycleOwner = LocalLifecycleOwner.current

                // 1. States
                var isNotifEnabled by remember { mutableStateOf(isNotificationServiceEnabled()) }
                var showOnboarding by rememberSaveable {
                    mutableStateOf(sharedPref.getBoolean("isFirstRun", true))
                }
                var userName by remember {
                    mutableStateOf(sharedPref.getString("userName", null))
                }

                // 2. Observer Lifecycle
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isNotifEnabled = isNotificationServiceEnabled()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Handle Result */ }

                // 3. Side Effect Service
                LaunchedEffect(isNotifEnabled, showOnboarding, userName) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // Service jalan jika sudah selesai onboarding & login
                    if (isNotifEnabled && !showOnboarding && userName != null) {
                        startSoundService()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // --- NAVIGATION FLOW ---
                    when {
                        showOnboarding -> {
                            // Onboarding (Edukasi Awal - Belum Butuh Nama)
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
                        userName == null -> {
                            // Login (Input Nama)
                            LoginScreen(
                                onNameSaved = { name ->
                                    sharedPref.edit().putString("userName", name).apply()
                                    userName = name
                                }
                            )
                        }
                        else -> {
                            // Dashboard (Tampilkan Nama)
                            DashboardScreen(
                                userName = userName!!,
                                isNotificationEnabled = isNotifEnabled,
                                onOpenNotificationSettings = { openNotificationSettings() },
                                onOptimizeBattery = { requestChinesePhonePermissions(this@MainActivity) }
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