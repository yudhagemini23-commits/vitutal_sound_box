package com.app.virtualsoundbox.ui.dashboard

import android.app.DatePickerDialog
import android.content.Context
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.virtualsoundbox.data.local.AppDatabase
import com.app.virtualsoundbox.data.repository.TransactionRepository
import com.app.virtualsoundbox.model.Transaction
import com.app.virtualsoundbox.model.UserProfile
import com.app.virtualsoundbox.utils.NotificationParser
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
    val sharedPref = remember { context.getSharedPreferences("SoundHoreePrefs", Context.MODE_PRIVATE) }

    // --- SETUP DB & VIEWMODEL ---
    val db = AppDatabase.getDatabase(context)
    val repository = TransactionRepository(db.transactionDao())
    val factory = DashboardViewModelFactory(repository)
    val viewModel: DashboardViewModel = viewModel(factory = factory)

    // --- STATES DATA ---
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val filterLabel by viewModel.filterLabel.collectAsStateWithLifecycle()

    // --- STATE SUBSCRIPTION & TRIAL ---
    var isPremium by remember { mutableStateOf(sharedPref.getBoolean("isPremium", false)) }

    // Mengambil jumlah transaksi total untuk hitungan trial (Disimpan di Prefs oleh Service)
    // Default 0 jika baru instal
    val transactionCount = sharedPref.getInt("trxCount", 0)
    val maxTrial = 3
    val remainingTrial = (maxTrial - transactionCount).coerceAtLeast(0)

    var showSubscriptionPopup by remember { mutableStateOf(false) }

    // Logic Popup Otomatis: Jika bukan premium & trial habis, paksa muncul
    LaunchedEffect(transactionCount, isPremium) {
        if (!isPremium && remainingTrial == 0) {
            showSubscriptionPopup = true
        }
    }

    // --- STATE UI LOKAL ---
    var selectedFilterIndex by remember { mutableStateOf(0) } // 0=Hari Ini, 1=Bulan Ini, 2=Custom
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
    datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

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
                    Column {
                        Text(displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(displayCategory, fontSize = 12.sp, color = Color.Gray)
                    }
                },
                actions = {
                    // Tombol PRO (Jika Premium)
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // 1. BANNER PREMIUM (JIKA BELUM SUBSCRIBE)
            if (!isPremium) {
                item {
                    PremiumBanner(
                        remainingTrial = remainingTrial,
                        onClick = { showSubscriptionPopup = true }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 2. STATUS CARDS
            item {
                StatusCard(isEnabled = isNotificationEnabled, onClick = onOpenNotificationSettings)
                Spacer(modifier = Modifier.height(8.dp))
                BatteryOptimizationCard(onClick = onOptimizeBattery)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 3. FILTER CHIPS
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipUI("Hari Ini", selectedFilterIndex == 0) {
                        selectedFilterIndex = 0
                        viewModel.setFilterToday()
                    }
                    FilterChipUI("Bulan Ini", selectedFilterIndex == 1) {
                        selectedFilterIndex = 1
                        viewModel.setFilterThisMonth()
                    }
                    FilterChipUI("📅 Pilih Tanggal", selectedFilterIndex == 2) {
                        datePickerDialog.show()
                    }
                }
            }

            // 4. BIG BALANCE CARD
            item {
                TotalBalanceCard(totalAmount = totalIncome, label = "Total Masuk ($filterLabel)")
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 5. LIST TRANSAKSI
            if (groupedTransactions.isEmpty()) {
                item { EmptyStateView(filterLabel) }
            } else {
                groupedTransactions.forEach { (date, trxs) ->
                    item {
                        Text(
                            text = date,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        )
                    }
                    items(trxs) { trx -> TransactionItem(trx) }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // --- POPUP DIALOGS ---

        // 1. Profil Dialog
        if (showProfileDialog) {
            ProfileDetailDialog(userProfile, userName, { showProfileDialog = false }, { showProfileDialog = false; onLogout() })
        }

        // 2. Subscription Dialog (Popup Penawaran)
        if (showSubscriptionPopup) {
            SubscriptionDialog(
                remainingTrial = remainingTrial,
                onDismiss = {
                    // Jika trial habis, tidak bisa ditutup (harus subscribe)
                    if (remainingTrial > 0) showSubscriptionPopup = false
                },
                onSubscribeSuccess = { planName ->
                    sharedPref.edit().putBoolean("isPremium", true).apply()
                    isPremium = true
                    showSubscriptionPopup = false
                    Toast.makeText(context, "Selamat! Anda berlangganan paket $planName", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}

// ==========================================
// KUMPULAN KOMPONEN UI & DIALOG
// ==========================================

@Composable
fun PremiumBanner(remainingTrial: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)), // Kuning Soft
        border = BorderStroke(1.dp, Color(0xFFFFD54F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFFFB300), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Mode Gratis Terbatas", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE65100))

                if (remainingTrial > 0) {
                    // PERBAIKAN DI SINI: Pakai kurung kurawal ${remainingTrial}x
                    Text("Sisa ${remainingTrial}x Transaksi Suara.", fontSize = 12.sp, color = Color(0xFFEF6C00))
                } else {
                    Text("Kuota Habis! Upgrade Premium sekarang.", fontSize = 12.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFFE65100))
        }
    }
}

@Composable
fun SubscriptionDialog(
    remainingTrial: Int,
    onDismiss: () -> Unit,
    onSubscribeSuccess: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedPlan by remember { mutableStateOf(1) } // 0=Weekly, 1=Monthly
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = (remainingTrial > 0), dismissOnClickOutside = (remainingTrial > 0))
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA000))), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Upgrade ke Premium", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))

                if (remainingTrial > 0) {
                    Text("Sisa Trial: $remainingTrial Transaksi", fontSize = 14.sp, color = Color.Gray)
                } else {
                    Text("Trial Habis! Suara dimatikan sementara.", fontSize = 14.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Paket Mingguan
                PlanCard("Mingguan", "Rp 5.000", "/minggu", selectedPlan == 0, false) { selectedPlan = 0 }
                Spacer(modifier = Modifier.height(12.dp))
                // Paket Bulanan
                PlanCard("Bulanan (Hemat 50%)", "Rp 10.000", "/bulan", selectedPlan == 1, true) { selectedPlan = 1 }

                Spacer(modifier = Modifier.height(24.dp))
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    BenefitItem("Notifikasi Suara Tanpa Batas")
                    BenefitItem("Support Semua QRIS & Bank")
                }
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            delay(2000)
                            isProcessing = false
                            onSubscribeSuccess(if (selectedPlan == 0) "Mingguan" else "Bulanan")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedPlan == 1) Color(0xFF2E7D32) else Color(0xFF1B5E20)),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text(if (remainingTrial > 0) "Langganan Sekarang" else "Buka Kunci Premium", fontWeight = FontWeight.Bold)
                }

                if (remainingTrial > 0) {
                    TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Nanti Saja", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PlanCard(title: String, price: String, period: String, isSelected: Boolean, isBestValue: Boolean, onClick: () -> Unit) {
    val borderColor by animateColorAsState(if (isSelected) Color(0xFF2E7D32) else Color(0xFFE0E0E0), label = "")
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val backgroundColor = if (isSelected) Color(0xFFF1F8E9) else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Circle,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF2E7D32) else Color.LightGray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(price, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF2E7D32))
                    Text(period, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
                }
            }
            if (isBestValue) {
                Surface(color = Color(0xFFFFD700), shape = RoundedCornerShape(8.dp)) {
                    Text("HEMAT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun BenefitItem(text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, color = Color.Gray)
    }
}

// --- KOMPONEN LAMA (TIDAK BERUBAH) ---

@Composable
fun StatusCard(isEnabled: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isEnabled) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val contentColor = if (isEnabled) Color(0xFF2E7D32) else Color(0xFFC62828)
    val icon = if (isEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff

    Surface(onClick = onClick, color = backgroundColor, shape = MaterialTheme.shapes.medium) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if (isEnabled) "Sound Horee Aktif" else "Izin Notifikasi Mati", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = contentColor)
                Text(if (isEnabled) "Siap mendeteksi uang masuk" else "Klik untuk mengaktifkan izin", fontSize = 12.sp, color = contentColor.copy(alpha = 0.8f))
            }
            Text(if (isEnabled) "CEK" else "AKTIFKAN", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = contentColor)
        }
    }
}

@Composable
fun FilterChipUI(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF2E7D32) else Color.White,
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE0E0E0)),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = if (isSelected) Color.White else Color.Gray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
    }
}

@Composable
fun TotalBalanceCard(totalAmount: Double?, label: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(formatRupiah(totalAmount), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun EmptyStateView(filterLabel: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.History, null, tint = Color.LightGray, modifier = Modifier.size(50.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Tidak ada data transaksi", fontWeight = FontWeight.Bold, color = Color.Gray)
        Text("Pada periode $filterLabel", fontSize = 12.sp, color = Color.LightGray)
    }
}

@Composable
fun BatteryOptimizationCard(onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.White, shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, Color(0xFFEEEEEE))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Optimalkan Performa", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                Text("Aktifkan auto-start agar suara lancar", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun TransactionItem(trx: Transaction) {
    // Tentukan Gaya Tampilan (Normal vs Limited)
    val cardColor = if (trx.isTrialLimited) Color(0xFFEEEEEE) else Color.White
    val contentAlpha = if (trx.isTrialLimited) 0.4f else 1.0f // Transparan jika limited
    val textColor = if (trx.isTrialLimited) Color.Gray else Color.Black
    val amountColor = if (trx.isTrialLimited) Color.Gray else Color(0xFF2E7D32)

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
        color = cardColor,
        shadowElevation = if (trx.isTrialLimited) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (trx.isTrialLimited) Color.LightGray else Color(0xFFF1F8E9),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = NotificationParser.getAppName(trx.sourceApp).take(1),
                    color = if (trx.isTrialLimited) Color.DarkGray else Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Tengah
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        NotificationParser.getAppName(trx.sourceApp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textColor.copy(alpha = contentAlpha)
                    )

                    // LABEL PREMIUM ONLY
                    if (trx.isTrialLimited) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = Color.Gray, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                "LOCKED",
                                fontSize = 10.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = if (trx.isTrialLimited) "Suara dimatikan (Trial Habis)" else trx.rawMessage.take(35) + "...",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Nominal & Jam
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "+ ${formatRupiah(trx.amount)}",
                    color = amountColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(trx.timestamp),
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
                // Countdown text (Opsional)
                if (trx.isTrialLimited) {
                    Text("Hilang dlm 30m", fontSize = 10.sp, color = Color.Red.copy(alpha=0.6f))
                }
            }
        }
    }
}

@Composable
fun ProfileDetailDialog(profile: UserProfile?, defaultName: String, onDismiss: () -> Unit, onLogout: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Profil Toko", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                ProfileRow(Icons.Default.Store, "Nama Toko", profile?.storeName ?: defaultName)
                ProfileRow(Icons.Default.Phone, "WhatsApp", profile?.phoneNumber ?: "-")
            }
        },
        confirmButton = { Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color(0xFFC62828))) { Text("Keluar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}

@Composable
fun ProfileRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column { Text(label, fontSize = 10.sp, color = Color.Gray); Text(value, fontSize = 14.sp) }
    }
}

fun formatRupiah(number: Double?): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(number ?: 0.0).replace("Rp", "Rp ")
}