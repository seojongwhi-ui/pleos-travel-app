package com.contest.travel.preview

import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay

internal val LocalTripLive = staticCompositionLocalOf { LiveTripState() }
internal val LocalTripActions = staticCompositionLocalOf { TripActions() }
internal val LocalConvoy = staticCompositionLocalOf<ConvoyController> { error("Convoy state is not provided") }

internal data class ConvoyController(
    val progress: Double,
    val revision: Int,
    val automatic: Boolean,
    val secondsUntilUpdate: Int,
    val useRealCar: Boolean,
    val advance: () -> Unit,
    val toggleAutomatic: () -> Unit,
    val toggleRealCar: () -> Unit,
    val reset: () -> Unit
)

@Composable
internal fun rememberConvoyController(foreground: Boolean): ConvoyController {
    var progress by rememberSaveable { mutableDoubleStateOf(.38) }
    var revision by rememberSaveable { mutableIntStateOf(0) }
    var automatic by rememberSaveable { mutableStateOf(true) }
    var realCar by rememberSaveable { mutableStateOf(false) }
    var updatedAt by rememberSaveable { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val advance: () -> Unit = {
        progress = (progress + .07).coerceAtMost(1.12)
        revision++
        updatedAt = SystemClock.elapsedRealtime()
        now = updatedAt
    }
    LaunchedEffect(foreground, automatic) {
        if (foreground) {
            // Resume with a full interval rather than advancing while the app was hidden.
            updatedAt = SystemClock.elapsedRealtime()
            while (true) {
                now = SystemClock.elapsedRealtime()
                if (automatic && now - updatedAt >= 120_000) advance()
                delay(1_000)
            }
        }
    }
    return ConvoyController(progress, revision, automatic,
        ((120_000 - (now - updatedAt)).coerceIn(0, 120_000) / 1000).toInt(), realCar,
        advance, { automatic = !automatic }, { realCar = !realCar }, {
            progress = .38; revision = 0; realCar = false
            updatedAt = SystemClock.elapsedRealtime(); now = updatedAt
        })
}
