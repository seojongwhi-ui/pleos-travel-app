package com.contest.travel.preview

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.listener.DrivingTimeListener
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.*

/** Only public Vehicle reads and user-triggered Navi route requests. No backend. */
internal class TripSdkSession(context: Context, private val onState: (LiveTripState) -> Unit) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var active = false
    private var generation = 0
    private var state = LiveTripState()
    private var vehicle: Vehicle? = null
    private var navi: NaviHelper? = null
    private var naviListener: NaviHelperEventListener? = null
    private var timeListener: DrivingTimeListener? = null
    private var requestNumber = 0
    private val pending = mutableMapOf<String, Int>()
    private var requestedDestination: GeoPoint? = null

    private fun post(token: Int, action: () -> Unit) {
        handler.post { if (active && token == generation) action() }
    }
    private fun update(transform: (LiveTripState) -> LiveTripState) {
        state = transform(state)
        onState(state)
    }
    private fun problem(error: Throwable): String = "${error.javaClass.simpleName}: ${error.message ?: "상세 메시지 없음"}"
    private fun failureStatus(error: Throwable): ReadStatus =
        if (error.message?.contains("ERROR_NOT_SUPPORTED_PROPERTY") == true) ReadStatus.UNSUPPORTED else ReadStatus.ERROR

    private fun begin(key: String, timeoutMs: Long = 8_000, onTimeout: () -> Unit): Int {
        val id = ++requestNumber
        val token = generation
        pending[key] = id
        handler.postDelayed({
            if (active && token == generation && pending[key] == id) {
                pending.remove(key)
                onTimeout()
            }
        }, timeoutMs)
        return id
    }
    private fun finish(key: String, id: Int): Boolean = if (pending[key] == id) {
        pending.remove(key); true
    } else false

    fun start() {
        if (active) return
        active = true
        val token = ++generation
        state = LiveTripState()
        onState(state)
        try {
            vehicle = Vehicle(appContext).also { it.initialize() }
            update { it.copy(vehicleStatus = "연결 요청 완료 · 데이터 확인 중") }
        } catch (error: Exception) {
            failVehicle(problem(error))
        }
        connectNavigation(token)
        refreshVehicle()
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!active || token != generation) return
                refresh()
                handler.postDelayed(this, 30_000)
            }
        }, 30_000)
    }

    private fun failVehicle(message: String) {
        update { it.copy(vehicleStatus = "차량 연결 실패", minutes = Reading(ReadStatus.ERROR, message = message),
            meters = Reading(ReadStatus.ERROR, message = message), battery = Reading(ReadStatus.ERROR, message = message),
            fuel = Reading(ReadStatus.ERROR, message = message)) }
    }

    fun refresh() {
        if (!active) return
        if (vehicle == null || (!state.naviConnected && "naviConnect" !in pending)) {
            stop()
            start()
            return
        }
        refreshVehicle()
        refreshNavigation()
    }

    private fun <T> readVehicle(
        key: String,
        capability: ((CapabilityConfig) -> Unit, (Exception) -> Unit) -> Unit,
        read: ((T?) -> Unit, (Exception) -> Unit) -> Unit,
        valid: (T) -> Boolean,
        set: (Reading<T>) -> Unit,
        afterRead: (() -> Unit)? = null
    ) {
        if (key in pending) return
        val token = generation
        set(Reading(message = "지원 여부와 현재값 확인 중"))
        val id = begin(key) { set(Reading(ReadStatus.ERROR, message = "8초 동안 차량 응답이 없습니다. 다시 조회해 주세요.")) }
        val failed: (Exception) -> Unit = { error -> post(token) {
            if (finish(key, id)) set(Reading(failureStatus(error), message = error.message ?: problem(error)))
        } }
        try {
            capability({ config -> post(token) {
                if (pending[key] != id) return@post
                if (config.accessType != AccessType.READ && config.accessType != AccessType.READ_WRITE) {
                    finish(key, id)
                    set(Reading(ReadStatus.UNSUPPORTED, message = "이 차량에서 읽기를 지원하지 않습니다 (${config.accessType})"))
                } else {
                    try {
                        read({ value -> post(token) {
                            if (!finish(key, id)) return@post
                            if (value == null) set(Reading(ReadStatus.EMPTY, message = "차량이 값을 제공하지 않았습니다"))
                            else if (!valid(value)) set(Reading(ReadStatus.ERROR, message = "유효하지 않은 차량 응답: $value"))
                            else {
                                set(Reading(ReadStatus.READY, value, "Vehicle SDK", System.currentTimeMillis()))
                                update { it.copy(vehicleStatus = "차량 데이터 수신 중") }
                                Log.i("GatigaSDK", "$key read=$value")
                                afterRead?.invoke()
                            }
                        } }, failed)
                    } catch (error: Exception) { failed(error) }
                }
            } }, failed)
        } catch (error: Exception) { failed(error) }
    }

    private fun refreshVehicle() {
        val sdk = vehicle ?: return
        val odometer = sdk.getOdometer()
        readVehicle("drivingMinutes", odometer::checkTripCurrentDrivingTimeCapabilities, odometer::getTripCurrentDrivingTime,
            { value: Int -> value >= 0 }, { value -> update { it.copy(minutes = value) } }) {
            if (timeListener == null) {
                val token = generation
                val listener = object : DrivingTimeListener {
                    override fun onDrivingTimeUpdated(value: Int?) = post(token) {
                        if (value != null && value >= 0) {
                            update { it.copy(minutes = Reading(ReadStatus.READY, value, "Vehicle SDK · 변경 수신", System.currentTimeMillis())) }
                        } else update { it.copy(minutes = Reading(ReadStatus.EMPTY, message = "주행 시간 변경값이 없습니다")) }
                    }
                    override fun onFailed(error: Exception) = post(token) {
                        update { it.copy(minutes = Reading(ReadStatus.ERROR, message = "변경 수신 실패 · ${problem(error)}")) }
                    }
                }
                try { odometer.registerTripCurrentDrivingTime(listener); timeListener = listener }
                catch (error: Exception) { update { it.copy(minutes = Reading(ReadStatus.ERROR, message = "변경 수신 등록 실패 · ${problem(error)}")) } }
            }
        }
        readVehicle("drivingMeters", odometer::checkTripCurrentDrivingDistanceCapabilities, odometer::getTripCurrentDrivingDistance,
            { value: Float -> value.isFinite() && value >= 0 }, { value -> update { it.copy(meters = value) } })
        val battery = sdk.getEvBattery()
        readVehicle("batteryPercent", battery::checkEvBatteryStateOfChargeCapabilities, battery::getEvBatteryStateOfCharge,
            { value: Int -> value in 0..100 }, { value -> update { it.copy(battery = value) } })
        val fuel = sdk.getFuel()
        readVehicle("fuelPercent", fuel::checkFuelPercentCapabilities, fuel::getFuelPercent,
            { value: Int -> value in 0..100 }, { value -> update { it.copy(fuel = value) } })
    }

    private fun connectNavigation(token: Int) {
        try {
            val sdk = NaviHelper(appContext)
            navi = sdk
            val listener = object : NaviHelperEventListener {
                override fun onCurrentLocationInfo(info: CurrentLocationInfo) = post(token) {
                    pending.remove("location")
                    val point = GeoPoint(info.latitude, info.longitude)
                    update { it.copy(location = if (point.isValid()) Reading(ReadStatus.READY, point, "NaviHelper", System.currentTimeMillis())
                        else Reading(ReadStatus.EMPTY, message = "내비게이션에서 유효한 현재 위치를 제공하지 않았습니다")) }
                    Log.i("GatigaSDK", "navigation location received valid=${point.isValid()}")
                }
                override fun onDestinationInfo(info: DestinationInfo) = post(token) {
                    pending.remove("destination")
                    val point = GeoPoint(info.latitude, info.longitude)
                    val valid = point.isValid() && info.remainDistance >= 0 && info.remainTime >= 0
                    update { it.copy(destination = if (valid) Reading(ReadStatus.READY,
                        LiveDestination(info.name, point, info.remainDistance, info.remainTime), "NaviHelper", System.currentTimeMillis())
                        else Reading(ReadStatus.EMPTY, message = "설정된 목적지 또는 유효한 경로 정보가 없습니다")) }
                    if (valid && requestedDestination?.let { distanceMeters(it, point) < 1500 } == true) {
                        pending.remove("route")
                        requestedDestination = null
                        update { it.copy(routePending = false, routeMessage = "가평 목적지 반영을 확인했습니다") }
                    }
                    Log.i("GatigaSDK", "destination received valid=$valid distance=${info.remainDistance} time=${info.remainTime}")
                }
                override fun onRoutingCompleted() = post(token) {
                    pending.remove("route")
                    update { it.copy(routePending = false, routeMessage = "내비게이션 경로 탐색 완료") }
                    refreshNavigation()
                }
                override fun onWayPointInfo(info: List<WaypointInfo>) = post(token) {
                    pending.remove("waypoints")
                    update { it.copy(waypoints = if (info.isEmpty()) Reading(ReadStatus.EMPTY, message = "설정된 경유지가 없습니다")
                        else Reading(ReadStatus.READY, info.map { point -> point.name.ifBlank { "이름 없는 경유지" } }, "NaviHelper", System.currentTimeMillis())) }
                    Log.i("GatigaSDK", "waypoints received count=${info.size}")
                }
                override fun onWaypointChanged(info: WaypointChangedInfo) = post(token) {
                    pending.remove("route")
                    update { it.copy(routePending = false, routeMessage = "내비게이션 경유지 변경 응답을 받았습니다") }
                    refreshNavigation()
                }
                override fun onError(errorCode: NaviErrorCode) = post(token) {
                    val message = "내비게이션 오류: $errorCode"
                    if (pending.remove("location") != null) update { it.copy(location = Reading(ReadStatus.ERROR, message = message)) }
                    if (pending.remove("destination") != null) update { it.copy(destination = Reading(ReadStatus.ERROR, message = message)) }
                    if (pending.remove("waypoints") != null) update { it.copy(waypoints = Reading(ReadStatus.ERROR, message = message)) }
                    if (pending.remove("route") != null) update { it.copy(routePending = false, routeMessage = message) }
                    update { it.copy(naviStatus = message) }
                    Log.w("GatigaSDK", message)
                }
            }
            naviListener = listener
            val id = begin("naviConnect", 15_000) {
                update { it.copy(naviStatus = "내비게이션 연결 응답이 없습니다", location = Reading(ReadStatus.ERROR, message = "서비스 연결 시간 초과"),
                    waypoints = Reading(ReadStatus.ERROR, message = "서비스 연결 시간 초과"),
                    destination = Reading(ReadStatus.ERROR, message = "서비스 연결 시간 초과")) }
            }
            sdk.initialize(onConnected = { post(token) {
                pending.remove("naviConnect")
                sdk.addListener(listener)
                update { it.copy(naviConnected = true, naviStatus = "NaviHelper 연결됨") }
                refreshNavigation()
            } }, onFailed = { error -> post(token) {
                finish("naviConnect", id)
                update { it.copy(naviConnected = false, naviStatus = "연결 실패 · ${problem(error)}",
                    waypoints = Reading(ReadStatus.ERROR, message = problem(error)),
                    location = Reading(ReadStatus.ERROR, message = problem(error)), destination = Reading(ReadStatus.ERROR, message = problem(error))) }
            } })
        } catch (error: Exception) {
            update { it.copy(naviStatus = "초기화 실패 · ${problem(error)}", location = Reading(ReadStatus.ERROR, message = problem(error)),
                waypoints = Reading(ReadStatus.ERROR, message = problem(error)),
                destination = Reading(ReadStatus.ERROR, message = problem(error))) }
        }
    }

    private fun refreshNavigation() {
        val sdk = navi ?: return
        if (!state.naviConnected) return
        if ("location" !in pending) {
            val id = begin("location") { update { it.copy(location = Reading(ReadStatus.ERROR, message = "현재 위치 조회 응답이 없습니다")) } }
            try { sdk.getCurrentLocationInfo() }
            catch (error: Exception) { finish("location", id); update { it.copy(location = Reading(ReadStatus.ERROR, message = problem(error))) } }
        }
        if ("destination" !in pending) {
            val id = begin("destination") { update { it.copy(destination = Reading(ReadStatus.ERROR, message = "목적지 조회 응답이 없습니다. 내비에서 목적지를 설정해 주세요.")) } }
            try { sdk.getDestinationInfo() }
            catch (error: Exception) { finish("destination", id); update { it.copy(destination = Reading(ReadStatus.ERROR, message = problem(error))) } }
        }
        if ("waypoints" !in pending) {
            val id = begin("waypoints") { update { it.copy(waypoints = Reading(ReadStatus.ERROR, message = "경유지 조회 응답이 없습니다")) } }
            try { sdk.getWaypointInfo() }
            catch (error: Exception) { finish("waypoints", id); update { it.copy(waypoints = Reading(ReadStatus.ERROR, message = problem(error))) } }
        }
    }

    fun requestDestination() = routeRequest {
        requestedDestination = demoDestination
        it.requestRoute(RouteInfo(latitude = demoDestination.latitude, longitude = demoDestination.longitude,
            poiName = "가평 계곡 · 링카 시연", poiId = "", address = "경기도 가평군", poiSubId = "0"))
    }

    fun addRestStop() = routeRequest {
        it.addWaypoint(RequestWaypointInfo(latitude = demoRestStop.latitude, longitude = demoRestStop.longitude,
            poiName = "가평 인근 휴식 · 시연", waypointIndex = WaypointIndex.FIRST, poiId = "", address = "경기도 가평군", poiSubId = "0"))
    }

    private fun routeRequest(action: (NaviHelper) -> Unit) {
        val sdk = navi ?: return
        if (!active || !state.naviConnected || state.routePending) return
        update { it.copy(routePending = true, routeMessage = "내비게이션에 요청했습니다. 처리 결과를 기다리고 있어요.") }
        val id = begin("route", 20_000) {
            update { it.copy(routePending = false, routeMessage = "요청 결과를 확인하지 못했습니다. 내비 상태를 확인해 주세요.") }
        }
        try { action(sdk) }
        catch (error: Exception) { finish("route", id); update { it.copy(routePending = false, routeMessage = "경로 요청 실패 · ${problem(error)}") } }
    }

    fun stop() {
        active = false
        generation++
        pending.clear()
        handler.removeCallbacksAndMessages(null)
        timeListener?.let { listener -> runCatching { vehicle?.getOdometer()?.unregisterTripCurrentDrivingTime(listener) } }
        timeListener = null
        naviListener?.let { listener -> runCatching { navi?.removeListener(listener) } }
        runCatching { navi?.release() }
        runCatching { vehicle?.release() }
        naviListener = null
        navi = null
        vehicle = null
        requestedDestination = null
    }
}
