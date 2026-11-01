package com.astrotalks.app

import android.os.Bundle
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.Calendar
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject

private val Ink = Color(0xFF2A2117)
private val Purple = Color(0xFF956719) // warm ochre accent retained as the existing token name
private val Cream = Color(0xFFFFFBF2)
private val Gold = Color(0xFFD09A2D)
private const val GEOAPIFY_KEY = "8185187c36e24029853d31ebe39daa14"

data class Guide(val id: String, val name: String, val symbol: String, val description: String, val rate: Int, val color: Color)
data class UserProfile(val fullName: String, val religion: String, val dateOfBirth: String, val birthTime: String, val birthplace: String)
data class SignedInUser(val google: GoogleAccount, val session: SupabaseSession)
data class ChatBubble(val text: String, val fromUser: Boolean)
private val guides = listOf(
    Guide("11111111-1111-1111-1111-111111111111", "Shri Krishna", "ॐ", "For clarity, courage & a lighter heart.", 15, Color(0xFF73501A)),
    Guide("22222222-2222-2222-2222-222222222222", "Lord Shiva", "◒", "For stillness, release & transformation.", 20, Color(0xFF51483A)),
    Guide("33333333-3333-3333-3333-333333333333", "Lord Ganesha", "◉", "For new beginnings & clearing the way.", 25, Color(0xFF9A5B31)),
    Guide("44444444-4444-4444-4444-444444444444", "Shri Hanuman", "✦", "For strength, devotion & resilience.", 30, Color(0xFF7F3D25)),
    Guide("55555555-5555-5555-5555-555555555555", "Goddess Saraswati", "◌", "For learning, creativity & expression.", 35, Color(0xFF6B7053)),
    Guide("66666666-6666-6666-6666-666666666666", "Mata Lakshmi", "✧", "For gratitude, abundance & grace.", 40, Color(0xFFB47B22)),
    Guide("77777777-7777-7777-7777-777777777777", "Maa Durga", "✥", "For courage, protection & inner power.", 30, Color(0xFF8E3C32)),
    Guide("88888888-8888-8888-8888-888888888888", "Maa Kali", "☾", "For truth, release & transformation.", 35, Color(0xFF3F354A)),
    Guide("99999999-9999-9999-9999-999999999999", "Lord Kartikeya", "⚔", "For focus, discipline & fearless action.", 30, Color(0xFF486273)),
    Guide("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "Shani Dev", "◐", "For patience, responsibility & balance.", 25, Color(0xFF3F4A50)),
    Guide("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "Jesus Christ", "✝", "For compassion, forgiveness & hope.", 20, Color(0xFF657C80))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { AarohanApp() } }
}

@Composable fun AarohanApp() {
    val context = LocalContext.current
    var user by remember { mutableStateOf<SignedInUser?>(null) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var loadingProfile by remember { mutableStateOf(false) }
    var pointsBalance by remember { mutableIntStateOf(15) }
    val scope = rememberCoroutineScope()
    if (user == null) {
        GoogleSignInScreen { google, session -> user = SignedInUser(google, session); loadingProfile = true; scope.launch { SupabaseAuth(context).loadProfile(session).onSuccess { profile = it }.also { loadingProfile = false } } }
        return
    }
    if (loadingProfile) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Gold) }; return }
    if (profile == null) {
        OnboardingScreenV3(defaultName = user!!.google.name) { completed -> profile = completed; scope.launch { SupabaseAuth(context).saveProfile(user!!.session, completed) } }
        return
    }
    LaunchedEffect(user!!.session.userId) { SupabaseAuth(context).pointsBalance(user!!.session).onSuccess { pointsBalance = it } }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedGuide by remember { mutableStateOf<Guide?>(null) }
    var paywallGuide by remember { mutableStateOf<Guide?>(null) }
    var endedChats by remember { mutableStateOf(listOf<ArchivedChat>()) }
    var selectedArchived by remember { mutableStateOf<ArchivedChat?>(null) }
    LaunchedEffect(user!!.session.userId) { SupabaseAuth(context).loadArchivedChats(user!!.session).onSuccess { endedChats = it } }
    MaterialTheme(colorScheme = lightColorScheme(primary = Purple, background = Cream)) {
        Surface(color = Cream, modifier = Modifier.fillMaxSize()) {
            if (selectedGuide != null) {
                ChatScreenV2(selectedGuide!!, profile!!.fullName, profile!!.religion, user!!.session) { selectedGuide = null }
                return@Surface
            }
            if (selectedArchived != null) {
                ReadOnlyConversationScreen(selectedArchived!!, user!!.session) { selectedArchived = null }
                return@Surface
            }
            paywallGuide?.let { guide -> PointsPaywall(guide, pointsBalance, onDismiss = { paywallGuide = null }) }
            Scaffold(bottomBar = { BottomBar(selectedTab) { selectedTab = it } }) { padding ->
                when (selectedTab) {
                    0 -> HomeScreen(Modifier.padding(padding), profile!!.fullName, profile!!.religion, pointsBalance) { guide -> if (pointsBalance >= guide.rate * 5) selectedGuide = guide else paywallGuide = guide }
                    1 -> ConversationHistoryScreen(Modifier.padding(padding), endedChats) { selectedArchived = it }
                    2 -> StoreScreen(Modifier.padding(padding), pointsBalance)
                    else -> ProfileScreen(Modifier.padding(padding), profile!!, user!!.session, { user = null; profile = null }, { user = null; profile = null })
                }
            }
        }
    }
}

@Composable private fun GoogleSignInScreen(onSignedIn: (GoogleAccount, SupabaseSession) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✦", color = Purple, fontSize = 42.sp)
        Text("AstroTalks", color = Ink, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
        Text("Conversations for the soul", color = Color.Gray, modifier = Modifier.padding(top = 6.dp))
        Text("\nA private space for thoughtful, AI-generated spiritual reflections.", color = Ink, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
        Button(onClick = { loading = true; error = null; scope.launch { GoogleAuth(context).signIn().onSuccess { googleAccount -> SupabaseAuth(context).signInWithGoogleIdToken(googleAccount.idToken).onSuccess { session -> onSignedIn(googleAccount, session) }.onFailure { error = it.message ?: "Supabase sign-in could not be completed." } }.onFailure { error = it.message ?: "Google Sign-In could not be completed." }; loading = false } }, enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink), modifier = Modifier.fillMaxWidth().padding(top = 28.dp), shape = RoundedCornerShape(10.dp)) { Text(if (loading) "Signing in…" else "Continue with Google") }
        Text("Google Sign-In only · no passwords", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
        error?.let { Text(it, color = Color(0xFFB04747), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp)) }
    }
}

@Composable private fun HomeScreen(modifier: Modifier, name: String, religion: String, balance: Int, onGuide: (Guide) -> Unit) {
    Column(modifier.padding(horizontal = 20.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Spacer(Modifier.height(18.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("ASTROTALKS", color = Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp); Text("$balance ✦", color = Gold, fontWeight = FontWeight.Bold) }
        Text("A quiet moment, $name.", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp))
        Text("Choose a guide for your ${religion.lowercase()} circle.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF332719)), shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(vertical = 22.dp).fillMaxWidth()) {
            Column(Modifier.padding(22.dp)) { Text("A quiet place to return to", color = Color(0xFFD7C99F), fontSize = 12.sp); Text("Who would you like to sit with?", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp)); Text("AI-generated spiritual reflections inspired by tradition.", color = Color(0xFFD4D1DF), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
        }
        Text("Your spiritual circle", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        val visibleGuides = guides.filter { if (religion == "Christianity") it.name == "Jesus Christ" else it.name != "Jesus Christ" }
        Spacer(Modifier.height(14.dp)); LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(510.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(visibleGuides) { guide -> GuideCard(guide) { onGuide(guide) } } }
        Text("AstroTalks is an AI-generated spiritual experience, not actual communication with a deity. It does not guarantee blessings, miracles, wealth, or outcomes.", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(vertical = 24.dp))
    }
}

@Composable private fun OnboardingScreen(defaultName: String, onComplete: (UserProfile) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(defaultName) }
    var religion by remember { mutableStateOf("Hinduism") }
    var dob by remember { mutableStateOf("") }
    var birthTime by remember { mutableStateOf("") }
    var birthplace by remember { mutableStateOf("") }
    var locationSuggestions by remember { mutableStateOf(listOf<String>()) }
    var searchingLocation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val prompts = listOf("What do you believe in?" to "Your spiritual path helps us shape your circle.", "What should we call you?" to "A name makes every reflection feel a little closer.", "When did your journey begin?" to "Your birth details help us understand the context you want to share.", "Where did your journey begin?" to "Choose your birthplace so we can use the right local context.")
    val value = listOf(religion, name, "$dob $birthTime", birthplace)[step]
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column { Spacer(Modifier.height(25.dp)); Text("WELCOME, DEAR SOUL", color = Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp); Text("Before we begin,\nlet’s get to know you.", color = Ink, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, lineHeight = 38.sp, modifier = Modifier.padding(top = 12.dp)); Text("Step ${step + 1} of 4", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp)); LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), color = Purple, trackColor = Color(0xFFE9D9B5)); Text(prompts[step].first, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 55.dp)); Text(prompts[step].second, color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)); when (step) { 0 -> OutlinedTextField(value = religion, onValueChange = { religion = it }, modifier = Modifier.fillMaxWidth().padding(top = 25.dp), label = { Text("Religion") }, placeholder = { Text("Hinduism") }); 1 -> OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth().padding(top = 25.dp), label = { Text("Full name") }); 2 -> Column { OutlinedTextField(value = dob, onValueChange = { dob = it }, modifier = Modifier.fillMaxWidth().padding(top = 25.dp), label = { Text("Date of birth · YYYY-MM-DD") }); OutlinedTextField(value = birthTime, onValueChange = { birthTime = it }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text("Birth time · HH:MM") }) }; 3 -> Column { OutlinedTextField(value = birthplace, onValueChange = { birthplace = it }, modifier = Modifier.fillMaxWidth().padding(top = 25.dp), label = { Text("Birthplace") }, placeholder = { Text("City, country") }); TextButton(onClick = { birthplace = "Current location" }) { Text("⌖ Use my current location") } } } }
        Button(onClick = { if (step < 3) step++ else onComplete(UserProfile(name.trim(), religion.trim(), dob.trim(), birthTime.trim(), birthplace.trim())) }, enabled = value.isNotBlank() && if (step == 2) dob.isNotBlank() && birthTime.isNotBlank() else true, modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp), shape = RoundedCornerShape(10.dp)) { Text(if (step == 3) "Enter AstroTalks" else "Continue") }
    }
}

@Composable private fun OnboardingScreenV2(defaultName: String, onComplete: (UserProfile) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(defaultName) }
    var religion by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var birthTime by remember { mutableStateOf("") }
    var birthplace by remember { mutableStateOf("") }
    val religions = listOf("Hinduism" to "ॐ", "Christianity" to "✝")
    val canContinue = when (step) { 0 -> religion.isNotBlank(); 1 -> name.isNotBlank(); 2 -> dob.isNotBlank() && birthTime.isNotBlank(); else -> birthplace.isNotBlank() }
    Column(Modifier.fillMaxSize().background(Cream).padding(horizontal = 22.dp, vertical = 25.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("✦  ASTROTALKS", color = Purple, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 12.sp); Text("${step + 1}/4", color = Color.Gray, fontSize = 11.sp) }
            LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp), color = Purple, trackColor = Color(0xFFE9D9B5))
            Crossfade(targetState = step, label = "onboarding-step") { current ->
                Column(Modifier.padding(top = 44.dp)) {
                    when (current) {
                        0 -> { Text("Okay, let’s meet you ✨", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp); Text("What feels like home to your spirit?", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)); religions.forEach { (label, symbol) -> Card(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { religion = label }, colors = CardDefaults.cardColors(containerColor = if (religion == label) Color(0xFFFFE9AF) else Color.White), border = if (religion == label) androidx.compose.foundation.BorderStroke(2.dp, Purple) else null, shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) { Text(symbol, color = Purple, fontSize = 24.sp); Spacer(Modifier.width(16.dp)); Text(label, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); if (religion == label) Text("✓", color = Purple, fontSize = 20.sp) } } } }
                        1 -> { Text("What should we call you? 🌙", color = Ink, fontSize = 31.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp); Text("Names carry energy. We’ll use yours to make every reflection feel personal.", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp, bottom = 28.dp)); OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 21.sp, fontWeight = FontWeight.SemiBold), label = { Text("First name") }, placeholder = { Text("e.g. Akash") }); Text("Nice to meet you, ${name.ifBlank { "beautiful soul" }}.", color = Purple, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp)) }
                        2 -> { Text("When did your story begin? 🪐", color = Ink, fontSize = 31.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp); Text("Your birth details help us tune the experience to your journey.", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)); OutlinedTextField(value = dob, onValueChange = { dob = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Date of birth · YYYY-MM-DD") }, placeholder = { Text("1996-10-14") }); OutlinedTextField(value = birthTime, onValueChange = { birthTime = it }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp), singleLine = true, label = { Text("Birth time · HH:MM") }, placeholder = { Text("07:42") }) }
                        else -> { Text("Where are you rooted? 📍", color = Ink, fontSize = 31.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp); Text("One last thing. Tell us the place that holds the beginning of your story.", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)); Card(Modifier.fillMaxWidth().clickable { birthplace = "Current location" }, colors = CardDefaults.cardColors(containerColor = Color(0xFF332719)), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Text("⌖", color = Color(0xFFFFD36A), fontSize = 27.sp); Spacer(Modifier.width(14.dp)); Column { Text("Use my current location", color = Color.White, fontWeight = FontWeight.SemiBold); Text("We’ll find your city automatically", color = Color(0xFFD8C9A8), fontSize = 11.sp) } } }; Text("or enter it yourself", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 22.dp, bottom = 6.dp)); OutlinedTextField(value = if (birthplace == "Current location") "" else birthplace, onValueChange = { birthplace = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("City, country") }, placeholder = { Text("Mumbai, India") }) }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { if (step > 0) OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Back") }; Button(onClick = { if (step < 3) step++ else onComplete(UserProfile(name.trim(), religion, dob.trim(), birthTime.trim(), birthplace.trim())) }, enabled = canContinue, modifier = Modifier.weight(2f), shape = RoundedCornerShape(14.dp)) { Text(if (step == 3) "Enter my circle  →" else "That’s me  →") } }
    }
}

@Composable private fun OnboardingScreenV3(defaultName: String, onComplete: (UserProfile) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var religion by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var birthTime by remember { mutableStateOf("") }
    var birthplace by remember { mutableStateOf("") }
    var locationSuggestions by remember { mutableStateOf(listOf<String>()) }
    var searchingLocation by remember { mutableStateOf(false) }
    val religions = listOf("Hinduism" to "ॐ", "Christianity" to "✝")
    val canContinue = when (step) { 0 -> religion.isNotBlank(); 1 -> name.isNotBlank(); 2 -> dob.isNotBlank() && birthTime.isNotBlank(); else -> birthplace.isNotBlank() }
    fun pickDate() { val now = Calendar.getInstance(); DatePickerDialog(context, { _, year, month, day -> dob = "%04d-%02d-%02d".format(year, month + 1, day) }, now.get(Calendar.YEAR) - 25, now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show() }
    fun pickTime() { val now = Calendar.getInstance(); TimePickerDialog(context, { _, hour, minute -> birthTime = "%02d:%02d".format(hour, minute) }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show() }
    Column(Modifier.fillMaxSize().background(Cream).padding(horizontal = 22.dp, vertical = 25.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("✦  ASTROTALKS", color = Purple, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 12.sp); Text("${step + 1}/4", color = Color.Gray, fontSize = 11.sp) }
            LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp), color = Purple, trackColor = Color(0xFFE9D9B5))
            Crossfade(targetState = step, label = "onboarding") { current ->
                Column(Modifier.padding(top = 44.dp)) {
                    when (current) {
                        0 -> { Text("Okay, let’s meet you ✨", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("What feels like home to your spirit?", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)); religions.forEach { (label, symbol) -> Card(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { religion = label }, colors = CardDefaults.cardColors(containerColor = if (religion == label) Color(0xFFFFE9AF) else Color.White), border = if (religion == label) androidx.compose.foundation.BorderStroke(2.dp, Purple) else null, shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Text(symbol, color = Purple, fontSize = 24.sp); Spacer(Modifier.width(16.dp)); Text(label, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); if (religion == label) Text("✓", color = Purple, fontSize = 20.sp) } } } }
                        1 -> { Text("What should we call you? 🌙", color = Ink, fontSize = 31.sp, fontWeight = FontWeight.Bold); Text("Just your first name — that’s all we need.", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp, bottom = 28.dp)); OutlinedTextField(value = name, onValueChange = { name = it.substringBefore(" ") }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("First name") }, placeholder = { Text("e.g. Akash") }); Text("Nice to meet you, ${name.ifBlank { "beautiful soul" }}.", color = Purple, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp)) }
                        2 -> { Text("When did your story begin? 🪐", color = Ink, fontSize = 31.sp, fontWeight = FontWeight.Bold); Text("Tap each card — no typing dates or times.", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)); OutlinedButton(onClick = { pickDate() }, modifier = Modifier.fillMaxWidth().height(62.dp), shape = RoundedCornerShape(16.dp)) { Text(if (dob.isBlank()) "📅  Choose date of birth" else "📅  $dob", color = Ink) }; OutlinedButton(onClick = { pickTime() }, modifier = Modifier.fillMaxWidth().height(62.dp).padding(top = 12.dp), shape = RoundedCornerShape(16.dp)) { Text(if (birthTime.isBlank()) "🕰  Choose birth time" else "🕰  $birthTime", color = Ink) } }
                        else -> { Text("Where are you rooted? 📍", color = Ink, fontSize = 31.sp, fontWeight = FontWeight.Bold); Text("Start typing and choose your actual city.", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)); OutlinedTextField(value = birthplace, onValueChange = { birthplace = it; if (it.length >= 2) scope.launch { searchingLocation = true; locationSuggestions = runCatching { val encoded = URLEncoder.encode(it, "UTF-8"); val body = URL("https://api.geoapify.com/v1/geocode/autocomplete?text=$encoded&limit=5&apiKey=$GEOAPIFY_KEY").readText(); val features = JSONObject(body).optJSONArray("features"); (0 until (features?.length() ?: 0)).map { index -> features!!.getJSONObject(index).getJSONObject("properties").optString("formatted") } }.getOrDefault(emptyList()); searchingLocation = false } }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Birthplace") }, placeholder = { Text("Pune, Maharashtra, India") }); if (searchingLocation) Text("Finding cities…", color = Purple, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)); locationSuggestions.forEach { suggestion -> Card(Modifier.fillMaxWidth().padding(top = 8.dp).clickable { birthplace = suggestion; locationSuggestions = emptyList() }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) { Text("📍  $suggestion", color = Ink, modifier = Modifier.padding(14.dp), fontSize = 13.sp) } } }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { if (step > 0) OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Back") }; Button(onClick = { if (step < 3) step++ else onComplete(UserProfile(name.trim(), religion, dob, birthTime, birthplace.trim())) }, enabled = canContinue, modifier = Modifier.weight(2f), shape = RoundedCornerShape(14.dp)) { Text(if (step == 3) "Enter my circle  →" else "That’s me  →") } }
    }
}

@Composable private fun GuideCard(guide: Guide, onClick: () -> Unit) { Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp)) { Column { Box(Modifier.fillMaxWidth().height(100.dp).background(guide.color), contentAlignment = Alignment.Center) { Text(guide.symbol, color = Color.White, fontSize = 42.sp) }; Column(Modifier.padding(12.dp)) { Text(guide.name, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Text(guide.description, color = Color.Gray, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 4.dp)); Text("${guide.rate} pts/min  →", color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp)) } } } }

@Composable private fun BottomBar(selected: Int, onSelected: (Int) -> Unit) { NavigationBar(containerColor = Color.White) { listOf(Icons.Default.Home to "Home", Icons.Default.Forum to "Chats", Icons.Default.Stars to "Store", Icons.Default.Person to "Profile").forEachIndexed { i, item -> NavigationBarItem(selected = selected == i, onClick = { onSelected(i) }, icon = { Icon(item.first, null) }, label = { Text(item.second, fontSize = 10.sp) }) } } }

@Composable private fun SimpleScreen(modifier: Modifier, title: String, body: String) { Column(modifier.padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text(title, color = Ink, fontSize = 30.sp, fontWeight = FontWeight.SemiBold); Text(body, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp)) } }
@Composable private fun ConversationHistoryScreen(modifier: Modifier, chats: List<ArchivedChat>, onOpen: (ArchivedChat) -> Unit) { Column(modifier.padding(24.dp)) { Text("Conversations", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.SemiBold); Text("Ended sessions stay read-only for 30 days.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp)); if (chats.isEmpty()) Text("Your private spiritual reflections will appear here.", color = Color.Gray, modifier = Modifier.padding(top = 44.dp)) else chats.forEach { chat -> Card(Modifier.fillMaxWidth().padding(top = 14.dp).clickable { onOpen(chat) }, shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Text("✦", color = Purple, fontSize = 22.sp); Spacer(Modifier.width(14.dp)); Column { Text(guides.firstOrNull { it.id == chat.guideId }?.name ?: "Spiritual guide", color = Ink, fontWeight = FontWeight.SemiBold); Text("Tap to read · read-only archive", color = Color.Gray, fontSize = 11.sp) }; Spacer(Modifier.weight(1f)); Text("›", color = Purple, fontSize = 24.sp) } } } } }
@Composable private fun ReadOnlyConversationScreen(chat: ArchivedChat, session: SupabaseSession, onBack: () -> Unit) { val context = LocalContext.current; var messages by remember { mutableStateOf(listOf<ChatBubble>()) }; val guide = guides.firstOrNull { it.id == chat.guideId }; LaunchedEffect(chat.id) { SupabaseAuth(context).loadArchivedMessages(session, chat.id).onSuccess { messages = it.map { row -> ChatBubble(row.content, row.role == "user") } } }; Column(Modifier.fillMaxSize().background(Cream)) { Row(Modifier.fillMaxWidth().background(Color.White).padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Text("‹", fontSize = 32.sp, modifier = Modifier.clickable(onClick = onBack)); Spacer(Modifier.width(12.dp)); Text(guide?.symbol ?: "✦", color = Purple, fontSize = 25.sp); Spacer(Modifier.width(10.dp)); Text(guide?.name ?: "Archived conversation", color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("READ ONLY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold) }; LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 18.dp)) { lazyItems(messages) { bubble -> Row(Modifier.fillMaxWidth(), horizontalArrangement = if (bubble.fromUser) Arrangement.End else Arrangement.Start) { Text(bubble.text, color = if (bubble.fromUser) Color.White else Ink, modifier = Modifier.widthIn(max = 310.dp).background(if (bubble.fromUser) Purple else Color.White, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 12.dp)) } } } } }
@Composable private fun ProfileScreen(modifier: Modifier, profile: UserProfile, session: SupabaseSession, onDeleted: () -> Unit, onSignedOut: () -> Unit) { val context = LocalContext.current; val scope = rememberCoroutineScope(); var confirm by remember { mutableStateOf(false) }; var editing by remember { mutableStateOf(false) }; var saved by remember { mutableStateOf(profile) }; Column(modifier.padding(24.dp)) { Text("Profile & privacy", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.SemiBold); Text("${saved.fullName} · ${saved.religion}\n${saved.dateOfBirth} · ${saved.birthTime}\n${saved.birthplace}", color = Color.Gray, modifier = Modifier.padding(top = 18.dp)); Spacer(Modifier.height(24.dp)); OutlinedButton(onClick = { editing = true }) { Text("Edit details") }; OutlinedButton(onClick = onSignedOut, modifier = Modifier.padding(top = 10.dp)) { Text("Sign out") }; OutlinedButton(onClick = { confirm = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB04747)), modifier = Modifier.padding(top = 10.dp)) { Text("Delete profile") }; Text("Deleting removes your AstroTalks account and profile. A later Google sign-in starts as a new account.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp)); if (editing) EditProfileDialog(saved, onDismiss = { editing = false }) { updated -> saved = updated; editing = false; scope.launch { SupabaseAuth(context).saveProfile(session, updated) } }; if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Delete your profile?") }, text = { Text("This permanently removes your account and saved profile data.") }, confirmButton = { Button(onClick = { scope.launch { SupabaseAuth(context).deleteAccount(session).onSuccess { onDeleted() } } }) { Text("Delete permanently") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }) } }

@Composable private fun EditProfileDialog(profile: UserProfile, onDismiss: () -> Unit, onSave: (UserProfile) -> Unit) { var name by remember { mutableStateOf(profile.fullName) }; var religion by remember { mutableStateOf(profile.religion) }; var birthplace by remember { mutableStateOf(profile.birthplace) }; AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit your details") }, text = { Column { OutlinedTextField(name, { name = it.substringBefore(" ") }, label = { Text("First name") }, singleLine = true); OutlinedTextField(religion, { religion = it }, label = { Text("Religion") }, singleLine = true, modifier = Modifier.padding(top = 8.dp)); OutlinedTextField(birthplace, { birthplace = it }, label = { Text("Birthplace") }, singleLine = true, modifier = Modifier.padding(top = 8.dp)) } }, confirmButton = { Button(onClick = { onSave(profile.copy(fullName = name.trim(), religion = religion.trim(), birthplace = birthplace.trim())) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }) }
@Composable private fun StoreScreen(modifier: Modifier, balance: Int) { Column(modifier.padding(24.dp)) { Text("Points store", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.SemiBold); Text("Simple pricing. Transparent usage.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp)); Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF332719)), modifier = Modifier.padding(vertical = 22.dp).fillMaxWidth()) { Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Text("Available balance", color = Color.White); Spacer(Modifier.weight(1f)); Text("$balance ✦", color = Color(0xFFFFD36A), fontSize = 22.sp, fontWeight = FontWeight.Bold) } }; listOf("300 points · ₹199", "800 points · ₹499", "1,800 points · ₹999").forEach { Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(14.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Text(it, color = Ink, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Button(onClick = {}, contentPadding = PaddingValues(horizontal = 14.dp)) { Text("Buy", fontSize = 11.sp) } } } } } }

@Composable private fun PointsPaywall(guide: Guide, balance: Int, onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("Continue with ${guide.name}") }, text = { Column { Text("${guide.rate} points per minute", color = Purple, fontWeight = FontWeight.Bold); Text("A five-minute continuation needs ${guide.rate * 5} points. Your balance is $balance points.", color = Color.Gray, modifier = Modifier.padding(top = 10.dp)); Text("Choose a points package to continue.", color = Ink, modifier = Modifier.padding(top = 16.dp)); listOf("300 points · ₹199", "800 points · ₹499", "1,800 points · ₹999").forEach { packageText -> OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(packageText) } } } }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } }) }

@Composable private fun ChatScreen(guide: Guide, name: String, onBack: () -> Unit) { var seconds by remember { mutableIntStateOf(60) }; var message by remember { mutableStateOf("") }; LaunchedEffect(Unit) { while (seconds > 0) { delay(1000); seconds-- } }; Column(Modifier.fillMaxSize().background(Cream)) { Row(Modifier.fillMaxWidth().background(Color.White).padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Text("‹", fontSize = 32.sp, modifier = Modifier.clickable(onClick = onBack)); Spacer(Modifier.width(12.dp)); Text(guide.symbol, color = Purple, fontSize = 25.sp); Spacer(Modifier.width(10.dp)); Column { Text(guide.name, color = Ink, fontWeight = FontWeight.Bold); Text("AI spiritual experience · ${guide.rate} pts/min", color = Color.Gray, fontSize = 10.sp) }; Spacer(Modifier.weight(1f)); Text("00:${seconds.toString().padStart(2, '0')}", color = Purple, fontWeight = FontWeight.Bold) }; Text("✦ This is an AI-generated reflection, not actual communication with a deity.", color = Color(0xFF6C5730), fontSize = 10.sp, modifier = Modifier.fillMaxWidth().background(Color(0xFFF5E8C8)).padding(12.dp)); Column(Modifier.weight(1f).padding(20.dp), verticalArrangement = Arrangement.Bottom) { Row(verticalAlignment = Alignment.Top) { Text(guide.symbol, color = Purple, fontSize = 20.sp); Spacer(Modifier.width(10.dp)); Text("Welcome, $name. Take a gentle breath. What feels most present in your heart today?", color = Ink, modifier = Modifier.background(Color.White, RoundedCornerShape(14.dp)).padding(14.dp)) } }; Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = message, onValueChange = { message = it }, modifier = Modifier.weight(1f), placeholder = { Text("Write what’s on your heart…") }); Spacer(Modifier.width(8.dp)); Button(onClick = { message = "" }, enabled = message.isNotBlank()) { Text("↑") } } } }

@Composable private fun ChatScreenV2(guide: Guide, name: String, religion: String, auth: SupabaseSession, onBack: (String) -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope(); val listState = androidx.compose.foundation.lazy.rememberLazyListState(); var seconds by remember { mutableIntStateOf(60) }; var started by remember { mutableStateOf(false) }; var message by remember { mutableStateOf("") }; var showContinue by remember { mutableStateOf(false) }; var chatId by remember { mutableStateOf<String?>(null) }; var messages by remember { mutableStateOf(listOf<ChatBubble>()) }; var isTyping by remember { mutableStateOf(false) }; var typingDots by remember { mutableIntStateOf(1) }
    LaunchedEffect(started, seconds) { if (started && seconds > 0) { delay(1000); seconds-- } else if (started && seconds == 0) showContinue = true }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }
    LaunchedEffect(isTyping) { while (isTyping) { typingDots = if (typingDots == 3) 1 else typingDots + 1; delay(380) } }
    Column(Modifier.fillMaxSize().background(Cream)) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Text("‹", fontSize = 32.sp, modifier = Modifier.clickable { scope.launch { chatId?.let { SupabaseAuth(context).archiveChatSession(auth, it, if (started) 60 - seconds else 0) }; onBack(guide.name) } }); Spacer(Modifier.width(12.dp)); Text(guide.symbol, color = Purple, fontSize = 25.sp); Spacer(Modifier.width(10.dp)); Column { Text(guide.name, color = Ink, fontWeight = FontWeight.Bold); Text("AI reflection · ${guide.rate} pts/min", color = Color.Gray, fontSize = 10.sp) }; Spacer(Modifier.weight(1f)); Text("00:${seconds.toString().padStart(2, '0')}", color = Purple, fontWeight = FontWeight.Bold) }
        Text("✦ This is an AI-generated reflection, not actual communication with a deity.", color = Color(0xFF6C5730), fontSize = 10.sp, modifier = Modifier.fillMaxWidth().background(Color(0xFFF5E8C8)).padding(12.dp))
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { lazyItems(messages) { bubble -> Row(Modifier.fillMaxWidth(), horizontalArrangement = if (bubble.fromUser) Arrangement.End else Arrangement.Start) { Text(bubble.text, color = if (bubble.fromUser) Color.White else Ink, modifier = Modifier.widthIn(max = 310.dp).background(if (bubble.fromUser) Purple else Color.White, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 12.dp)) } }; if (isTyping) { item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) { Text("${guide.symbol}  ${"• ".repeat(typingDots)}", color = Purple, modifier = Modifier.background(Color.White, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 12.dp)) } } } }
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = message, onValueChange = { message = it }, modifier = Modifier.weight(1f), placeholder = { Text("Say what’s on your heart…") }, shape = RoundedCornerShape(24.dp), singleLine = false); Spacer(Modifier.width(8.dp)); Button(onClick = { val sent = message.trim(); val updatedMessages = messages + ChatBubble(sent, true); messages = updatedMessages; message = ""; isTyping = true; scope.launch { val activeId = chatId ?: SupabaseAuth(context).createChatSession(auth, guide.id).getOrNull()?.id; if (activeId == null) { isTyping = false; messages = messages + ChatBubble("This guide needs more points before a session can begin.", false); return@launch }; started = true; chatId = activeId; SupabaseAuth(context).appendChatMessage(auth, activeId, "user", sent); delay((900L + (sent.length.coerceAtMost(120) * 8L))); SupabaseAuth(context).generateReflection(auth, guide, name, religion, updatedMessages).onSuccess { reply -> isTyping = false; messages = messages + ChatBubble(reply, false); SupabaseAuth(context).appendChatMessage(auth, activeId, "assistant", reply) }.onFailure { isTyping = false; messages = messages + ChatBubble("I’m having trouble connecting right now. Please try again in a moment.", false) } } }, enabled = message.isNotBlank() && seconds > 0 && !isTyping, shape = RoundedCornerShape(22.dp), contentPadding = PaddingValues(horizontal = 17.dp, vertical = 14.dp)) { Text("↑", fontSize = 20.sp) } }
    }
    if (showContinue) AlertDialog(onDismissRequest = {}, title = { Text("Your free minute is complete") }, text = { Text("Continue this conversation for 5 minutes using ${guide.rate * 5} points, or end this session and save it to your history.") }, confirmButton = { Button(onClick = { scope.launch { chatId?.let { SupabaseAuth(context).purchaseFiveMinuteExtension(auth, it).onSuccess { seconds = 300; showContinue = false } } } }) { Text("Continue · 5 min") } }, dismissButton = { OutlinedButton(onClick = { scope.launch { chatId?.let { SupabaseAuth(context).archiveChatSession(auth, it, 60) }; onBack(guide.name) } }) { Text("End conversation") } })
}
