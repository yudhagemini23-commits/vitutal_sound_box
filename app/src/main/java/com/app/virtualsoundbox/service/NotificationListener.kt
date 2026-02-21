package com.app.virtualsoundbox.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.app.virtualsoundbox.MainActivity
import com.app.virtualsoundbox.R
import com.app.virtualsoundbox.data.local.AppDatabase
import com.app.virtualsoundbox.data.remote.RetrofitClient
import com.app.virtualsoundbox.data.repository.TransactionRepository
import com.app.virtualsoundbox.model.NotificationRule
import com.app.virtualsoundbox.model.Transaction
import com.app.virtualsoundbox.utils.NotificationParser
import com.app.virtualsoundbox.utils.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay

class NotificationListener : NotificationListenerService(), TextToSpeech.OnInitListener {

    private lateinit var repository: TransactionRepository
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val scope = CoroutineScope(Dispatchers.IO)
    private val CHANNEL_ID = "SoundHoree_Service"
    private val NOTIF_ID = 99

    private val TARGET_APPS by lazy {
        listOf(
            "com.bca",
            "id.dana",
            "mandiri.online",
            "com.gojek.app",
            "com.android.shell",
            packageName
        )
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("AKD_DEBUG", "Service onCreate terpanggil")
        val db = AppDatabase.getDatabase(applicationContext)
        repository = TransactionRepository(db.transactionDao())
        tts = TextToSpeech(this, this, "com.google.android.tts")
        startAsForeground()
        startPeriodicConfigSync()
    }

    // --- TAMBAHAN PROPER LIFECYCLE LISTENER ---
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("AKD_DEBUG", "Sistem Android berhasil menyambungkan NotificationListener!")
        // Panggil lagi untuk memastikan foreground aktif saat sistem benar-benar terkoneksi
        startAsForeground()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.e("AKD_DEBUG", "NotificationListener terputus dari sistem!")
        // Minta OS Android untuk menyambungkan kembali jika terbunuh paksa
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName(this, NotificationListener::class.java))
        }
    }
    // ------------------------------------------

    private fun startPeriodicConfigSync() {
        scope.launch {
            while (true) {
                syncRulesFromServer()
                // Tunggu 12 jam sebelum narik data lagi (12 jam * 60 mnt * 60 dtk * 1000 ms)
                delay(12L * 60 * 60 * 1000)
            }
        }
    }

    private suspend fun syncRulesFromServer() {
        try {
            val response = RetrofitClient.instance.getNotificationRules()
            if (response.isSuccessful && response.body() != null) {
                val rulesJson = Gson().toJson(response.body())
                val userSession = UserSession(applicationContext)

                // Cek apakah ada perubahan biar nggak buang-buang resource nyimpen kalau sama
                if (userSession.getNotificationRules() != rulesJson) {
                    userSession.saveNotificationRules(rulesJson)
                    Log.d("AKD_RULES", "Background Sync: Aturan notifikasi berhasil di-update dari server!")
                }
            }
        } catch (e: Exception) {
            Log.e("AKD_RULES", "Background Sync Gagal: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AKD_DEBUG", "Service onStartCommand terpanggil")
        startAsForeground()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext, 1, restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        super.onTaskRemoved(rootIntent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val pkgName = sbn.packageName ?: ""

        val userSession = UserSession(applicationContext)
        val rulesJson = userSession.getNotificationRules()

        // 1. Jika Config dari server kosong, diam saja (abaikan)
        if (rulesJson.isNullOrEmpty()) return

        // 2. Parse Config JSON ke List Object
        val type = object : TypeToken<List<NotificationRule>>() {}.type
        val rules: List<NotificationRule> = Gson().fromJson(rulesJson, type)

        // 3. Cek apakah package name notifikasi cocok dengan yang ada di Database Server
        val activeRule = rules.find { pkgName.contains(it.packageName, ignoreCase = true) }
        if (activeRule == null) return // Abaikan jika aplikasi tidak ada di daftar pantauan

        // --- KUMPULKAN TEKS NOTIFIKASI ---
        val extras = notification.extras
        val allTexts = mutableListOf<String>()

        extras.getCharSequence("android.title")?.let { allTexts.add(it.toString()) }
        extras.getCharSequence("android.text")?.let { allTexts.add(it.toString()) }
        extras.getCharSequence("android.bigText")?.let { allTexts.add(it.toString()) }

        if (allTexts.isEmpty()) {
            for (key in extras.keySet()) {
                val value = extras.get(key)
                if (value is CharSequence || value is String) allTexts.add(value.toString())
            }
        }

        val finalText = allTexts.joinToString(" ").trim()
        var amount = 0.0

        // --- 4. EKSEKUSI REGEX DINAMIS DARI SERVER ---
        try {
            val regex = Regex(activeRule.regexPattern, RegexOption.IGNORE_CASE)
            val matchResult = regex.find(finalText)

            if (matchResult != null && matchResult.groupValues.size > 1) {
                // Ambil grup ke-1 (angka) dan buang pemisah ribuan (titik/koma)
                val rawAmountStr = matchResult.groupValues[1]
                val cleanAmountStr = rawAmountStr.replace(Regex("[^\\d]"), "")
                amount = cleanAmountStr.toDoubleOrNull() ?: 0.0
            }
        } catch (e: Exception) {
            Log.e("AKD_REGEX", "Error eksekusi regex untuk ${activeRule.appName}: ${e.message}")
            return
        }

        // --- 5. JIKA UANG DITEMUKAN, PROSES TRANSAKSI ---
        if (amount > 0) {
            val isPremium = userSession.isPremium()
            val currentRemaining = userSession.getRemainingTrial()

            val canPlaySound = isPremium || currentRemaining > 0
            val isLimitReached = !canPlaySound

            if (!isPremium && currentRemaining > 0) {
                userSession.savePremiumStatus(isPremium = false, remainingTrial = currentRemaining - 1)
            }

            val cleanDisplay = finalText
                .replace(Regex("(?i)android\\.app\\.Notification\\$[a-zA-Z]+"), "")
                .replace("\n", " ").replace(Regex("\\s+"), " ").trim().take(100)

            val trx = Transaction(
                sourceApp = pkgName,
                amount = amount,
                rawMessage = cleanDisplay,
                timestamp = System.currentTimeMillis(),
                isTrialLimited = isLimitReached
            )

            val token = userSession.getToken()
            val uid = userSession.getUserId()

            scope.launch {
                if (token != null && uid != null) repository.insert(trx, token, uid)
                else repository.insert(trx)
            }

            // --- 6. SUARA (TTS) DINAMIS DARI SERVER ---
            if (canPlaySound) {
                val amountString = String.format(Locale("id", "ID"), "%.0f", amount)

                // Ganti tag {amount} dan {app_name} sesuai format di MySQL
                val ttsMessage = activeRule.ttsFormat
                    .replace("{amount}", amountString)
                    .replace("{app_name}", activeRule.appName)

                speak(ttsMessage)
            } else {
                Log.d("AKD_LISTENER", "Trial Habis & Belum Premium. Suara diblokir.")
            }
        }
    }

    private fun speak(message: String) {
        if (isTtsReady && tts != null) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "SoundHoreeID")
        } else {
            Log.e("AKD_LISTENER", "TTS belum siap.")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val localeId = Locale("id", "ID")
            val result = tts?.setLanguage(localeId)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            isTtsReady = true
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    @SuppressLint("ForegroundServiceType")
    private fun startAsForeground() {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Layanan Sound Horee Tetap Aktif",
                    // Naikkan importance ke DEFAULT agar lebih diutamakan sistem
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Mengamankan agar suara notifikasi uang masuk selalu aktif"
                }
                manager.createNotificationChannel(channel)
            }

            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sound Horee Aktif")
                .setContentText("Siap mendengarkan notifikasi uang masuk...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Sesuaikan dengan channel
                .build()

            // Perbaikan untuk Android 14+ (API 34+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIF_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIF_ID, notification)
            }
            Log.d("AKD_DEBUG", "startAsForeground: SUKSES dijalankan")
        } catch (e: Exception) {
            Log.e("AKD_DEBUG", "startAsForeground GAGAL: ${e.message}")
        }
    }
}