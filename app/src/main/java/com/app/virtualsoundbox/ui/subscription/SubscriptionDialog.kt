package com.app.virtualsoundbox.ui.subscription

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SubscriptionDialog(
    remainingTrial: Int, // Sisa trial (3, 2, 1, atau 0)
    onDismiss: () -> Unit,
    onSubscribeSuccess: (String) -> Unit // Return plan name
) {
    val scope = rememberCoroutineScope()
    var selectedPlan by remember { mutableStateOf(1) } // 0 = Weekly, 1 = Monthly (Default)
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
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
                // 1. Header & Icon
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFFFD700), Color(0xFFFFA000)) // Emas
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Upgrade ke Premium",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF2E7D32)
                )

                // Info Trial
                if (remainingTrial > 0) {
                    Text(
                        text = "Sisa Trial Gratis: $remainingTrial Transaksi",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "Kuota Trial Habis! Langganan agar suara tetap aktif.",
                        fontSize = 14.sp,
                        color = Color(0xFFC62828), // Merah Warning
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Pilihan Paket
                // Paket Mingguan
                PlanCard(
                    title = "Mingguan",
                    price = "Rp 5.000",
                    period = "/minggu",
                    isSelected = selectedPlan == 0,
                    isBestValue = false,
                    onClick = { selectedPlan = 0 }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Paket Bulanan (BEST VALUE)
                PlanCard(
                    title = "Bulanan (Hemat 50%)",
                    price = "Rp 10.000",
                    period = "/bulan",
                    isSelected = selectedPlan == 1,
                    isBestValue = true, // Highlight Emas
                    onClick = { selectedPlan = 1 }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Benefit List
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    BenefitItem("Notifikasi Suara Tanpa Batas")
                    BenefitItem("Support Semua QRIS & Bank")
                    BenefitItem("Prioritas Update Fitur")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Action Button
                Button(
                    onClick = {
                        isProcessing = true
                        // Simulasi Proses Bayar
                        scope.launch {
                            delay(2000) // Pura-pura loading
                            isProcessing = false
                            val planName = if (selectedPlan == 0) "Mingguan" else "Bulanan"
                            onSubscribeSuccess(planName)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedPlan == 1) Color(0xFF2E7D32) else Color(0xFF1B5E20)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (remainingTrial > 0) "Langganan Sekarang" else "Buka Kunci Premium",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Nanti Saja", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun PlanCard(
    title: String,
    price: String,
    period: String,
    isSelected: Boolean,
    isBestValue: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(if (isSelected) Color(0xFF2E7D32) else Color(0xFFE0E0E0))
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
            // Radio Icon
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Circle,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF2E7D32) else Color.LightGray,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(price, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF2E7D32))
                    Text(period, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
                }
            }

            // Badge Best Value
            if (isBestValue) {
                Surface(
                    color = Color(0xFFFFD700), // Emas
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "HEMAT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
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