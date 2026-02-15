package com.app.virtualsoundbox.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import com.app.virtualsoundbox.data.repository.TransactionRepository
import com.app.virtualsoundbox.model.Transaction
import com.app.virtualsoundbox.utils.NotificationParser
import com.app.virtualsoundbox.utils.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

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
        val db = AppDatabase.getDatabase(applicationContext)
        repository = TransactionRepository(db.transactionDao())
        tts = TextToSpeech(this, this, "com.google.android.tts")
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

        val isTarget = TARGET_APPS.any { pkgName.contains(it, ignoreCase = true) }
        if (!isTarget) return

        val extras = notification.extras
        val allTexts = mutableListOf<String>()

        extras.getCharSequence("android.title")?.let { allTexts.add(it.toString()) }
        extras.getCharSequence("android.text")?.let { allTexts.add(it.toString()) }
        extras.getCharSequence("android.bigText")?.let { allTexts.add(it.toString()) }

        if (allTexts.isEmpty()) {
            for (key in extras.keySet()) {
                val value = extras.get(key)
                if (value is CharSequence || value is String) {
                    allTexts.add(value.toString())
                }
            }
        }

        val finalText = allTexts.joinToString(" ").trim()
        val amount = NotificationParser.extractAmount(finalText)

        if (amount > 0) {
            // --- PERBAIKAN LOGIC SUBSCRIPTION: SOURCE FROM BACKEND (via UserSession) ---
            val userSession = UserSession(applicationContext)
            val isPremium = userSession.isPremium()
            val currentRemaining = userSession.getRemainingTrial()


            // Suara hanya bunyi jika: USER PREMIUM atau TRIAL MASIH ADA (> 0)
            val canPlaySound = isPremium || currentRemaining > 0
            val isLimitReached = !canPlaySound

            // Update jatah trial secara lokal (Optimistic Update)
            // Agar jika ada 2 notifikasi beruntun, notifikasi kedua tahu trial sudah berkurang
            if (!isPremium && currentRemaining > 0) {
                // PERBAIKAN: Gunakan 'remainingTrial' bukan 'remaining'
                userSession.savePremiumStatus(
                    isPremium = false,
                    remainingTrial = currentRemaining - 1
                )
            }
            // ---------------------------------------------------------------------------

            val cleanDisplay = finalText
                .replace(Regex("(?i)android\\.app\\.Notification\\$[a-zA-Z]+"), "")
                .replace("\n", " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(100)

            val trx = Transaction(
                sourceApp = pkgName,
                amount = amount,
                rawMessage = cleanDisplay,
                timestamp = System.currentTimeMillis(),
                isTrialLimited = isLimitReached // Status 'LOCKED' ditentukan di sini
            )

            val token = userSession.getToken()
            val uid = userSession.getUserId()

            scope.launch {
                if (token != null && uid != null) {
                    // Sync ke MySQL Backend (Tabel Transactions)
                    repository.insert(trx, token, uid)
                } else {
                    repository.insert(trx)
                }
            }

            // --- TTS SPEAK LOGIC ---
            if (canPlaySound) {
                val amountString = String.format(Locale("id", "ID"), "%.0f", amount)
                speak("Uang masuk, $amountString rupiah dari ${getSimpleAppName(pkgName)}")
            } else {
                Log.d("AKD_LISTENER", "Trial Habis & Belum Premium. Suara diblokir.")
            }
        }
    }

    private fun getSimpleAppName(pkg: String): String {
        return when {
            pkg.contains("dana") -> "Dana"
            pkg.contains("bca") -> "BCA"
            pkg.contains("mandiri") -> "Livin Mandiri"
            pkg.contains("gojek") -> "GoPay"
            else -> "Aplikasi Lain"
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
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Layanan Sound Horee Tetap Aktif",
                NotificationManager.IMPORTANCE_LOW
            )
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIF_ID, notification)
    }
}