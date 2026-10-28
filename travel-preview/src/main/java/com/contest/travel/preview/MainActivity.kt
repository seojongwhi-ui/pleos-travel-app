package com.contest.travel.preview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private var drivingUx by mutableStateOf(DrivingUxState())
    private var liveTrip by mutableStateOf(LiveTripState())
    private var foreground by mutableStateOf(false)
    private val uxObserver by lazy { DrivingUxObserver(this) { drivingUx = it } }
    private val tripSession by lazy { TripSdkSession(this) { liveTrip = it } }
    private val tripActions by lazy { TripActions(tripSession::refresh, tripSession::requestDestination, tripSession::addRestStop) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TravelPreview(drivingUx, liveTrip, tripActions, foreground) }
    }

    override fun onStart() {
        super.onStart()
        foreground = true
        uxObserver.start()
        tripSession.start()
    }

    override fun onStop() {
        foreground = false
        tripSession.stop()
        uxObserver.stop()
        super.onStop()
    }
}
