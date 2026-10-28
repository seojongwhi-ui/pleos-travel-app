package com.contest.travel.preview

import kotlin.math.cos
import kotlin.math.min

internal fun placeCarLabels(desired: List<Pair<Float, Float>>, width: Float, height: Float,
    cardWidth: Float, cardHeight: Float, margin: Float, topMargin: Float, bottomMargin: Float): List<Pair<Float, Float>> {
    val placed = mutableListOf<Pair<Float, Float>>()
    desired.forEach { (x, y) ->
        val candidates = listOf(0f, -(cardHeight + margin), cardHeight + margin,
            -2 * (cardHeight + margin), 2 * (cardHeight + margin)).flatMap { dy ->
            listOf(0f, -(cardWidth + margin), cardWidth + margin).map { dx ->
                (x + dx).coerceIn(margin, (width - cardWidth - margin).coerceAtLeast(margin)) to
                    (y + dy).coerceIn(topMargin, (height - cardHeight - bottomMargin).coerceAtLeast(topMargin))
            }
        }.distinct()
        val candidate = candidates.firstOrNull { (cx, cy) -> placed.none { (px, py) ->
            cx < px + cardWidth + margin && cx + cardWidth + margin > px &&
                cy < py + cardHeight + margin && cy + cardHeight + margin > py
        } } ?: candidates.first()
        placed += candidate
    }
    return placed
}

internal data class MapViewport(val minLon: Double, val maxLat: Double, val lonScale: Double,
    val scale: Double, val left: Double, val top: Double) {
    fun project(point: GeoPoint): Pair<Float, Float> =
        Pair((left + (point.longitude - minLon) * lonScale * scale).toFloat(),
            (top + (maxLat - point.latitude) * scale).toFloat())

    companion object {
        fun fit(points: List<GeoPoint>, width: Float, height: Float, padding: Float): MapViewport {
            val valid = points.filter { it.isValid() }
            require(valid.isNotEmpty())
            val minLon = valid.minOf { it.longitude }
            val maxLon = valid.maxOf { it.longitude }
            val minLat = valid.minOf { it.latitude }
            val maxLat = valid.maxOf { it.latitude }
            val lonScale = cos(Math.toRadians((minLat + maxLat) / 2)).coerceAtLeast(.1)
            val spanX = ((maxLon - minLon) * lonScale).coerceAtLeast(.02)
            val spanY = (maxLat - minLat).coerceAtLeast(.02)
            val scale = min((width - 2 * padding).coerceAtLeast(1f) / spanX,
                (height - 2 * padding).coerceAtLeast(1f) / spanY)
            return MapViewport(minLon, maxLat, lonScale, scale,
                (width - (maxLon - minLon) * lonScale * scale) / 2,
                (height - (maxLat - minLat) * scale) / 2)
        }
    }
}
