package com.astrotalks.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class SupabaseSession(val accessToken: String, val userId: String, val email: String?)
data class ChatSession(val id: String)

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

    suspend fun deleteAccount(session: SupabaseSession): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val connection = URL("${BuildConfig.SUPABASE_URL}/auth/v1/user").openConnection() as HttpURLConnection
            connection.requestMethod = "DELETE"
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            if (connection.responseCode !in 200..299) error("Account deletion failed: ${connection.responseCode}")
            Result.success(Unit)
        } catch (error: Exception) { Result.failure(error) }
    }

    suspend fun createChatSession(session: SupabaseSession, guideId: String): Result<ChatSession> = withContext(Dispatchers.IO) {
        try {
            val connection = URL("${BuildConfig.SUPABASE_URL}/rest/v1/chat_sessions").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            connection.setRequestProperty("Content-Type", "application/json"); connection.setRequestProperty("Prefer", "return=representation")
            connection.outputStream.use { it.write(JSONObject().apply { put("user_id", session.userId); put("guide_id", guideId); put("status", "active") }.toString().toByteArray()) }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299) error("Chat session could not be created")
            Result.success(ChatSession(JSONObject(body.trim().removePrefix("[").removeSuffix("]")).getString("id")))
        } catch (error: Exception) { Result.failure(error) }
    }

    suspend fun appendChatMessage(session: SupabaseSession, chatSessionId: String, role: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val connection = URL("${BuildConfig.SUPABASE_URL}/rest/v1/chat_messages").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY); connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}"); connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(JSONObject().apply { put("session_id", chatSessionId); put("role", role); put("content", content); put("delivered", true) }.toString().toByteArray()) }
            if (connection.responseCode !in 200..299) error("Message could not be saved")
            Result.success(Unit)
        } catch (error: Exception) { Result.failure(error) }
    }

    suspend fun endChatSession(session: SupabaseSession, chatSessionId: String, billableSeconds: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val connection = URL("${BuildConfig.SUPABASE_URL}/rest/v1/chat_sessions?id=eq.$chatSessionId").openConnection() as HttpURLConnection
            connection.requestMethod = "PATCH"; connection.doOutput = true
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY); connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}"); connection.setRequestProperty("Content-Type", "application/json"); connection.setRequestProperty("Prefer", "return=minimal")
            connection.outputStream.use { it.write(JSONObject().apply { put("status", "archived"); put("billable_seconds", billableSeconds) }.toString().toByteArray()) }
            if (connection.responseCode !in 200..299) error("Chat session could not be ended")
            Result.success(Unit)
        } catch (error: Exception) { Result.failure(error) }
    }

    suspend fun purchaseFiveMinuteExtension(session: SupabaseSession, chatSessionId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val connection = URL("${BuildConfig.SUPABASE_URL}/rest/v1/rpc/purchase_chat_extension").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY); connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}"); connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(JSONObject().put("p_session_id", chatSessionId).toString().toByteArray()) }
            val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream).bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299) error("Not enough points for this extension")
            Result.success(JSONObject(body).getInt("balance"))
        } catch (error: Exception) { Result.failure(error) }
    }

    suspend fun archiveChatSession(session: SupabaseSession, chatSessionId: String, billableSeconds: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val connection = URL("${BuildConfig.SUPABASE_URL}/rest/v1/rpc/archive_chat_session").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY); connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}"); connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(JSONObject().apply { put("p_session_id", chatSessionId); put("p_billable_seconds", billableSeconds) }.toString().toByteArray()) }
            if (connection.responseCode !in 200..299) error("Chat session could not be archived")
            Result.success(Unit)
        } catch (error: Exception) { Result.failure(error) }
    }

    suspend fun pointsBalance(session: SupabaseSession): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val connection = URL("${BuildConfig.SUPABASE_URL}/rest/v1/rpc/points_balance").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY); connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}"); connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write("{}".toByteArray()) }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299) error("Balance could not be loaded")
            Result.success(body.trim().toInt())
        } catch (error: Exception) { Result.failure(error) }
    }
}
