package com.app.virtualsoundbox.ui.dashboard

import android.app.DatePickerDialog
import android.content.Context
import android.util.Log
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.virtualsoundbox.data.local.AppDatabase
import com.app.virtualsoundbox.data.remote.RetrofitClient
import com.app.virtualsoundbox.data.repository.TransactionRepository
import com.app.virtualsoundbox.model.Transaction
import com.app.virtualsoundbox.model.UserProfile
import com.app.virtualsoundbox.ui.profile.ProfileViewModel
import com.app.virtualsoundbox.ui.profile.SetupState
import com.app.virtualsoundbox.ui.subscription.SubscriptionDialog
import com.app.virtualsoundbox.utils.NotificationParser
import com.app.virtualsoundbox.utils.UserSession
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userName: String,
    isNotificationEnabled: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onOptimizeBattery: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val userSession = remember { UserSession(context) }

    // --- SETUP DB & VIEWMODEL ---
    val db = AppDatabase.getDatabase(context)
    val repository = TransactionRepository(db.transactionDao())
    val factory = DashboardViewModelFactory(repository)
    val viewModel: DashboardViewModel = viewModel(factory = factory)
    val profileViewModel: ProfileViewModel = viewModel() // Untuk upgrade premium

    // --- STATES DATA ---
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val filterLabel by viewModel.filterLabel.collectAsStateWithLifecycle()
    val setupState by profileViewModel.setupState.collectAsStateWithLifecycle()

    // --- PERBAIKAN: DATA SUBSCRIPTION DARI USER SESSION (BACKEND SOURCE) ---
    var isPremium by remember { mutableStateOf(userSession.isPremium()) }
    var remainingTrial by remember { mutableStateOf(userSession.getRemainingTrial()) }
    var showSubscriptionPopup by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope() // Tambahkan scope untuk API call
    var mockTapCount by remember { mutableIntStateOf(0) }

    // Pantau perubahan dari server (SetupState)
    LaunchedEffect(setupState) {
        if (setupState is SetupState.Success) {
            isPremium = userSession.isPremium()
            remainingTrial = userSession.getRemainingTrial()
            showSubscriptionPopup = false
        }
    }

    // Logic Popup Otomatis: Jika bukan premium & trial habis
    LaunchedEffect(remainingTrial, isPremium) {
        if (!isPremium && remainingTrial <= 0) {
            showSubscriptionPopup = true
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getNotificationRules()
                if (response.isSuccessful && response.body() != null) {
                    val rulesJson = Gson().toJson(response.body())
                    userSession.saveNotificationRules(rulesJson)
                    Log.d("AKD_RULES", "Foreground Sync: Rules Updated!")
                }
            } catch (e: Exception) {
                Log.e("AKD_RULES", "Gagal update rules saat buka app: ${e.message}")
            }
        }
    }

    // --- STATE UI LOKAL ---
    var selectedFilterIndex by remember { mutableStateOf(0) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // --- DATE PICKER ---
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val selectedCal = Calendar.getInstance()
            selectedCal.set(year, month, dayOfMonth)
            viewModel.setFilterCustom(selectedCal.timeInMillis, null)
            selectedFilterIndex = 2
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // --- LOAD PROFIL ---
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val profiles = db.userProfileDao().getAllProfiles()
            if (profiles.isNotEmpty()) userProfile = profiles.last()
        }
    }

    val displayName = userProfile?.storeName ?: userName
    val displayCategory = userProfile?.category ?: "UMKM Indonesia"
    val groupedTransactions = transactions.groupBy { trx ->
        SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(trx.timestamp)
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.clickable {
                            mockTapCount++
                            if (mockTapCount >= 5) {
                                mockTapCount = 0
                                scope.launch {
                                    performMockNotification(context, repository, userSession)
                                }
                            }
                        }
                    ) {
                        Text(displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(displayCategory, fontSize = 12.sp, color = Color.Gray)
                    }
                },
                actions = {
                    if (isPremium) {
                        Icon(Icons.Default.WorkspacePremium, null, tint = Color(0xFFFFD700), modifier = Modifier.padding(end = 8.dp))
                    }
                    IconButton(onClick = { showProfileDialog = true }) {
                        Surface(shape = CircleShape, color = Color(0xFFE8F5E9), modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Store, "Profile", tint = Color(0xFF2E7D32), modifier = Modifier.padding(8.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            if (!isPremium) {
                item {
                    PremiumBanner(remainingTrial = remainingTrial, onClick = { showSubscriptionPopup = true })
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                StatusCard(isEnabled = isNotificationEnabled, onClick = onOpenNotificationSettings)
                Spacer(modifier = Modifier.height(8.dp))
                BatteryOptimizationCard(onClick = onOptimizeBattery)
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChipUI("Hari Ini", selectedFilterIndex == 0) { selectedFilterIndex = 0; viewModel.setFilterToday() }
                    FilterChipUI("Bulan Ini", selectedFilterIndex == 1) { selectedFilterIndex = 1; viewModel.setFilterThisMonth() }
                    FilterChipUI("📅 Pilih Tanggal", selectedFilterIndex == 2) { datePickerDialog.show() }
                }
            }

            item {
                TotalBalanceCard(totalAmount = totalIncome, label = "Total Masuk ($filterLabel)")
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (groupedTransactions.isEmpty()) {
                item { EmptyStateView(filterLabel) }
            } else {
                groupedTransactions.forEach { (date, trxs) ->
                    item { Text(text = date, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)) }
                    items(trxs) { trx -> TransactionItem(trx) }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        if (showProfileDialog) {
            ProfileDetailDialog(userProfile, userName, { showProfileDialog = false }, { showProfileDialog = false; onLogout() })
        }

        if (showSubscriptionPopup) {
            SubscriptionDialog(
                remainingTrial = remainingTrial,
                onDismiss = { if (remainingTrial > 0) showSubscriptionPopup = false },
                onSubscribeSuccess = { plan ->
                    // HIT API BACKEND UNTUK UPGRADE
//                    profileViewModel.upgradePremium(plan, userSession.getUserId() ?: "")
                    val uid = userSession.getUserId() ?: ""
                    userSession.savePremiumStatus(isPremium = true, remainingTrial = 0)
                    profileViewModel.upgradePremium(plan, uid)
                    viewModel.unlockHistory()
                }
            )
        }
    }
}

private suspend fun performMockNotification(
    context: Context,
    repository: TransactionRepository, // Kita butuh ini untuk insert ke Room
    userSession: UserSession
) {
    withContext(Dispatchers.IO) {
        try {
            val nominal = 50000.0
            val appName = "com.dana.id" // Kita pura-pura ini dari DANA
            val message = "Berhasil terima uang Rp 50.000 dari Penguji Google"

            // 1. Masukkan ke Database Lokal (Room)
            // Ini yang membuat data langsung muncul di LazyColumn Mas
            val mockTrx = com.app.virtualsoundbox.model.Transaction(
                id = 0,
                amount = nominal,
                sourceApp = appName,
                rawMessage = message,
                timestamp = System.currentTimeMillis(),
                isTrialLimited = false
            )
            repository.insert(mockTrx)

            // 2. Trigger Suara (Text-to-Speech)
            // Kita inisialisasi TTS lokal untuk keperluan testing reviewer
            val tts = android.speech.tts.TextToSpeech(context) { status -> }
            delay(500) // Beri waktu inisialisasi
            tts.setLanguage(Locale("id", "ID"))
            tts.speak(
                "Ada uang masuk sebesar lima puluh ribu rupiah dari dana",
                android.speech.tts.TextToSpeech.QUEUE_FLUSH,
                null,
                "MOCK_ID"
            )

            // 3. Kirim ke Backend (Sync)
            // Sesuaikan endpoint-nya dengan fungsi simpan transaksi Mas
            try {
                val req = com.app.virtualsoundbox.data.remote.model.LoginRequest(
                    uid = userSession.getUserId() ?: "",
                    email = userSession.getUserEmail() ?: "",
                    storeName = "MOCK_DANA",
                    phoneNumber = nominal.toString(),
                    category = message
                )
                RetrofitClient.instance.loginUser(req)
            } catch (e: Exception) {
                Log.e("AKD_MOCK", "Gagal sync backend: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Simulasi Berhasil: Check List Transaksi!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Simulasi Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// Komponen Pendukung
@Composable
fun PremiumBanner(remainingTrial: Int, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)), border = BorderStroke(1.dp, Color(0xFFFFD54F)), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(Color(0xFFFFB300), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Star, null, tint = Color.White) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Mode Gratis Terbatas", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE65100))
                if (remainingTrial > 0) Text("Sisa ${remainingTrial}x Transaksi Suara.", fontSize = 12.sp, color = Color(0xFFEF6C00))
                else Text("Kuota Habis! Upgrade Premium sekarang.", fontSize = 12.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFFE65100))
        }
    }
}

@Composable
fun StatusCard(isEnabled: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isEnabled) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val contentColor = if (isEnabled) Color(0xFF2E7D32) else Color(0xFFC62828)
    Surface(onClick = onClick, color = backgroundColor, shape = MaterialTheme.shapes.medium) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = if (isEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if (isEnabled) "Sound Horee Aktif" else "Izin Notifikasi Mati", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = contentColor)
                Text(if (isEnabled) "Siap mendeteksi uang masuk" else "Klik untuk mengaktifkan izin", fontSize = 12.sp, color = contentColor.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun FilterChipUI(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, color = if (isSelected) Color(0xFF2E7D32) else Color.White, border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE0E0E0)), shape = RoundedCornerShape(20.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun TotalBalanceCard(totalAmount: Double?, label: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32))) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            Text(formatRupiah(totalAmount), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun TransactionItem(trx: Transaction) {
    val alpha = if (trx.isTrialLimited) 0.4f else 1.0f
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = MaterialTheme.shapes.large, color = if (trx.isTrialLimited) Color(0xFFEEEEEE) else Color.White) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(if (trx.isTrialLimited) Color.LightGray else Color(0xFFF1F8E9), CircleShape), contentAlignment = Alignment.Center) {
                Text(NotificationParser.getAppName(trx.sourceApp).take(1), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(NotificationParser.getAppName(trx.sourceApp), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black.copy(alpha = alpha))
                    if (trx.isTrialLimited) {
                        Spacer(Modifier.width(8.dp))
                        Surface(color = Color.Gray, shape = RoundedCornerShape(4.dp)) { Text("LOCKED", fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(2.dp)) }
                    }
                }
                Text(if (trx.isTrialLimited) "Suara dimatikan (Trial Habis)" else trx.rawMessage.take(35), fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("+ ${formatRupiah(trx.amount)}", color = if (trx.isTrialLimited) Color.Gray else Color(0xFF2E7D32), fontWeight = FontWeight.ExtraBold)
                Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(trx.timestamp), fontSize = 11.sp, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun EmptyStateView(label: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.History, null, tint = Color.LightGray, modifier = Modifier.size(50.dp))
        Text("Tidak ada transaksi periode $label", color = Color.Gray)
    }
}

@Composable
fun BatteryOptimizationCard(onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.White, shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, Color(0xFFEEEEEE))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Icon(Icons.Default.Settings, null, tint = Color.Gray)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Optimalkan Performa", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Aktifkan auto-start agar suara lancar", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun ProfileDetailDialog(profile: UserProfile?, defaultName: String, onDismiss: () -> Unit, onLogout: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profil Toko") },
        text = { Column { ProfileRow(Icons.Default.Store, "Toko", profile?.storeName ?: defaultName); ProfileRow(Icons.Default.Phone, "WhatsApp", profile?.phoneNumber ?: "-") } },
        confirmButton = { Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color(0xFFC62828))) { Text("Keluar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}

@Composable
fun ProfileRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.padding(vertical = 4.dp)) { Icon(icon, null, Modifier.size(16.dp), tint = Color.Gray); Spacer(Modifier.width(8.dp)); Column { Text(label, fontSize = 10.sp, color = Color.Gray); Text(value) } }
}

fun formatRupiah(number: Double?): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(number ?: 0.0).replace("Rp", "Rp ")
}