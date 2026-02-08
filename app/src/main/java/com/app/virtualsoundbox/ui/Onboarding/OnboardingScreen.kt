package com.app.virtualsoundbox.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onOpenNotifSettings: () -> Unit,
    onOptimizeBattery: () -> Unit
) {
    val pages = listOf(
        OnboardingData.Welcome,
        OnboardingData.NotifAccess,
        OnboardingData.ChinaPhone,
        OnboardingData.GojekQris
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                // Tombol Utama
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onFinished()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        if (pagerState.currentPage == pages.size - 1) "Mulai Berjualan Sekarang" else "Lanjut",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Indikator Titik
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) Color(0xFF2E7D32) else Color(0xFFE0E0E0)
                        Box(modifier = Modifier.padding(4.dp).size(8.dp).background(color, CircleShape))
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { index ->
            PageLayout(pages[index], onOpenNotifSettings, onOptimizeBattery)
        }
    }
}

@Composable
fun PageLayout(page: OnboardingData, onNotif: () -> Unit, onOptimize: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon Circle
        Surface(modifier = Modifier.size(100.dp), color = Color(0xFFF1F8E9), shape = CircleShape) {
            Box(contentAlignment = Alignment.Center) {
                Icon(page.icon, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(page.title, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(16.dp))

        Text(page.desc, fontSize = 16.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 24.sp)

        if (page is OnboardingData.NotifAccess || page is OnboardingData.ChinaPhone) {
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(
                onClick = { if (page is OnboardingData.NotifAccess) onNotif() else onOptimize() },
                border = BorderStroke(1.dp, Color(0xFF2E7D32))
            ) {
                Text("Klik Untuk Mengatur", color = Color(0xFF2E7D32))
            }
        }
    }
}

sealed class OnboardingData(val title: String, val desc: String, val icon: ImageVector) {
    object Welcome : OnboardingData(
        "Selamat Datang, Juragan!",
        "Sound Horee siap bantu bacakan nominal uang masuk secara otomatis. Fokus melayani pelanggan, biar kami yang urus suaranya.",
        Icons.Default.Star
    )
    object NotifAccess : OnboardingData(
        "Izin Akses Notifikasi",
        "Wajib Aktif! Agar aplikasi bisa mendeteksi uang masuk, silakan berikan izin 'Akses Notifikasi' pada sistem HP Anda.",
        Icons.Default.NotificationsActive
    )
    object ChinaPhone : OnboardingData(
        "Khusus HP Xiaomi/Oppo/Vivo",
        "Aktifkan 'Auto-Start' agar Sound Horee tetap bersuara kencang meskipun layar HP sedang terkunci atau mati.",
        Icons.Default.RocketLaunch
    )
    object GojekQris : OnboardingData(
        "Support QRIS Gojek",
        "Kini fitur deteksi transaksi Gojek sudah tersedia. Setiap notifikasi pembayaran Gojek akan otomatis dibacakan.",
        Icons.Default.Security
    )
}