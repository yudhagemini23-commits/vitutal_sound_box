package com.app.virtualsoundbox

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
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
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.app.virtualsoundbox.service.NotificationListener // Import Service
import com.app.virtualsoundbox.ui.dashboard.DashboardScreen
import com.app.virtualsoundbox.ui.theme.VirtualSoundboxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VirtualSoundboxTheme {
                var isNotifEnabled by remember { mutableStateOf(isNotificationServiceEnabled()) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Handle Result */ }

                LaunchedEffect(Unit) {
                    // Minta izin Post Notif (Android 13+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // --- PERUBAHAN DISINI: Jalankan Service secara eksplisit agar persistent ---
                    if (isNotifEnabled) {
                        val serviceIntent = Intent(this@MainActivity, NotificationListener::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent)
                        } else {
                            startService(serviceIntent)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(
                        isNotificationEnabled = isNotifEnabled,
                        onOpenNotificationSettings = {
                            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                        },
                        onOptimizeBattery = {
                            requestChinesePhonePermissions(this@MainActivity)
                        },
                    )
                }
            }
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