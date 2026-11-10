package com.contest.travel.preview

import kotlinx.coroutines.flow.Flow

/**
 * Integration boundaries for the handoff's vehicle, navigation and group features.
 * The on-device walkthrough in LinkCarDemo.kt deliberately does not instantiate these adapters.
 * A real adapter must return failures explicitly and must never substitute demo data for a failed read.
 */
internal data class LinkCarApiSlots(
    val vehicle: LinkCarVehiclePort? = null,
    val navigation: LinkCarNavigationPort? = null,
    val mapRoute: LinkCarMapRoutePort? = null,
    val proximity: LinkCarProximityPort? = null,
    val speech: LinkCarSpeechPort? = null,
    val voiceMessage: LinkCarVoiceMessagePort? = null,
    val group: LinkCarGroupPort? = null,
    val notification: LinkCarNotificationPort? = null,
    val proposal: LinkCarProposalPort? = null,
    val call: LinkCarCallPort? = null,
    val music: LinkCarMusicPort? = null
)

internal data class LinkCarVehicleSnapshot(
    val speedMetersPerSecond: Float?,
    val occupiedPassengerSeats: Int?,
    val tripMeters: Float?,
    val tripMinutes: Int?
)

internal interface LinkCarVehiclePort {
    suspend fun readSnapshot(): Result<LinkCarVehicleSnapshot>
}

internal data class LinkCarNavigationSnapshot(
    val location: GeoPoint?,
    val destination: GeoPoint?,
    val remainingMeters: Int?,
    val remainingSeconds: Int?
)

internal interface LinkCarNavigationPort {
    val updates: Flow<Result<LinkCarNavigationSnapshot>>
    suspend fun requestDestination(destination: GeoPoint): Result<Unit>
    suspend fun addWaypoint(waypoint: GeoPoint): Result<Unit>
}

/** Naver Maps renders the map; Directions supplies a polyline. Neither supplies other cars' positions. */
internal interface LinkCarMapRoutePort {
    suspend fun drivingPath(origin: GeoPoint, destination: GeoPoint): Result<List<GeoPoint>>
}

internal interface LinkCarProximityPort {
    fun arrivalsAt(point: GeoPoint, radiusMeters: Int): Flow<Result<Unit>>
}

internal interface LinkCarSpeechPort {
    suspend fun recognizeOnce(): Result<String>
    suspend fun speak(text: String): Result<Unit>
}

internal interface LinkCarVoiceMessagePort {
    suspend fun recordOnce(): Result<ByteArray>
    suspend fun play(audio: ByteArray): Result<Unit>
}

internal sealed interface LinkCarGroupEvent {
    data class Position(val memberId: String, val point: GeoPoint, val atMillis: Long) : LinkCarGroupEvent
    data class Knock(val fromId: String, val toId: String, val accepted: Boolean?) : LinkCarGroupEvent
    data class Vote(val memberId: String, val proposalId: String, val agreed: Boolean) : LinkCarGroupEvent
    data class Message(val memberId: String, val text: String) : LinkCarGroupEvent
}

internal interface LinkCarGroupPort {
    val events: Flow<Result<LinkCarGroupEvent>>
    suspend fun publish(event: LinkCarGroupEvent): Result<Unit>
}

internal interface LinkCarNotificationPort {
    suspend fun invite(memberId: String, tripId: String): Result<Unit>
}

internal data class LinkCarProposal(val isProposal: Boolean, val text: String, val waypoint: GeoPoint?)

internal interface LinkCarProposalPort {
    suspend fun classify(utterance: String): Result<LinkCarProposal>
}

/** STT/TTS cannot transport a live call. This requires a separate audio channel. */
internal interface LinkCarCallPort {
    suspend fun open(memberId: String): Result<Unit>
    suspend fun close(): Result<Unit>
}

internal interface LinkCarMusicPort {
    suspend fun play(trackId: String): Result<Unit>
    suspend fun pause(): Result<Unit>
}
