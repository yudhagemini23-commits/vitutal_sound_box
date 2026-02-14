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
        val pkgName = sbn.packageName ?: "" // Perbaikan: Handle null safety

        // Cek apakah paket aplikasi ada di daftar target
        // (Logic Mas sebelumnya pakai .any { contains } sudah oke, tapi ini lebih aman)
        val isTarget = TARGET_APPS.any { pkgName.contains(it, ignoreCase = true) }
        if (!isTarget) return

        val extras = notification.extras
        val allTexts = mutableListOf<String>()

        // Ambil Title & Text
        extras.getCharSequence("android.title")?.let { allTexts.add(it.toString()) }
        extras.getCharSequence("android.text")?.let { allTexts.add(it.toString()) }
        extras.getCharSequence("android.bigText")?.let { allTexts.add(it.toString()) }

        // Fallback ke strategi lama Mas jika null
        if (allTexts.isEmpty()) {
            for (key in extras.keySet()) {
                val value = extras.get(key)
                if (value is CharSequence || value is String) {
                    allTexts.add(value.toString())
                }
            }
        }

        val finalText = allTexts.joinToString(" ").trim()
        Log.d("AKD_LISTENER", "RAW TEXT FULL: $finalText")

        // 1. Parse Amount
        val amount = NotificationParser.extractAmount(finalText) // Pastikan return Double

        if (amount > 0) { // Hanya proses jika ada angka uang valid

            // --- LOGIC SUBSCRIPTION (MASIH SAMA) ---
            val sharedPref = applicationContext.getSharedPreferences("SoundHoreePrefs", Context.MODE_PRIVATE)
            val isPremium = sharedPref.getBoolean("isPremium", false)
            val trxCount = sharedPref.getInt("trxCount", 0)
            val isLimitReached = !isPremium && trxCount >= 3
            sharedPref.edit().putInt("trxCount", trxCount + 1).apply()
            // ---------------------------------------

            val cleanDisplay = finalText
                .replace(Regex("(?i)android\\.app\\.Notification\\$[a-zA-Z]+"), "") // Hapus sampah sistem
                .replace("\n", " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(100) // Ambil 100 karakter saja biar DB gak penuh

            // 2. Buat Object Transaksi
            val trx = Transaction(
                sourceApp = pkgName,
                amount = amount, // Pastikan tipe data Double
                rawMessage = cleanDisplay,
                timestamp = System.currentTimeMillis(),
                isTrialLimited = isLimitReached
            )

            // --- [PERUBAHAN PENTING: HARDCODE TOKEN DULU] ---
            // Ambil Token & UID ini dari hasil Login di Terminal / Postman tadi
            // Nanti kalau Login Screen sudah jadi, ganti ini pakai UserSession.getToken()

            val userSession = com.app.virtualsoundbox.utils.UserSession(applicationContext)
            val token = userSession.getToken()
            val uid = userSession.getUserId()
            // ------------------------------------------------

            // 3. Simpan & Sync
            scope.launch {
                if (token != null && uid != null) {
                    Log.d("AKD_LISTENER", "User Login ($uid). Sync ke Server...")
                    repository.insert(trx, token, uid)
                } else {
                    Log.w("AKD_LISTENER", "User Belum Login / Token Null. Simpan lokal saja.")
                    repository.insert(trx) // Parameter token & uid default-nya null (lihat Repository)
                }
            }

            // 4. TTS Speak
            if (!isLimitReached) {
                // Tips: Ubah format double jadi string rapi (tanpa koma desimal .0)
                val amountString = String.format(Locale("id", "ID"), "%.0f", amount)
                speak("Uang masuk, $amountString rupiah dari ${getSimpleAppName(pkgName)}")
            } else {
                Log.d("AKD_LISTENER", "Trial Limit Reached. Suara dimatikan.")
            }
        }
    }

    // Helper biar TTS ngomongnya enak (bukan com.package.name)
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