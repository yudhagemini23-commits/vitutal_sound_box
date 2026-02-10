package com.app.virtualsoundbox.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.virtualsoundbox.data.local.AppDatabase
import com.app.virtualsoundbox.model.UserProfile
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onProfileSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val auth = Firebase.auth

    // Inisialisasi Database Local
    val db = AppDatabase.getDatabase(context)
    val userProfileDao = db.userProfileDao()

    // State Form
    var storeName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Scope untuk operasi database background
    val scope = rememberCoroutineScope()

    val categories = listOf(
        "Kuliner (Makanan/Minuman)",
        "Retail / Kelontong",
        "Fashion / Pakaian",
        "Jasa / Service",
        "Digital / Pulsa / PPOB",
        "Lainnya"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lengkapi Profil Usaha") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FA))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Data disimpan secara lokal di perangkat Anda.",
                color = Color.Gray,
                fontSize = 14.sp
            )

            // 1. Input Nama Toko
            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("Nama Toko / Usaha") },
                leadingIcon = { Icon(Icons.Default.Store, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 2. Input Nomor HP
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { if (it.all { char -> char.isDigit() }) phoneNumber = it },
                label = { Text("Nomor WhatsApp / HP") },
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("0812...") }
            )

            // 3. Pilihan Kategori
            Text("Kategori Bisnis", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column {
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (selectedCategory == category),
                                    onClick = { selectedCategory = category }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedCategory == category),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = category, fontSize = 14.sp)
                        }
                        Divider(color = Color(0xFFEEEEEE))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Tombol Simpan (KE DATABASE LOKAL)
            Button(
                onClick = {
                    if (storeName.isBlank() || phoneNumber.isBlank() || selectedCategory.isBlank()) {
                        Toast.makeText(context, "Mohon lengkapi semua data!", Toast.LENGTH_SHORT).show()
                    } else {
                        isLoading = true
                        val currentUser = auth.currentUser
                        val uid = currentUser?.uid ?: return@Button
                        val email = currentUser.email ?: ""

                        // Object UserProfile untuk disimpan ke Room
                        val newProfile = UserProfile(
                            uid = uid,
                            email = email,
                            storeName = storeName,
                            phoneNumber = phoneNumber,
                            category = selectedCategory,
                            joinedAt = System.currentTimeMillis(),
                            isSynced = false // Tandai belum sync ke server Golang
                        )

                        // Simpan ke Room Database (Background Thread)
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                userProfileDao.insert(newProfile)
                            }

                            // Kembali ke UI Thread
                            isLoading = false
                            onProfileSaved(storeName) // Lanjut ke Dashboard
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Simpan & Masuk", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Check, null)
                }
            }
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}