package com.app.virtualsoundbox.ui.profile

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.lifecycle.viewmodel.compose.viewModel // Pastikan ada dependency ini
import com.app.virtualsoundbox.data.local.AppDatabase
import com.app.virtualsoundbox.model.UserProfile
import com.app.virtualsoundbox.utils.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onProfileSaved: (String) -> Unit,
    // Inject ViewModel di sini
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userSession = remember { UserSession(context) }

    // 1. Ambil Data Awal (jika ada sisa login sebelumnya)
    val sharedPref = context.getSharedPreferences("SoundHoreePrefs", Context.MODE_PRIVATE)
    val googleEmail = sharedPref.getString("userEmail", "") ?: ""
    val googleName = sharedPref.getString("userName", "") ?: ""

    // Database Room Local
    val db = AppDatabase.getDatabase(context)
    val userProfileDao = db.userProfileDao()

    // Observasi State dari ViewModel (Loading, Success, Error)
    val setupState by viewModel.setupState.collectAsState()

    // State Form
    var storeName by remember { mutableStateOf(googleName) }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }

    // Ganti variable isLoading manual dengan state dari ViewModel
    val isLoading = setupState is SetupState.Loading

    // --- LOGIC: DENGARKAN HASIL DARI BACKEND ---
    LaunchedEffect(setupState) {
        when (setupState) {
            is SetupState.Success -> {
                // 1. Backend Sukses, Token sudah disimpan ViewModel di UserSession
                // 2. Sekarang kita simpan data ke Room Local (Backup Offline)
                scope.launch {
                    val uid = userSession.getUserId() ?: "unknown_uid"
                    val email = "user@soundhoree.app" // Bisa diambil dari input jika ada field email

                    val newProfile = UserProfile(
                        uid = uid,
                        email = email,
                        storeName = storeName,
                        phoneNumber = phoneNumber,
                        category = selectedCategory,
                        joinedAt = System.currentTimeMillis(),
                        isSynced = true // Karena sudah sukses dari backend
                    )

                    withContext(Dispatchers.IO) {
                        userProfileDao.insert(newProfile)
                    }

                    Toast.makeText(context, "Profil Tersimpan & Online!", Toast.LENGTH_SHORT).show()
                    onProfileSaved(storeName) // Pindah ke Home
                }
            }
            is SetupState.Error -> {
                val errorMsg = (setupState as SetupState.Error).message
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

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
            // Informasi Header
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Halo Juragan! Data ini akan disinkronkan ke server agar aman jika ganti HP.",
                        fontSize = 14.sp,
                        color = Color(0xFF0D47A1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Input Nama Toko
            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("Nama Toko / Usaha") },
                leadingIcon = { Icon(Icons.Default.Store, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
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
                placeholder = { Text("0812...") },
                enabled = !isLoading
            )

            // 3. Pilihan Kategori
            Text("Kategori Bisnis", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Column {
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (selectedCategory == category),
                                    onClick = { if (!isLoading) selectedCategory = category }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedCategory == category),
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2E7D32)),
                                enabled = !isLoading
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = category, fontSize = 14.sp)
                        }
                        if (category != categories.last()) {
                            Divider(color = Color(0xFFF5F5F5))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Tombol Simpan
            Button(
                onClick = {
                    if (storeName.isBlank() || phoneNumber.isBlank() || selectedCategory.isBlank()) {
                        Toast.makeText(context, "Mohon lengkapi semua data!", Toast.LENGTH_SHORT).show()
                    } else {
                        // --- INTEGRASI BACKEND ---
                        viewModel.registerUser(
                            storeName = storeName,
                            email = googleEmail,
                            phone = phoneNumber,
                            category = selectedCategory // SEKARANG TERKIRIM
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = MaterialTheme.shapes.medium,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Simpan & Onlinekan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Check, null)
                }
            }
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}