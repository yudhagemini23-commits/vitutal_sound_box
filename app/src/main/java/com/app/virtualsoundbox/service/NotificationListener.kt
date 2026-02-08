package com.app.virtualsoundbox.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
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
        // Inisialisasi TTS dengan mesin Google jika tersedia agar bahasa Indonesia lebih akurat
        tts = TextToSpeech(this, this, "com.google.android.tts")
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
            // PEMBERSIHAN PESAN: Agar UI Dashboard tidak berantakan
            val cleanDisplay = finalText
                .replace(Regex("(?i)android\\.app\\.Notification\\$[a-zA-Z]+"), "")
                .replace(Regex("(?i)androidx\\.core\\.app\\.[a-zA-Z]+"), "")
                .replace("$", "")
                .replace(Regex("\\s+"), " ") // gabungkan spasi ganda
                .trim()
                .take(60) // Batasi 60 karakter agar pas di satu baris UI

            val trx = Transaction(
                sourceApp = pkgName,
                amount = amount,
                rawMessage = cleanDisplay
            )

            scope.launch { repository.saveTransaction(trx) }

            // Contoh suara: "Dana masuk, lima ratus rupiah"
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
            // 1. Coba paksa Bahasa Indonesia
            val localeId = Locale("id", "ID")
            val result = tts?.setLanguage(localeId)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // 2. Jika gagal, coba Locale Indonesia alternatif
                val altResult = tts?.setLanguage(Locale("in", "ID"))

                if (altResult == TextToSpeech.LANG_MISSING_DATA || altResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // 3. Jika benar-benar tidak ada data suara Indonesia, baru ke English
                    Log.e("AKD_LISTENER", "Indo Gagal, Fallback ke US English")
                    tts?.setLanguage(Locale.US)
                }
            }

            isTtsReady = true
            Log.d("AKD_LISTENER", "TTS Status: READY")
        } else {
            Log.e("AKD_LISTENER", "Inisialisasi TTS Gagal Total")
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}