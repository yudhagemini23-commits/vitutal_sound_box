package com.app.virtualsoundbox.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.virtualsoundbox.data.local.AppDatabase
import com.app.virtualsoundbox.data.repository.TransactionRepository
import com.app.virtualsoundbox.model.Transaction
import com.app.virtualsoundbox.utils.DateFilter
import com.app.virtualsoundbox.utils.NotificationParser
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userName: String,
    isNotificationEnabled: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onOptimizeBattery: () -> Unit,
    onLogout: () -> Unit // Parameter Baru: Aksi Logout/Ganti Nama
) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val repository = TransactionRepository(db.transactionDao())
    val factory = DashboardViewModelFactory(repository)
    val viewModel: DashboardViewModel = viewModel(factory = factory)

    // Collect States
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val totalAmount by viewModel.totalAmount.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val appStats by viewModel.appStats.collectAsStateWithLifecycle()

    // State untuk Dialog Profil
    var showProfileDialog by remember { mutableStateOf(false) }

    // --- DIALOG PROFIL ---
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF2E7D32)
                )
            },
            title = {
                Text("Profil Juragan", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(userName, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Pemilik Toko / Admin", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Sound Horee v1.0",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showProfileDialog = false
                        onLogout() // Panggil aksi logout
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Ganti Nama / Keluar")
                }
            },
            containerColor = Color.White
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                // Header Nama & Profile
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Halo, $userName!", fontSize = 14.sp, color = Color.Gray)
                        Text("Ringkasan", fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                    // TOMBOL PROFILE (Ganti dari Settings)
                    IconButton(onClick = { showProfileDialog = true }) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Profil",
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFF2E7D32)
                        )
                    }
                }

                // Filter Tanggal
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateFilter.values().forEach { filter ->
                        FilterChip(
                            selected = (filter == selectedFilter),
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE8F5E9),
                                selectedLabelColor = Color(0xFF2E7D32)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = (filter == selectedFilter),
                                borderColor = if (filter == selectedFilter) Color(0xFF2E7D32) else Color.LightGray
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {

            // --- BAGIAN 1: STATUS PERMISSION ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PermissionStatusCard(
                        isActive = isNotificationEnabled,
                        activeText = "Layanan Suara Aktif",
                        inactiveText = "Izin Notifikasi Mati",
                        activeIcon = Icons.Default.CheckCircle,
                        inactiveIcon = Icons.Default.NotificationsActive,
                        onClick = onOpenNotificationSettings
                    )

                    Surface(
                        onClick = onOptimizeBattery,
                        color = Color.White,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color(0xFFFFA000))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Optimalkan Performa", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Klik jika suara sering telat/mati (Xiaomi/Oppo)", fontSize = 11.sp, color = Color.Gray)
                            }
                            // Icon panah kecil
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- BAGIAN 2: CARD TOTAL ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Total ${selectedFilter.label}", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(formatRupiah(totalAmount), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // --- BAGIAN 3: PIE CHART ---
            item {
                if (appStats.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        SimplePieChart(appStats)
                    }
                }
            }

            // --- BAGIAN 4: LIST TRANSAKSI ---
            if (transactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada transaksi masuk", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                items(
                    items = transactions,
                    key = { it.id }
                ) { trx ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.deleteTransaction(trx)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFEF5350), MaterialTheme.shapes.medium)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.White)
                            }
                        },
                        content = {
                            TransactionItem(trx)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            item { Spacer(modifier = Modifier.height(50.dp)) }
        }
    }
}

// --- KOMPONEN HELPER (Sama seperti sebelumnya) ---

@Composable
fun PermissionStatusCard(
    isActive: Boolean,
    activeText: String,
    inactiveText: String,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, if (isActive) Color(0xFFA5D6A7) else Color(0xFFFFCDD2)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isActive) activeIcon else inactiveIcon,
                contentDescription = null,
                tint = if (isActive) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isActive) activeText else inactiveText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isActive) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                if (!isActive) {
                    Text("Klik untuk mengaktifkan sekarang!", fontSize = 11.sp, color = Color(0xFFC62828))
                }
            }
            if (!isActive) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun SimplePieChart(stats: Map<String, Float>) {
    val colors = listOf(Color(0xFF2E7D32), Color(0xFFFFA000), Color(0xFF1976D2), Color(0xFFC62828), Color.Gray)

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Komposisi Sumber Dana", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(100.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    stats.entries.forEachIndexed { index, entry ->
                        val sweepAngle = (entry.value / 100f) * 360f
                        drawArc(
                            color = colors[index % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true
                        )
                        startAngle += sweepAngle
                    }
                }
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                stats.entries.forEachIndexed { index, entry ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                        Box(modifier = Modifier.size(12.dp).background(colors[index % colors.size], CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${entry.key}: ${"%.1f".format(entry.value)}%", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(trx: Transaction) {
    Surface(
        shape = MaterialTheme.shapes.medium,
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
                Text(NotificationParser.getAppName(trx.sourceApp), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(trx.timestamp),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = "+ ${formatRupiah(trx.amount)}",
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

fun formatRupiah(number: Double?): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(number ?: 0.0)
}