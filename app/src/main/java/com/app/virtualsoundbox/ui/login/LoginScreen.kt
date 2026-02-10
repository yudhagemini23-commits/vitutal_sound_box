package com.app.virtualsoundbox.ui.login

import android.widget.Toast
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
    onSignInClick: () -> Unit
) {
    val context = LocalContext.current

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
            // Logo Aplikasi (Placeholder Icon)
            Surface(
                modifier = Modifier.size(100.dp),
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

            // Tombol Login Google
            Button(
                onClick = onSignInClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Mas Yudha bisa ganti ini dengan icon Google PNG kalau ada
                    // Untuk sekarang pakai Text "G" warna warni atau icon sederhana
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

            if (state.isLoading) {
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(color = Color(0xFF2E7D32))
            }
        }
    }
}

// State class untuk Login Screen
data class SignInState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val signInError: String? = null
)