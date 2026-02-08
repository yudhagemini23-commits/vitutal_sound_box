package com.app.virtualsoundbox.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
    // Parameter userName SAYA HAPUS karena user belum login
    isNotificationEnabled: Boolean,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indikator Titik
                Row(
                    modifier = Modifier.padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) Color(0xFF2E7D32) else Color(0xFFE0E0E0)
                        Box(modifier = Modifier.padding(4.dp).size(8.dp).background(color, CircleShape))
                    }
                }

                // Tombol Navigasi
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
                        text = if (pagerState.currentPage == pages.size - 1) "Lanjut ke Login" else "Lanjut",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { index ->
            OnboardingPageLayout(
                page = pages[index],
                isNotifEnabled = isNotificationEnabled,
                onNotif = onOpenNotifSettings,
                onOptimize = onOptimizeBattery
            )
        }
    }
}

@Composable
fun OnboardingPageLayout(
    page: OnboardingData,
    isNotifEnabled: Boolean,
    onNotif: () -> Unit,
    onOptimize: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon Circle
        Surface(modifier = Modifier.size(120.dp), color = Color(0xFFF1F8E9), shape = CircleShape) {
            Box(contentAlignment = Alignment.Center) {
                Icon(page.icon, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(56.dp))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(page.title, fontSize = 26.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 32.sp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(page.desc, fontSize = 15.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 24.sp)

        if (page is OnboardingData.NotifAccess || page is OnboardingData.ChinaPhone) {
            Spacer(modifier = Modifier.height(32.dp))

            val isDone = (page is OnboardingData.NotifAccess && isNotifEnabled)

            OutlinedButton(
                onClick = { if (!isDone) { if (page is OnboardingData.NotifAccess) onNotif() else onOptimize() } },
                border = BorderStroke(1.dp, if (isDone) Color(0xFF2E7D32) else Color(0xFF2E7D32))
            ) {
                if (isDone) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Izin Sudah Aktif")
                } else {
                    Text("Klik Untuk Mengatur", color = Color(0xFF2E7D32))
                }
            }
        }
    }
}

sealed class OnboardingData(val title: String, val desc: String, val icon: ImageVector) {
    // Sapaan Generic untuk User Baru
    object Welcome : OnboardingData(
        "Halo",
        "Sound Horee siap bantu bacakan nominal uang masuk secara otomatis. Fokus melayani pelanggan, biar kami yang urus suaranya.",
        Icons.Default.Star
    )
    object NotifAccess : OnboardingData(
        "Izin Akses Notifikasi",
        "Wajib Aktif! Agar aplikasi bisa mendeteksi uang masuk, silakan berikan izin 'Akses Notifikasi' pada sistem HP Anda.",
        Icons.Default.NotificationsActive
    )
    object ChinaPhone : OnboardingData(
        "Optimasi HP China",
        "Aktifkan 'Auto-Start' agar Sound Horee tetap bersuara kencang meskipun layar HP sedang terkunci atau mati.",
        Icons.Default.RocketLaunch
    )
    object GojekQris : OnboardingData(
        "Baru: Deteksi QRIS Gojek",
        "Kini Sound Horee bisa mendeteksi notifikasi dari aplikasi Gojek secara akurat untuk setiap transaksi QRIS Anda.",
        Icons.Default.Security
    )
}