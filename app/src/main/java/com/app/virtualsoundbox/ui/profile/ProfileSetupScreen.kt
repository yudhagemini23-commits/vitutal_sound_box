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
import androidx.lifecycle.viewmodel.compose.viewModel
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
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userSession = remember { UserSession(context) }

    // Ambil Data dari SharedPreferences yang disimpan MainActivity
    val sharedPref = context.getSharedPreferences("SoundHoreePrefs", Context.MODE_PRIVATE)
    val googleUid = userSession.getUserId() ?: ""
    val googleEmail = userSession.getUserEmail() ?: ""
    val googleName = userSession.getStoreName() ?: ""

    val db = AppDatabase.getDatabase(context)
    val userProfileDao = db.userProfileDao()
    val setupState by viewModel.setupState.collectAsState()

    var storeName by remember { mutableStateOf(userSession.getStoreName() ?: "") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }

    val isLoading = setupState is SetupState.Loading



    LaunchedEffect(setupState) {
        when (setupState) {
            is SetupState.Success -> {
                scope.launch {
                    val uid = userSession.getUserId() ?: googleUid
                    val newProfile = UserProfile(
                        uid = uid,
                        email = googleEmail,
                        storeName = storeName,
                        phoneNumber = phoneNumber,
                        category = selectedCategory,
                        joinedAt = System.currentTimeMillis(),
                        isSynced = true
                    )
                    withContext(Dispatchers.IO) { userProfileDao.insert(newProfile) }

                    // Simpan Nama Toko ke Prefs untuk Dashboard
                    sharedPref.edit().putString("userName", storeName).apply()

                    Toast.makeText(context, "Profil Berhasil Disinkronkan!", Toast.LENGTH_SHORT).show()
                    onProfileSaved(storeName)
                }
            }
            is SetupState.Error -> {
                Toast.makeText(context, (setupState as SetupState.Error).message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    val categories = listOf("Kuliner", "Retail", "Fashion", "Jasa", "Digital", "Lainnya")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Lengkapi Profil Usaha") }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Text("Halo Juragan $googleName! Silakan lengkapi profil toko Anda.", modifier = Modifier.padding(16.dp))
            }

            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("Nama Toko") },
                leadingIcon = { Icon(Icons.Default.Store, null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { if (it.all { c -> c.isDigit() }) phoneNumber = it },
                label = { Text("Nomor WhatsApp") },
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Text("Kategori Bisnis", fontWeight = FontWeight.Bold)
            categories.forEach { category ->
                Row(
                    modifier = Modifier.fillMaxWidth().selectable(
                        selected = (selectedCategory == category),
                        onClick = { if (!isLoading) selectedCategory = category }
                    ).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = (selectedCategory == category), onClick = null)
                    Text(text = category, modifier = Modifier.padding(start = 8.dp))
                }
            }

            Button(
                onClick = {
                    android.util.Log.d("AKD_DEBUG", "UID: $googleUid, Email: $googleEmail")
                    if (storeName.isBlank() || phoneNumber.isBlank() || googleUid.isBlank()) {
                        Toast.makeText(context, "Data tidak lengkap / UID error!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.registerOrLogin(storeName, googleEmail, phoneNumber, selectedCategory, googleUid)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White)
                else Text("Simpan & Lanjutkan")
            }
        }
    }
}