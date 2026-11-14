package com.contest.travel.preview

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

internal data class VehicleMotionState(
    val meters: Double = 0.0,
    val speedKph: Float = 0f,
    val error: String? = null
)

/** Integrates the same vehicle speed property that Pleos uses for the cluster. */
internal class VehicleMotionObserver(
    private val context: Context,
    private val onState: (VehicleMotionState) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var car: Car? = null
    private var manager: CarPropertyManager? = null
    private var active = false
    private var generation = 0
    private var state = VehicleMotionState()
    private var lastSampleMs = 0L
    private var lastSpeedMps = 0f

    private val listener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            if (value.propertyId != VehiclePropertyIds.PERF_VEHICLE_SPEED) return
            val speed = (value.value as? Number)?.toFloat() ?: return
            handler.post { if (active) sample(speed) }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            if (propertyId == VehiclePropertyIds.PERF_VEHICLE_SPEED) {
                handler.post {
                    if (active) publish(error = "차량 속도를 읽을 수 없어요. Virtual Console 연결을 확인해 주세요.")
                }
            }
        }
    }

    fun permissionMissing() {
        publish(error = "차량 속도 권한이 필요해요. 정차 중 권한을 허용해 주세요.")
    }

    fun start() {
        if (active) return
        active = true
        val token = ++generation
        lastSampleMs = 0L
        lastSpeedMps = 0f
        try {
            car = Car.createCar(context, handler, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                if (token != generation) return@createCar
                if (!ready) {
                    publish(error = "차량 속도 서비스에 연결할 수 없어요.")
                    return@createCar
                }
                try {
                    val service = connected.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
                    manager = service
                    check(service.registerCallback(listener, VehiclePropertyIds.PERF_VEHICLE_SPEED, 5f)) {
                        "차량 속도 업데이트를 등록하지 못했어요."
                    }
                    sample(service.getFloatProperty(VehiclePropertyIds.PERF_VEHICLE_SPEED, 0))
                } catch (error: Exception) {
                    publish(error = "차량 속도 조회 실패: ${error.message ?: error.javaClass.simpleName}")
                }
            }
        } catch (error: Exception) {
            publish(error = "차량 연결 실패: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun sample(speedMps: Float) {
        if (!speedMps.isFinite() || speedMps < -80f || speedMps > 80f) {
            publish(error = "차량 속도 값이 올바르지 않아요.")
            return
        }
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (lastSampleMs == 0L) 0.0
            else ((now - lastSampleMs).coerceIn(0L, 2_000L) / 1000.0)
        // Reverse and standstill must never advance the forward journey.
        val forwardSpeed = speedMps.coerceAtLeast(0f)
        val gained = if (lastSpeedMps > 0f && forwardSpeed > 0f)
            (lastSpeedMps + forwardSpeed) / 2.0 * elapsed else 0.0
        lastSampleMs = now
        lastSpeedMps = forwardSpeed
        state = state.copy(meters = state.meters + gained,
            speedKph = forwardSpeed * 3.6f, error = null)
        onState(state)
    }

    private fun publish(error: String) {
        state = state.copy(speedKph = 0f, error = error)
        onState(state)
    }

    fun stop() {
        active = false
        generation++
        runCatching { manager?.unregisterCallback(listener) }
        manager = null
        runCatching { car?.disconnect() }
        car = null
        lastSampleMs = 0L
        lastSpeedMps = 0f
    }
}
