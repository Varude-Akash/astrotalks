package com.astrotalks.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

private val Ink = Color(0xFF2A2117)
private val Purple = Color(0xFF956719) // warm ochre accent retained as the existing token name
private val Cream = Color(0xFFFFFBF2)
private val Gold = Color(0xFFD09A2D)

data class Guide(val name: String, val symbol: String, val description: String, val rate: Int, val color: Color)
data class UserProfile(val fullName: String, val religion: String, val dateOfBirth: String, val birthTime: String, val birthplace: String)
data class SignedInUser(val google: GoogleAccount, val session: SupabaseSession)
private val guides = listOf(
    Guide("Shri Krishna", "ॐ", "For clarity, courage & a lighter heart.", 15, Color(0xFF73501A)),
    Guide("Lord Shiva", "◒", "For stillness, release & transformation.", 20, Color(0xFF51483A)),
    Guide("Lord Ganesha", "◉", "For new beginnings & clearing the way.", 25, Color(0xFF9A5B31)),
    Guide("Shri Hanuman", "✦", "For strength, devotion & resilience.", 30, Color(0xFF7F3D25)),
    Guide("Goddess Saraswati", "◌", "For learning, creativity & expression.", 35, Color(0xFF6B7053)),
    Guide("Shri Mahalakshmi", "✧", "For gratitude, abundance & grace.", 40, Color(0xFFB47B22))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { AarohanApp() } }
}

@Composable fun AarohanApp() {
    val context = LocalContext.current
    var user by remember { mutableStateOf<SignedInUser?>(null) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    val scope = rememberCoroutineScope()
    if (user == null) {
        GoogleSignInScreen { google, session -> user = SignedInUser(google, session) }
        return
    }
    if (profile == null) {
        OnboardingScreen(defaultName = user!!.google.name) { completed -> profile = completed; scope.launch { SupabaseAuth(context).saveProfile(user!!.session, completed) } }
        return
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedGuide by remember { mutableStateOf<Guide?>(null) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Purple, background = Cream)) {
        Surface(color = Cream, modifier = Modifier.fillMaxSize()) {
            Scaffold(bottomBar = { BottomBar(selectedTab) { selectedTab = it } }) { padding ->
                when (selectedTab) {
                    0 -> HomeScreen(Modifier.padding(padding), profile!!.fullName) { selectedGuide = it }
                    1 -> SimpleScreen(Modifier.padding(padding), "Conversations", "Your private spiritual reflections will appear here.")
                    2 -> StoreScreen(Modifier.padding(padding))
                    else -> SimpleScreen(Modifier.padding(padding), "Profile & privacy", "Signed in with Google\n\nAnanya Sharma · Hinduism\n14 October 1996 · Mumbai, India")
                }
            }
            selectedGuide?.let { ChatDialog(it) { selectedGuide = null } }
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
        Button(onClick = { loading = true; error = null; scope.launch { GoogleAuth(context).signIn().onSuccess { googleAccount -> SupabaseAuth(context).signInWithGoogleIdToken(googleAccount.idToken).onSuccess { session -> onSignedIn(googleAccount, session) }.onFailure { error = it.message ?: "Supabase sign-in could not be completed." } }.onFailure { error = it.message ?: "Google Sign-In could not be completed." }; loading = false } }, enabled = !loading, modifier = Modifier.fillMaxWidth().padding(top = 28.dp), shape = RoundedCornerShape(10.dp)) { Text(if (loading) "Signing in…" else "Continue with Google") }
        Text("Google Sign-In only · no passwords", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
        error?.let { Text(it, color = Color(0xFFB04747), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp)) }
    }
}

@Composable private fun HomeScreen(modifier: Modifier, name: String, onGuide: (Guide) -> Unit) {
    Column(modifier.padding(horizontal = 20.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Spacer(Modifier.height(25.dp)); Text("ASTROTALKS", color = Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Good morning,\n$name.", color = Ink, fontSize = 38.sp, fontWeight = FontWeight.SemiBold, lineHeight = 42.sp)
        Text("Take a breath. Ask what’s on your heart.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF332719)), shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(vertical = 22.dp).fillMaxWidth()) {
            Column(Modifier.padding(22.dp)) { Text("A quiet place to return to", color = Color(0xFFD7C99F), fontSize = 12.sp); Text("Who would you like to sit with?", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp)); Text("AI-generated spiritual reflections inspired by tradition.", color = Color(0xFFD4D1DF), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) { Text("Your spiritual circle", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold); Text("240 ✦", color = Gold, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(14.dp)); LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(510.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(guides) { guide -> GuideCard(guide) { onGuide(guide) } } }
        Text("AstroTalks is an AI-generated spiritual experience, not actual communication with a deity. It does not guarantee blessings, miracles, wealth, or outcomes.", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(vertical = 24.dp))
    }
}

@Composable private fun OnboardingScreen(defaultName: String, onComplete: (UserProfile) -> Unit) {
    var name by remember { mutableStateOf(defaultName) }
    var religion by remember { mutableStateOf("Hinduism") }
    var dob by remember { mutableStateOf("") }
    var birthTime by remember { mutableStateOf("") }
    var birthplace by remember { mutableStateOf("") }
    val valid = name.isNotBlank() && dob.isNotBlank() && birthTime.isNotBlank() && birthplace.isNotBlank()
    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(24.dp)) {
        Spacer(Modifier.height(25.dp)); Text("YOUR PATH", color = Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Tell us a little\nabout yourself.", color = Ink, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, lineHeight = 38.sp, modifier = Modifier.padding(top = 10.dp))
        Text("This helps personalize your spiritual circle. You can edit these details anytime.", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, bottom = 22.dp))
        Text("Religion", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); OutlinedTextField(value = religion, onValueChange = { religion = it }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), singleLine = true, label = { Text("Religion") }, placeholder = { Text("e.g. Hinduism") })
        Text("Full name", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(top = 15.dp)); OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), singleLine = true, label = { Text("Full name") })
        Text("Date of birth", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(top = 15.dp)); OutlinedTextField(value = dob, onValueChange = { dob = it }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), singleLine = true, label = { Text("YYYY-MM-DD") }, placeholder = { Text("1996-10-14") })
        Text("Birth time", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(top = 15.dp)); OutlinedTextField(value = birthTime, onValueChange = { birthTime = it }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), singleLine = true, label = { Text("HH:MM") }, placeholder = { Text("07:42") })
        Text("Birthplace", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(top = 15.dp)); OutlinedTextField(value = birthplace, onValueChange = { birthplace = it }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), singleLine = true, label = { Text("City, country") }, placeholder = { Text("Mumbai, India") }, supportingText = { Text("We’ll add place search and map suggestions next.") })
        Button(onClick = { onComplete(UserProfile(name.trim(), religion.trim(), dob.trim(), birthTime.trim(), birthplace.trim())) }, enabled = valid, modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 30.dp), shape = RoundedCornerShape(10.dp)) { Text("Continue to AstroTalks") }
    }
}

@Composable private fun GuideCard(guide: Guide, onClick: () -> Unit) { Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp)) { Column { Box(Modifier.fillMaxWidth().height(100.dp).background(guide.color), contentAlignment = Alignment.Center) { Text(guide.symbol, color = Color.White, fontSize = 42.sp) }; Column(Modifier.padding(12.dp)) { Text(guide.name, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Text(guide.description, color = Color.Gray, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 4.dp)); Text("${guide.rate} pts/min  →", color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp)) } } } }

@Composable private fun BottomBar(selected: Int, onSelected: (Int) -> Unit) { NavigationBar(containerColor = Color.White) { listOf(Icons.Default.Home to "Home", Icons.Default.Forum to "Chats", Icons.Default.Stars to "Store", Icons.Default.Person to "Profile").forEachIndexed { i, item -> NavigationBarItem(selected = selected == i, onClick = { onSelected(i) }, icon = { Icon(item.first, null) }, label = { Text(item.second, fontSize = 10.sp) }) } } }

@Composable private fun SimpleScreen(modifier: Modifier, title: String, body: String) { Column(modifier.padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text(title, color = Ink, fontSize = 30.sp, fontWeight = FontWeight.SemiBold); Text(body, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp)) } }
@Composable private fun StoreScreen(modifier: Modifier) { Column(modifier.padding(24.dp)) { Text("Points store", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.SemiBold); Text("Simple pricing. Transparent usage.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp)); Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF332719)), modifier = Modifier.padding(vertical = 22.dp).fillMaxWidth()) { Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Text("Available balance", color = Color.White); Spacer(Modifier.weight(1f)); Text("240 ✦", color = Color(0xFFFFD36A), fontSize = 22.sp, fontWeight = FontWeight.Bold) } }; listOf("300 points · ₹199", "800 points · ₹499", "1,800 points · ₹999").forEach { Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(14.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Text(it, color = Ink, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Button(onClick = {}, contentPadding = PaddingValues(horizontal = 14.dp)) { Text("Buy", fontSize = 11.sp) } } } } } }

@Composable private fun ChatDialog(guide: Guide, onDismiss: () -> Unit) { var seconds by remember { mutableIntStateOf(60) }; LaunchedEffect(Unit) { while (seconds > 0) { delay(1000); seconds-- } }; AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = onDismiss) { Text("Close") } }, title = { Row(verticalAlignment = Alignment.CenterVertically) { Text(guide.symbol, color = Purple, fontSize = 24.sp); Spacer(Modifier.width(10.dp)); Column { Text(guide.name); Text("00:${seconds.toString().padStart(2, '0')} · ${guide.rate} pts/min", color = Purple, fontSize = 11.sp) } } }, text = { Column { Text("✦ AI-generated spiritual experience", color = Purple, fontSize = 11.sp); Text("\nWelcome, Ananya. Take a gentle breath. What feels most present in your heart today?", color = Ink, lineHeight = 22.sp); OutlinedTextField(value = "", onValueChange = {}, placeholder = { Text("Write what’s on your heart…") }, modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) } }) }
