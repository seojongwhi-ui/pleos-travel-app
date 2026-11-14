package com.contest.travel.preview

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper

internal data class DrivingUxState(
    val canInteract: Boolean = false,
    val setupRestricted: Boolean = false,
    val notice: String? = "차량의 화면 사용 상태를 확인하고 있어요."
)

/** Reads the platform's UX restrictions. No vehicle properties are changed. */
internal class DrivingUxObserver(
    private val context: Context,
    private val onState: (DrivingUxState) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null
    private var generation = 0
    private val timeout = Runnable {
        onState(DrivingUxState(notice = "차량 상태 응답이 없습니다. 주차 상태에서 다시 열어주세요."))
    }

    fun start() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            onState(DrivingUxState(notice = "이 차량에서는 화면 사용 상태를 확인할 수 없습니다."))
            return
        }
        val request = ++generation
        onState(DrivingUxState())
        handler.postDelayed(timeout, 8_000)
        try {
            car = Car.createCar(context, handler, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connectedCar, ready ->
                if (request != generation) return@createCar
                handler.removeCallbacks(timeout)
                runCatching { manager?.unregisterListener() }
                manager = null
                if (!ready) {
                    onState(DrivingUxState(notice = "차량 연결이 끊겼습니다. 화면을 다시 열어주세요."))
                } else {
                    try {
                        val restrictionsManager = connectedCar.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE)
                            as? CarUxRestrictionsManager
                            ?: error("차량의 화면 사용 상태를 확인할 수 없습니다.")
                        manager = restrictionsManager
                        restrictionsManager.registerListener { restrictions -> update(restrictions) }
                        update(restrictionsManager.currentCarUxRestrictions)
                    } catch (exception: Exception) {
                        onState(DrivingUxState(notice = "차량 상태 확인 실패: ${exception.message ?: exception.javaClass.simpleName}"))
                    }
                }
            }
        } catch (exception: Exception) {
            handler.removeCallbacks(timeout)
            onState(DrivingUxState(notice = "차량 연결 실패: ${exception.message ?: exception.javaClass.simpleName}"))
        }
    }

    private fun update(restrictions: CarUxRestrictions?) {
        if (restrictions == null) {
            onState(DrivingUxState(notice = "차량에서 화면 사용 상태를 제공하지 않습니다."))
        } else if (restrictions.isRequiresDistractionOptimization) {
            val noSetup = restrictions.activeRestrictions and
                CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP != 0
            val noKeyboard = restrictions.activeRestrictions and
                CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD != 0
            onState(DrivingUxState(canInteract = true,
                setupRestricted = noSetup || noKeyboard, notice = null))
        } else {
            onState(DrivingUxState(canInteract = true, notice = null))
        }
    }

    fun stop() {
        generation++
        handler.removeCallbacks(timeout)
        runCatching { manager?.unregisterListener() }
        manager = null
        runCatching { car?.disconnect() }
        car = null
    }
}
