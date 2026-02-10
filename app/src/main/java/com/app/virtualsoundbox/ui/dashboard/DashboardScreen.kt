package com.app.virtualsoundbox.ui.dashboard

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.app.virtualsoundbox.data.repository.TransactionRepository
import com.app.virtualsoundbox.model.Transaction
import com.app.virtualsoundbox.model.UserProfile
import com.app.virtualsoundbox.utils.NotificationParser
import kotlinx.coroutines.Dispatchers
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

    // Setup DB & ViewModel
    val db = AppDatabase.getDatabase(context)
    val repository = TransactionRepository(db.transactionDao())
    val factory = DashboardViewModelFactory(repository)
    val viewModel: DashboardViewModel = viewModel(factory = factory)

    // States Data
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val filterLabel by viewModel.filterLabel.collectAsStateWithLifecycle()

    // State UI Lokal
    var selectedFilterIndex by remember { mutableStateOf(0) } // 0=Hari Ini, 1=Bulan Ini, 2=Custom

    // State Profil Toko
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Date Picker Dialog
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val selectedCal = Calendar.getInstance()
            selectedCal.set(year, month, dayOfMonth)
            viewModel.setFilterCustom(selectedCal.timeInMillis, null)
            selectedFilterIndex = 2 // Set aktif ke Custom
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

    // Load Profil
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val profiles = db.userProfileDao().getAllProfiles()
            if (profiles.isNotEmpty()) userProfile = profiles.last()
        }
    }

    val displayName = userProfile?.storeName ?: userName
    val displayCategory = userProfile?.category ?: "UMKM Indonesia"

    // Grouping Transaksi
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
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // 1. STATUS CARDS (DIKEMBALIKAN SESUAI PERMINTAAN)
            // Selalu tampil untuk memberi info apakah service aktif atau mati
            item {
                StatusCard(
                    isEnabled = isNotificationEnabled,
                    onClick = onOpenNotificationSettings
                )
                Spacer(modifier = Modifier.height(8.dp))

                BatteryOptimizationCard(onClick = onOptimizeBattery)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. FILTER CHIPS
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

            // 3. BIG BALANCE CARD
            item {
                TotalBalanceCard(totalAmount = totalIncome, label = "Total Masuk ($filterLabel)")
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 4. LIST TRANSAKSI
            if (groupedTransactions.isEmpty()) {
                item {
                    EmptyStateView(filterLabel)
                }
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

        if (showProfileDialog) {
            ProfileDetailDialog(userProfile, userName, { showProfileDialog = false }, { showProfileDialog = false; onLogout() })
        }
    }
}

// --- KOMPONEN UI ---

@Composable
fun StatusCard(isEnabled: Boolean, onClick: () -> Unit) {
    // Logic Warna: Hijau jika Aktif, Merah jika Mati
    val backgroundColor = if (isEnabled) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val contentColor = if (isEnabled) Color(0xFF2E7D32) else Color(0xFFC62828)
    val icon = if (isEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff

    Surface(
        onClick = onClick,
        color = backgroundColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEnabled) "Sound Horee Aktif" else "Izin Notifikasi Mati",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = if (isEnabled) "Siap mendeteksi uang masuk" else "Klik untuk mengaktifkan izin",
                    fontSize = 12.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
            // Indikator teks di sebelah kanan
            Text(
                text = if (isEnabled) "CEK" else "AKTIFKAN",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = contentColor
            )
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
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
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
            Text(
                text = formatRupiah(totalAmount),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun EmptyStateView(filterLabel: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.History, null, tint = Color.LightGray, modifier = Modifier.size(50.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Tidak ada data transaksi",
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Text(
            text = "Pada periode $filterLabel",
            fontSize = 12.sp,
            color = Color.LightGray
        )
    }
}

@Composable
fun BatteryOptimizationCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.White,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = MaterialTheme.shapes.large, color = Color.White, shadowElevation = 1.dp) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(Color(0xFFF1F8E9), CircleShape), contentAlignment = Alignment.Center) {
                Text(NotificationParser.getAppName(trx.sourceApp).take(1), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(NotificationParser.getAppName(trx.sourceApp), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(trx.rawMessage.take(35).let { if (it.length >= 35) "$it..." else it }, fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("+ ${formatRupiah(trx.amount)}", color = Color(0xFF2E7D32), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(trx.timestamp), fontSize = 11.sp, color = Color.LightGray)
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