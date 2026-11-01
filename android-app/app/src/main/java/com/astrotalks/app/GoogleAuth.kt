package com.astrotalks.app

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException

/**
 * Native Google account picker. Replace this value with the Web client ID
 * from Firebase Console before shipping the app.
 */
private const val GOOGLE_WEB_CLIENT_ID = "126316545839-4qlmafojquvfcbfbbk0p6k7u6svat9nr.apps.googleusercontent.com"

class GoogleAuth(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): Result<GoogleAccount> = try {
        require(GOOGLE_WEB_CLIENT_ID.startsWith("REPLACE").not()) {
            "Google Sign-In is not configured yet. Add the Firebase Web client ID."
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()
        val result = credentialManager.getCredential(
            context,
            GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
        )
        val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
        Result.success(GoogleAccount(credential.displayName.orEmpty(), credential.id, credential.idToken))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: GetCredentialException) {
        Result.failure(error)
    } catch (error: Exception) {
        Result.failure(error)
    }
}

data class GoogleAccount(val name: String, val email: String, val idToken: String)
