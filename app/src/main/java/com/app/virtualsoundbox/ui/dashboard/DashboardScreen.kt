package com.app.virtualsoundbox.ui.dashboard

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
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
import com.app.virtualsoundbox.MainActivity
import com.app.virtualsoundbox.data.local.AppDatabase
import com.app.virtualsoundbox.data.repository.TransactionRepository
import com.app.virtualsoundbox.model.Transaction
import com.app.virtualsoundbox.utils.NotificationParser

import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val repository = TransactionRepository(db.transactionDao())
    val factory = DashboardViewModelFactory(repository)
    val viewModel: DashboardViewModel = viewModel(factory = factory)

    val totalToday by viewModel.totalToday.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    val groupedTransactions = transactions.groupBy { trx ->
        java.text.SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(trx.timestamp)
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            // Tambahkan statusBarsPadding() agar tidak menabrak bagian atas HP
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Surface(
                    onClick = {
                        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                        context.startActivity(intent)
                    },
                    color = Color(0xFFFFEBEE),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️ Status Izin Notifikasi", fontSize = 12.sp, color = Color(0xFFC62828), modifier = Modifier.weight(1f))
                        Text("Cek", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
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
            // Icon Placeholder (Bisa Mas Yudha ganti logo Bank nantinya)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF1F8E9), shape = androidx.compose.foundation.shape.CircleShape),
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
                // Jam Transaksi
                Text(
                    text = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(trx.timestamp),
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