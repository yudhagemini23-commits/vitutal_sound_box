# =========================================================
# PROGUARD RULES UNTUK SOUND HOREE
# =========================================================

# 1. ATURAN CRASH REPORTING (SANGAT PENTING UNTUK GOOGLE PLAY)
# Ini agar kalau ada error di HP user, log di Play Console tetap bisa dibaca
# (menampilkan nomor baris asli), bukan cuma huruf acak.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 2. ATURAN GSON & DATA MODEL (WAJIB)
# Mencegah variabel di data class diacak menjadi a, b, c yang membuat
# proses parsing JSON dari server jadi gagal (null).
-keep class com.app.virtualsoundbox.model.** { *; }
-keep class com.app.virtualsoundbox.data.remote.model.** { *; }
-keepattributes Signature

# 3. ATURAN RETROFIT & OKHTTP (JARINGAN)
# Mengamankan fungsi API agar tidak dibuang oleh R8 karena dianggap "tidak terpakai"
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# 4. ATURAN ROOM DATABASE (LOKAL)
# Room men-generate kode secara otomatis, jika diacak aplikasinya akan langsung Force Close.
-keep class com.app.virtualsoundbox.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# 5. ATURAN FIREBASE & GOOGLE AUTH (LOGIN)
# Mencegah error saat user mencoba login menggunakan akun Google.
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# 6. ATURAN TAMBAHAN JETPACK COMPOSE & COROUTINES
# Mencegah animasi atau state Compose hilang saat di-shrink
-keep class androidx.compose.** { *; }
-keep class kotlinx.coroutines.** { *; }