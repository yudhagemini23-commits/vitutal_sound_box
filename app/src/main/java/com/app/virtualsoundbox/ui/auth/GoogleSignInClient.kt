package com.app.virtualsoundbox.ui.auth

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.app.virtualsoundbox.R
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await


// Data class untuk hasil login yang bersih
data class SignInResult(
    val data: UserData?,
    val errorMessage: String?
)

data class UserData(
    val userId: String,
    val userName: String?,
    val email: String?,
    val profilePictureUrl: String?
)

class GoogleAuthClient(
    private val context: Context
) {
    private val auth = Firebase.auth
    private val oneTapClient: SignInClient = Identity.getSignInClient(context)

    // Cek apakah user sudah login sebelumnya
    fun getSignedInUser(): UserData? = auth.currentUser?.run {
        UserData(
            userId = uid,
            userName = displayName,
            email = email,
            profilePictureUrl = photoUrl?.toString()
        )
    }

    // Langkah 1: Memunculkan Pop-up pilih akun Google
    suspend fun signIn(): IntentSender? {
        val result = try {
            oneTapClient.beginSignIn(
                BeginSignInRequest.builder()
                    .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                            .setSupported(true)
                            .setServerClientId(context.getString(R.string.default_web_client_id)) // Otomatis digenerate dari google-services.json
                            .setFilterByAuthorizedAccounts(false)
                            .build()
                    )
                    .setAutoSelectEnabled(true)
                    .build()
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return result.pendingIntent.intentSender
    }

    // Langkah 2: Memproses hasil pilihan user & Login ke Firebase
    suspend fun signInWithIntent(intent: Intent): SignInResult {
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(intent)
            val googleIdToken = credential.googleIdToken
            val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)

            val user = auth.signInWithCredential(googleCredentials).await().user

            return SignInResult(
                data = user?.run {
                    UserData(
                        userId = uid,
                        userName = displayName,
                        email = email,
                        profilePictureUrl = photoUrl?.toString()
                    )
                },
                errorMessage = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return SignInResult(data = null, errorMessage = e.message)
        }
    }

    suspend fun signOut() {
        try {
            oneTapClient.signOut().await()
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}