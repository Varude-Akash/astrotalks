package com.astrotalks.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class SupabaseSession(val accessToken: String, val userId: String, val email: String?)

class SupabaseAuth(private val context: Context) {
    suspend fun signInWithGoogleIdToken(idToken: String): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${BuildConfig.SUPABASE_URL}/auth/v1/token?grant_type=id_token"
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(JSONObject().apply { put("provider", "google"); put("id_token", idToken) }.toString().toByteArray()) }
            val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream).bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299) error("Supabase sign-in failed: ${connection.responseCode}")
            val json = JSONObject(body)
            val user = json.optJSONObject("user") ?: error("Supabase did not return a user")
            val email = if (user.has("email") && !user.isNull("email")) user.getString("email") else null
            Result.success(SupabaseSession(json.getString("access_token"), user.getString("id"), email))
        } catch (error: Exception) { Result.failure(error) }
    }

    suspend fun saveProfile(session: SupabaseSession, profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${BuildConfig.SUPABASE_URL}/rest/v1/profiles?id=eq.${session.userId}"
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "PATCH"
            connection.doOutput = true
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Prefer", "return=minimal")
            val payload = JSONObject().apply {
                put("full_name", profile.fullName); put("religion", profile.religion)
                put("date_of_birth", profile.dateOfBirth); put("birth_time", profile.birthTime)
                put("birthplace", profile.birthplace)
            }
            connection.outputStream.use { it.write(payload.toString().toByteArray()) }
            if (connection.responseCode !in 200..299) error("Profile save failed: ${connection.responseCode}")
            Result.success(Unit)
        } catch (error: Exception) { Result.failure(error) }
    }
}
