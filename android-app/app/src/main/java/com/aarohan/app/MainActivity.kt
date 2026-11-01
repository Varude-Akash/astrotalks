package com.aarohan.app

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

private val Ink = Color(0xFF202033)
private val Purple = Color(0xFF5F56A7)
private val Cream = Color(0xFFF8F7F4)
private val Gold = Color(0xFFC79A55)

data class Guide(val name: String, val symbol: String, val description: String, val rate: Int, val color: Color)
private val guides = listOf(
    Guide("Shri Krishna", "ॐ", "For clarity, courage & a lighter heart.", 15, Color(0xFF484A91)),
    Guide("Lord Shiva", "◒", "For stillness, release & transformation.", 20, Color(0xFF75628B)),
    Guide("Lord Ganesha", "◉", "For new beginnings & clearing the way.", 25, Color(0xFFB76D67)),
    Guide("Shri Hanuman", "✦", "For strength, devotion & resilience.", 30, Color(0xFFB37645)),
    Guide("Goddess Saraswati", "◌", "For learning, creativity & expression.", 35, Color(0xFF4E7776)),
    Guide("Shri Mahalakshmi", "✧", "For gratitude, abundance & grace.", 40, Color(0xFFB88C49))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { AarohanApp() } }
}

@Composable fun AarohanApp() {
    var account by remember { mutableStateOf<GoogleAccount?>(null) }
    if (account == null) {
        GoogleSignInScreen { account = it }
        return
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedGuide by remember { mutableStateOf<Guide?>(null) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Purple, background = Cream)) {
        Surface(color = Cream, modifier = Modifier.fillMaxSize()) {
            Scaffold(bottomBar = { BottomBar(selectedTab) { selectedTab = it } }) { padding ->
                when (selectedTab) {
                    0 -> HomeScreen(Modifier.padding(padding)) { selectedGuide = it }
                    1 -> SimpleScreen(Modifier.padding(padding), "Conversations", "Your private spiritual reflections will appear here.")
                    2 -> StoreScreen(Modifier.padding(padding))
                    else -> SimpleScreen(Modifier.padding(padding), "Profile & privacy", "Signed in with Google\n\nAnanya Sharma · Hinduism\n14 October 1996 · Mumbai, India")
                }
            }
            selectedGuide?.let { ChatDialog(it) { selectedGuide = null } }
        }
    }
}

@Composable private fun GoogleSignInScreen(onSignedIn: (GoogleAccount) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✦", color = Purple, fontSize = 42.sp)
        Text("AstroTalks", color = Ink, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
        Text("Conversations for the soul", color = Color.Gray, modifier = Modifier.padding(top = 6.dp))
        Text("\nA private space for thoughtful, AI-generated spiritual reflections.", color = Ink, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
        Button(onClick = { scope.launch { GoogleAuth(context).signIn().onSuccess(onSignedIn).onFailure { error = it.message ?: "Google Sign-In could not be completed." } } }, modifier = Modifier.fillMaxWidth().padding(top = 28.dp), shape = RoundedCornerShape(10.dp)) { Text("Continue with Google") }
        Text("Google Sign-In only · no passwords", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
        error?.let { Text(it, color = Color(0xFFB04747), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 18.dp)) }
    }
}

@Composable private fun HomeScreen(modifier: Modifier, onGuide: (Guide) -> Unit) {
    Column(modifier.padding(horizontal = 20.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
        Spacer(Modifier.height(25.dp)); Text("ASTROTALKS", color = Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Good morning,\nAnanya.", color = Ink, fontSize = 38.sp, fontWeight = FontWeight.SemiBold, lineHeight = 42.sp)
        Text("Take a breath. Ask what’s on your heart.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2A50)), shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(vertical = 22.dp).fillMaxWidth()) {
            Column(Modifier.padding(22.dp)) { Text("A quiet place to return to", color = Color(0xFFD7C99F), fontSize = 12.sp); Text("Who would you like to sit with?", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp)); Text("AI-generated spiritual reflections inspired by tradition.", color = Color(0xFFD4D1DF), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) { Text("Your spiritual circle", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold); Text("240 ✦", color = Gold, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(14.dp)); LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(510.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(guides) { guide -> GuideCard(guide) { onGuide(guide) } } }
        Text("AstroTalks is an AI-generated spiritual experience, not actual communication with a deity. It does not guarantee blessings, miracles, wealth, or outcomes.", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(vertical = 24.dp))
    }
}

@Composable private fun GuideCard(guide: Guide, onClick: () -> Unit) { Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp)) { Column { Box(Modifier.fillMaxWidth().height(100.dp).background(guide.color), contentAlignment = Alignment.Center) { Text(guide.symbol, color = Color.White, fontSize = 42.sp) }; Column(Modifier.padding(12.dp)) { Text(guide.name, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Text(guide.description, color = Color.Gray, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 4.dp)); Text("${guide.rate} pts/min  →", color = Purple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp)) } } } }

@Composable private fun BottomBar(selected: Int, onSelected: (Int) -> Unit) { NavigationBar(containerColor = Color.White) { listOf(Icons.Default.Home to "Home", Icons.Default.Forum to "Chats", Icons.Default.Stars to "Store", Icons.Default.Person to "Profile").forEachIndexed { i, item -> NavigationBarItem(selected = selected == i, onClick = { onSelected(i) }, icon = { Icon(item.first, null) }, label = { Text(item.second, fontSize = 10.sp) }) } } }

@Composable private fun SimpleScreen(modifier: Modifier, title: String, body: String) { Column(modifier.padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text(title, color = Ink, fontSize = 30.sp, fontWeight = FontWeight.SemiBold); Text(body, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp)) } }
@Composable private fun StoreScreen(modifier: Modifier) { Column(modifier.padding(24.dp)) { Text("Points store", color = Ink, fontSize = 30.sp, fontWeight = FontWeight.SemiBold); Text("Simple pricing. Transparent usage.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp)); Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2A50)), modifier = Modifier.padding(vertical = 22.dp).fillMaxWidth()) { Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Text("Available balance", color = Color.White); Spacer(Modifier.weight(1f)); Text("240 ✦", color = Color(0xFFE4D4A9), fontSize = 22.sp, fontWeight = FontWeight.Bold) } }; listOf("300 points · ₹199", "800 points · ₹499", "1,800 points · ₹999").forEach { Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(14.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Text(it, color = Ink, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); Button(onClick = {}, contentPadding = PaddingValues(horizontal = 14.dp)) { Text("Buy", fontSize = 11.sp) } } } } } }

@Composable private fun ChatDialog(guide: Guide, onDismiss: () -> Unit) { var seconds by remember { mutableIntStateOf(60) }; LaunchedEffect(Unit) { while (seconds > 0) { delay(1000); seconds-- } }; AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = onDismiss) { Text("Close") } }, title = { Row(verticalAlignment = Alignment.CenterVertically) { Text(guide.symbol, color = Purple, fontSize = 24.sp); Spacer(Modifier.width(10.dp)); Column { Text(guide.name); Text("00:${seconds.toString().padStart(2, '0')} · ${guide.rate} pts/min", color = Purple, fontSize = 11.sp) } } }, text = { Column { Text("✦ AI-generated spiritual experience", color = Purple, fontSize = 11.sp); Text("\nWelcome, Ananya. Take a gentle breath. What feels most present in your heart today?", color = Ink, lineHeight = 22.sp); OutlinedTextField(value = "", onValueChange = {}, placeholder = { Text("Write what’s on your heart…") }, modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) } }) }
