package com.contest.travel.preview

import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private val speedPermission = "android.car.permission.CAR_SPEED"
    private var drivingUx by mutableStateOf(DrivingUxState())
    private var liveTrip by mutableStateOf(LiveTripState())
    private var motion by mutableStateOf(VehicleMotionState())
    private var foreground by mutableStateOf(false)
    private var speedPermissionAsked = false
    private val uxObserver by lazy { DrivingUxObserver(this) {
        drivingUx = it
        connectMotionWhenAllowed()
    } }
    private val motionObserver by lazy { VehicleMotionObserver(this) { motion = it } }
    private val tripSession by lazy { TripSdkSession(this) { liveTrip = it } }
    private val tripActions by lazy { TripActions(tripSession::refresh, tripSession::requestDestination, tripSession::addRestStop) }
    private val speedPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && foreground) motionObserver.start()
        else if (!granted) motionObserver.permissionMissing()
    }

    private fun connectMotionWhenAllowed() {
        if (!foreground) return
        if (checkSelfPermission(speedPermission) == PackageManager.PERMISSION_GRANTED) {
            motionObserver.start()
        } else {
            motionObserver.permissionMissing()
            if (!speedPermissionAsked && drivingUx.canInteract && !drivingUx.setupRestricted) {
                speedPermissionAsked = true
                speedPermissionLauncher.launch(speedPermission)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TravelPreview(drivingUx, liveTrip, motion, tripActions, foreground) }
    }

    override fun onStart() {
        super.onStart()
        foreground = true
        uxObserver.start()
        tripSession.start()
        connectMotionWhenAllowed()
    }

    override fun onStop() {
        foreground = false
        tripSession.stop()
        motionObserver.stop()
        uxObserver.stop()
        super.onStop()
    }
}
