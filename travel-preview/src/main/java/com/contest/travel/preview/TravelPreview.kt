package com.contest.travel.preview

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** The LinkCar wireframes are the only app-facing flow in the preview module. */
@Composable
internal fun TravelPreview(drivingUx: DrivingUxState, liveTrip: LiveTripState,
    actions: TripActions, foreground: Boolean) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) PleosDark else PleosLight,
        typography = PleosTypography) {
        val convoy = rememberConvoyController(foreground)
        val context = LocalContext.current
        CompositionLocalProvider(LocalTripLive provides liveTrip, LocalTripActions provides actions,
            LocalConvoy provides convoy) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wide = maxWidth >= 1100.dp
                    Column(Modifier.fillMaxSize()) {
                        drivingUx.notice?.let { notice ->
                            Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(notice, Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        LinkCarDemoScreen(wide = wide,
                            enabled = drivingUx.canInteract,
                            onExit = { (context as? Activity)?.finish() },
                            modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
