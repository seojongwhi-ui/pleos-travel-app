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

@Composable
internal fun TravelPreview(drivingUx: DrivingUxState, liveTrip: LiveTripState, actions: TripActions, foreground: Boolean) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) PleosDark else PleosLight, typography = PleosTypography) {
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
                Column(Modifier.fillMaxSize().padding(horizontal = Pleos215.WindowPadding)) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = Pleos215.PageNavigation),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (canGoBack) {
                                PleosTextButton(onClick = goBack, modifier = Modifier.heightIn(min = Pleos215.ButtonMedium)) {
                                    AppSymbol("back", Modifier.size(Pleos215.IconMedium))
                                    Spacer(Modifier.width(10.dp))
                                    Text("뒤로가기", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            RouteMark(Modifier.size(Pleos215.Icon))
                            Text("같이가", style = MaterialTheme.typography.titleLarge)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (page != "lobby") {
                                PleosTextButton(onClick = { go("lobby") }, enabled = drivingUx.canInteract,
                                    modifier = Modifier.heightIn(min = Pleos215.ButtonMedium)) {
                                    Text("여행 목록", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            AppBadge(if (inTrip) "개발 시연" else "미리보기")
                        }
                    }
                    drivingUx.notice?.let { notice ->
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                            Text(notice, Modifier.fillMaxWidth().padding(18.dp), style = MaterialTheme.typography.labelMedium)
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
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        AppPage(page, wide, canNavigate, go)
    }
}

@Composable
private fun AppRail(selected: String, enabled: Boolean, go: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.width(180.dp).fillMaxHeight().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("강릉 여행", Modifier.padding(horizontal = 10.dp, vertical = 18.dp),
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            tripTabs.forEach { tab ->
                Surface(
                    onClick = { go(tab.key) }, enabled = enabled,
                    color = if (tab.key == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
                ) {
                    Column(Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppSymbol(tab.symbol, Modifier.size(Pleos215.IconMedium))
                        Text(tab.label, style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (tab.key == selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text("차량 3대", Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BottomTabs(selected: String, enabled: Boolean, go: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            tripTabs.forEach { tab ->
                Surface(
                    onClick = { go(tab.key) }, enabled = enabled,
                    color = if (selected == tab.key) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).heightIn(min = Pleos215.ButtonMedium)
                ) {
                    Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        AppSymbol(tab.symbol, Modifier.size(Pleos215.IconMedium))
                        Text(tab.label, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected == tab.key) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
