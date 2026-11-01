package com.astrotalks.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class SupabaseSession(val accessToken: String, val userId: String, val email: String?)
data class ChatSession(val id: String, val balance: Int)
data class ArchivedChat(val id: String, val guideId: String, val createdAt: String)
data class ArchivedMessage(val role: String, val content: String)

class SupabaseAuth(private val context: Context) {
    suspend fun loadProfile(session: SupabaseSession): Result<UserProfile?> = withContext(Dispatchers.IO) {
        try {
            val connection = URL("${BuildConfig.SUPABASE_URL}/rest/v1/profiles?id=eq.${session.userId}&select=full_name,religion,date_of_birth,birth_time,birthplace,onboarding_completed").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"; connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY); connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299) error("Profile could not be loaded")
            val rows = org.json.JSONArray(body)
            if (rows.length() == 0) Result.success(cachedProfile(session.userId)) else { val row = rows.getJSONObject(0); if (!row.optBoolean("onboarding_completed", false)) Result.success(null) else Result.success(UserProfile(row.optString("full_name"), row.optString("religion", "Hinduism"), row.optString("date_of_birth"), row.optString("birth_time"), row.optString("birthplace"))) }
        } catch (error: Exception) { Result.success(cachedProfile(session.userId)) }
    }

    suspend fun loadArchivedChats(session: SupabaseSession): Result<List<ArchivedChat>> = withContext(Dispatchers.IO) {
        try {
            val connection = URL("${BuildConfig.SUPABASE_URL}/rest/v1/chat_sessions?select=id,guide_id,created_at&user_id=eq.${session.userId}&status=eq.archived&order=created_at.desc").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"; connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY); connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299) error("Conversation history could not be loaded")
            val rows = org.json.JSONArray(body); Result.success((0 until rows.length()).map { index -> val row = rows.getJSONObject(index); ArchivedChat(row.getString("id"), row.getString("guide_id"), row.optString("created_at")) })
        } catch (error: Exception) { Result.failure(error) }
    }

    suspend fun loadArchivedMessages(session: SupabaseSession, chatSessionId: String): Result<List<ArchivedMessage>> = withContext(Dispatchers.IO) {
        try {
            val connection = URL("${BuildConfig.SUPABASE_URL}/rest/v1/chat_messages?select=role,content&session_id=eq.$chatSessionId&order=created_at.asc").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"; connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY); connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            val body = connection.inputStream.bufferedReader().use { it.readText() }; if (connection.responseCode !in 200..299) error("Messages could not be loaded")
            val rows = org.json.JSONArray(body); Result.success((0 until rows.length()).map { index -> val row = rows.getJSONObject(index); ArchivedMessage(row.getString("role"), row.getString("content")) })
        } catch (error: Exception) { Result.failure(error) }
    }
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
            val endpoint = "${BuildConfig.SUPABASE_URL}/rest/v1/profiles"
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
            val payload = JSONObject().apply {
                put("id", session.userId)
                put("full_name", profile.fullName); put("religion", profile.religion)
                put("date_of_birth", profile.dateOfBirth); put("birth_time", profile.birthTime)
                put("birthplace", profile.birthplace); put("onboarding_completed", true)
            }
            connection.outputStream.use { it.write(payload.toString().toByteArray()) }
            if (connection.responseCode !in 200..299) error("Profile save failed: ${connection.responseCode}")
            context.getSharedPreferences("astrotalks_profile", Context.MODE_PRIVATE).edit().putString("${session.userId}_name", profile.fullName).putString("${session.userId}_religion", profile.religion).putString("${session.userId}_dob", profile.dateOfBirth).putString("${session.userId}_time", profile.birthTime).putString("${session.userId}_place", profile.birthplace).apply()
            Result.success(Unit)
        } catch (error: Exception) { Result.failure(error) }
    }

    private fun cachedProfile(userId: String): UserProfile? {
        val prefs = context.getSharedPreferences("astrotalks_profile", Context.MODE_PRIVATE)
        val name = prefs.getString("${userId}_name", null) ?: return null
        return UserProfile(name, prefs.getString("${userId}_religion", "Hinduism") ?: "Hinduism", prefs.getString("${userId}_dob", "") ?: "", prefs.getString("${userId}_time", "") ?: "", prefs.getString("${userId}_place", "") ?: "")
    }

    suspend fun deleteAccount(session: SupabaseSession): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val connection = URL("${BuildConfig.SUPABASE_URL}/functions/v1/delete-account").openConnection() as HttpURLConnection
            connection.requestMethod = "DELETE"
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            if (connection.responseCode !in 200..299) error("Account deletion failed: ${connection.responseCode}")
            context.getSharedPreferences("astrotalks_profile", Context.MODE_PRIVATE).edit().clear().apply()
            Result.success(Unit)
        } catch (error: Exception) { Result.failure(error) }
    }

    suspend fun createChatSession(session: SupabaseSession, guideId: String): Result<ChatSession> = withContext(Dispatchers.IO) {
        try {
            val connection = URL("${BuildConfig.SUPABASE_URL}/rest/v1/rpc/start_chat_session").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(JSONObject().put("p_guide_id", guideId).toString().toByteArray()) }
            val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream).bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299) error("Not enough points for this guide")
            val json = JSONObject(body)
            Result.success(ChatSession(json.getString("id"), json.getInt("balance")))
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

    suspend fun generateReflection(session: SupabaseSession, guide: Guide, name: String, religion: String, messages: List<ChatBubble>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payloadMessages = org.json.JSONArray().apply { messages.forEach { put(JSONObject().apply { put("role", if (it.fromUser) "user" else "assistant"); put("content", it.text) }) } }
            val payload = JSONObject().apply { put("guide", JSONObject().apply { put("name", guide.name) }); put("userName", name); put("religion", religion); put("messages", payloadMessages) }
            val connection = URL("${BuildConfig.SUPABASE_URL}/functions/v1/spiritual-chat").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY); connection.setRequestProperty("Authorization", "Bearer ${session.accessToken}"); connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(payload.toString().toByteArray()) }
            val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream).bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299) error("AI reflection unavailable")
            Result.success(JSONObject(body).getString("reply"))
        } catch (error: Exception) { Result.failure(error) }
    }
}
