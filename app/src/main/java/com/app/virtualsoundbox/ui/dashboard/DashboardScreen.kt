package com.app.virtualsoundbox.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
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
import com.app.virtualsoundbox.utils.NotificationParser
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    isNotificationEnabled: Boolean,      // Parameter 1: Status Izin
    onOpenNotificationSettings: () -> Unit, // Parameter 2: Aksi buka setting notif
    onOptimizeBattery: () -> Unit,          // Parameter 3: Aksi optimasi HP China
) {
    val context = LocalContext.current

    // Inisialisasi ViewModel & Repository
    val db = AppDatabase.getDatabase(context)
    val repository = TransactionRepository(db.transactionDao())
    val factory = DashboardViewModelFactory(repository)
    val viewModel: DashboardViewModel = viewModel(factory = factory)

    val totalToday by viewModel.totalToday.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    // Pengelompokan transaksi berdasarkan tanggal
    val groupedTransactions = transactions.groupBy { trx ->
        SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(trx.timestamp)
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // --- 1. CARD STATUS IZIN (DINAMIS) ---
                Surface(
                    onClick = onOpenNotificationSettings,
                    color = if (isNotificationEnabled) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (isNotificationEnabled) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isNotificationEnabled) "Layanan Sound Horee Aktif" else "Izin Notifikasi Belum Aktif",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isNotificationEnabled) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (isNotificationEnabled) "Cek" else "Aktifkan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNotificationEnabled) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- 2. CARD OPTIMASI BATERAI (AUTO-START) ---
                Surface(
                    onClick = onOptimizeBattery,
                    color = Color.White,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🚀 Optimalkan Performa (Auto-Start)",
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Halo, Sound Horee!", fontSize = 14.sp, color = Color.Gray)
                Text("Ringkasan Penjualan", fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // --- 3. TOTAL CARD ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("Total Masuk Hari Ini", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatRupiah(totalToday),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // --- 4. TRANSACTION LIST ---
            groupedTransactions.forEach { (date, trxs) ->
                item {
                    Text(
                        text = date,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                    )
                }

                items(trxs) { trx ->
                    TransactionItem(trx)
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun TransactionItem(trx: Transaction) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF1F8E9), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = NotificationParser.getAppName(trx.sourceApp).take(1),
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = NotificationParser.getAppName(trx.sourceApp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = trx.rawMessage.take(35).let { if (it.length >= 35) "$it..." else it },
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+ ${formatRupiah(trx.amount)}",
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(trx.timestamp),
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

fun formatRupiah(number: Double?): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(number ?: 0.0)
}