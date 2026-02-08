package com.app.virtualsoundbox.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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

    // --- PERUBAHAN DISINI: Agar service tetap hidup (Sticky) ---
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        return START_STICKY
    }

    // --- PERUBAHAN DISINI: Memicu restart jika aplikasi di-swipe dari recents ---
    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext, 1, restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        // Opsional: Jika HP sangat agresif, bisa pakai AlarmManager di sini
        super.onTaskRemoved(rootIntent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val pkgName = sbn.packageName ?: ""

        if (!TARGET_APPS.any { pkgName.contains(it) }) return

        val extras = notification.extras
        val allTexts = mutableListOf<String>()

        sbn.tag?.let { allTexts.add(it) }

        for (key in extras.keySet()) {
            val value = extras.get(key)
            if (value is CharSequence || value is String) {
                allTexts.add(value.toString())
            }
        }

        val finalText = allTexts.joinToString(" ").trim()
        Log.d("AKD_LISTENER", "RAW TEXT FULL: $finalText")

        val amount = NotificationParser.extractAmount(finalText)

        if (amount > 0) {
            val cleanDisplay = finalText
                .replace(Regex("(?i)android\\.app\\.Notification\\$[a-zA-Z]+"), "")
                .replace(Regex("(?i)androidx\\.core\\.app\\.[a-zA-Z]+"), "")
                .replace("$", "")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(60)

            val trx = Transaction(
                sourceApp = pkgName,
                amount = amount,
                rawMessage = cleanDisplay
            )

            scope.launch { repository.saveTransaction(trx) }
            speak("Dana masuk, ${amount.toInt()} rupiah")
        }
    }

    private fun speak(message: String) {
        if (isTtsReady && tts != null) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "SoundHoreeID")
        } else {
            Log.e("AKD_LISTENER", "TTS belum siap untuk bicara.")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val localeId = Locale("id", "ID")
            val result = tts?.setLanguage(localeId)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                val altResult = tts?.setLanguage(Locale("in", "ID"))
                if (altResult == TextToSpeech.LANG_MISSING_DATA || altResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
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
            .setOngoing(true) // Notifikasi tidak bisa di-swipe
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIF_ID, notification)
    }
}