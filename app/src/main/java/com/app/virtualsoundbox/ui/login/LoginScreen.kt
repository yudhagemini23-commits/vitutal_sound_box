package com.app.virtualsoundbox.ui.login

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun LoginScreen(
    state: SignInState,
    onSignInClick: () -> Unit,
    onTesterLoginClick: (String) -> Unit // Callback baru untuk bypass
) {
    val context = LocalContext.current

    // State internal untuk mendeteksi ketukan rahasia
    var tapCount by remember { mutableIntStateOf(0) }
    var isTesterModeVisible by remember { mutableStateOf(false) }

    // Tampilkan error jika login gagal
    LaunchedEffect(key1 = state.signInError) {
        state.signInError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo Aplikasi - Bisa diketuk 7x untuk munculkan backdoor
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        tapCount++
                        if (tapCount >= 7) {
                            isTesterModeVisible = true
                        }
                    },
                shape = CircleShape,
                color = Color(0xFFE8F5E9)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("SH", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Selamat Datang\nJuragan!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Masuk untuk menyimpan data transaksi dan mengakses fitur premium.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Tombol Login Google Normal
            Button(
                onClick = onSignInClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.Blue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Lanjutkan dengan Google",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // --- TOMBOL RAHASIA (Hanya muncul setelah 7x klik logo) ---
            if (isTesterModeVisible) {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { onTesterLoginClick("tester@algoritmakitadigital.id") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Reviewer Access: Bypass Login", fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Mode penguji aktif untuk Google Review",
                    fontSize = 11.sp,
                    color = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (state.isLoading) {
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(color = Color(0xFF2E7D32))
            }
        }
    }
}

data class SignInState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val signInError: String? = null
)