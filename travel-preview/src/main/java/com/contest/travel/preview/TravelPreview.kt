package com.contest.travel.preview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AppLight = lightColorScheme(
    primary = ConceptNavy, onPrimary = Color.White,
    background = Color(0xFFF2F5F8), surface = Color.White,
    onBackground = Color(0xFF152738), onSurface = Color(0xFF152738),
    surfaceVariant = Color(0xFFE7EEF3), onSurfaceVariant = Color(0xFF52677A),
    primaryContainer = Color(0xFFE2ECF4), onPrimaryContainer = ConceptNavy,
    secondaryContainer = Color(0xFFFFEBDD), onSecondaryContainer = Color(0xFF984417),
    outline = Color(0xFFD4DEE6)
)
private val AppDark = darkColorScheme(
    primary = Color(0xFFC6DFF6), onPrimary = ConceptNavy,
    background = Color(0xFF111C27), surface = Color(0xFF1C2A39),
    onBackground = Color(0xFFF0F5FA), onSurface = Color(0xFFF0F5FA),
    surfaceVariant = Color(0xFF2A3B4D), onSurfaceVariant = Color(0xFFBFCFDE),
    primaryContainer = Color(0xFF334D65), onPrimaryContainer = Color(0xFFE1EFFD),
    secondaryContainer = Color(0xFF573C2A), onSecondaryContainer = Color(0xFFFFD5B8),
    outline = Color(0xFF516579)
)

@Composable
internal fun TravelPreview(drivingUx: DrivingUxState, liveTrip: LiveTripState, actions: TripActions, foreground: Boolean) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) AppDark else AppLight) {
        val convoy = rememberConvoyController(foreground)
        CompositionLocalProvider(LocalTripLive provides liveTrip, LocalTripActions provides actions, LocalConvoy provides convoy) {
        var backStack by rememberSaveable { mutableStateOf(listOf("lobby")) }
        val page = backStack.last()
        val canGoBack = backStack.size > 1
        val isForm = page in listOf("create", "join")
        val inTrip = page != "lobby" && !isForm
        val selectedTab = when (page) {
            "rest" -> "home"
            "changes" -> "schedule"
            "invite" -> "crew"
            "connection" -> "home"
            else -> page
        }
        val go: (String) -> Unit = { target ->
            if (drivingUx.canInteract && target != backStack.last()) {
                backStack = when {
                    target == "lobby" -> listOf("lobby")
                    target == "home" -> listOf("lobby", "home")
                    tripTabs.any { it.key == target } -> listOf("lobby", "home", target)
                    else -> backStack + target
                }
            }
        }
        val goBack: () -> Unit = {
            if (backStack.size > 1) backStack = backStack.dropLast(1)
        }
        BackHandler(enabled = canGoBack, onBack = goBack)

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= 1100.dp
                Column(Modifier.fillMaxSize().padding(horizontal = if (wide) 30.dp else 22.dp)) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (canGoBack) {
                                TextButton(onClick = goBack, modifier = Modifier.heightIn(min = 76.dp)) {
                                    AppSymbol("back", Modifier.size(28.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("뒤로가기", fontSize = 23.sp)
                                }
                            }
                            RouteMark(Modifier.size(36.dp))
                            Text("같이가", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (page != "lobby") {
                                TextButton(onClick = { go("lobby") }, enabled = drivingUx.canInteract,
                                    modifier = Modifier.heightIn(min = 76.dp)) {
                                    Text("여행 목록", fontSize = 23.sp)
                                }
                            }
                            AppBadge(if (inTrip) "개발 시연" else "미리보기")
                        }
                    }
                    drivingUx.notice?.let { notice ->
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
                            Text(notice, Modifier.fillMaxWidth().padding(18.dp), fontSize = 22.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                    }

                    if (inTrip && wide) {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            AppRail(selectedTab, drivingUx.canInteract, go)
                            key(if (page in journeyMapPages) "journey-map" else page) {
                                PageBody(page, wide, drivingUx.canInteract, go, Modifier.weight(1f))
                            }
                        }
                    } else {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            key(if (page in journeyMapPages) "journey-map" else page) {
                                PageBody(page, wide, drivingUx.canInteract, go, Modifier.weight(1f))
                            }
                            if (inTrip) BottomTabs(selectedTab, drivingUx.canInteract, go)
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun PageBody(page: String, wide: Boolean, canNavigate: Boolean, go: (String) -> Unit, modifier: Modifier) {
    if (page in journeyMapPages) {
        InAppJourneyScreen(page, wide, canNavigate, go, modifier.padding(bottom = 16.dp))
        return
    }
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        AppPage(page, wide, canNavigate, go)
    }
}

@Composable
private fun AppRail(selected: String, enabled: Boolean, go: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.width(180.dp).fillMaxHeight().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("강릉 여행", Modifier.padding(horizontal = 10.dp, vertical = 18.dp),
                fontSize = 24.sp, fontWeight = FontWeight.Bold)
            tripTabs.forEach { tab ->
                Surface(
                    onClick = { go(tab.key) }, enabled = enabled,
                    color = if (tab.key == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
                ) {
                    Column(Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppSymbol(tab.symbol, Modifier.size(30.dp))
                        Text(tab.label, fontSize = 24.sp,
                            fontWeight = if (tab.key == selected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text("차량 3대", Modifier.padding(12.dp), fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BottomTabs(selected: String, enabled: Boolean, go: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tripTabs.forEach { tab ->
                Surface(
                    onClick = { go(tab.key) }, enabled = enabled,
                    color = if (selected == tab.key) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).heightIn(min = 94.dp)
                ) {
                    Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        AppSymbol(tab.symbol, Modifier.size(27.dp))
                        Text(tab.label, fontSize = 21.sp, fontWeight = if (selected == tab.key) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}
