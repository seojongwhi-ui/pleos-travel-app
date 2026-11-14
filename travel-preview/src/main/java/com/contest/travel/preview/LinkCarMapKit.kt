package com.contest.travel.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.naver.maps.map.overlay.OverlayImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Colors and image sizes supplied in the user's lincar-map-kit. */
internal object LinkCarMapPalette {
    const val ownPassed = 0xFF6A7BFF.toInt()
    const val ownRemaining = 0x665263E7
    const val dayeonPassed = 0xFF8D99FF.toInt()
    const val dayeonRemaining = dayeonPassed
    const val munjunPassed = 0xFFFF7777.toInt()
    const val munjunRemaining = 0x66FF7777
    const val ink = 0xFF191A1B.toInt()
    const val labelInk = 0xFF2E323A.toInt()
}

internal data class LinkCarMapImage(
    val icon: OverlayImage,
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val anchor: PointF,
)

/** Renders the supplied pin art, portrait, and pill as one anchored map marker. */
internal object LinkCarMapArtwork {
    private const val scale = 2

    fun car(context: Context, own: Boolean, portraitRes: Int, label: String): LinkCarMapImage {
        val pinWidth = if (own) 118 else 76
        val pinHeight = if (own) 138 else 96
        val pinTip = if (own) 114 else 95
        val photoTop = if (own) 26 else 7
        val photoRadius = 30f
        val typeface = Typeface.create(context.resources.getFont(R.font.asta_sans), Typeface.BOLD)
        val textPaint = textPaint(typeface, 18f, if (own) Color.WHITE else LinkCarMapPalette.labelInk)
        val labelWidth = (textPaint.measureText(label) + 32f).toInt().coerceAtLeast(124)
        val width = maxOf(pinWidth, labelWidth) + 12
        val labelTop = pinHeight + 4
        val height = labelTop + 48
        val bitmap = canvasBitmap(width, height) { canvas ->
            val pinLeft = (width - pinWidth) / 2f
            drawResource(context, canvas,
                if (own) R.drawable.mapkit_pin_me else R.drawable.mapkit_pin_friend,
                RectF(pinLeft, 0f, pinLeft + pinWidth, pinHeight.toFloat()))
            val portrait = BitmapFactory.decodeResource(context.resources, portraitRes)
            if (portrait != null) {
                val cx = pinLeft + pinWidth / 2f
                val cy = photoTop + photoRadius
                val clip = Path().apply { addCircle(cx, cy, photoRadius, Path.Direction.CW) }
                canvas.save()
                canvas.clipPath(clip)
                val edge = minOf(portrait.width, portrait.height)
                val src = Rect((portrait.width - edge) / 2, (portrait.height - edge) / 2,
                    (portrait.width + edge) / 2, (portrait.height + edge) / 2)
                canvas.drawBitmap(portrait, src, RectF(cx - photoRadius, cy - photoRadius,
                    cx + photoRadius, cy + photoRadius),
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                canvas.restore()
                portrait.recycle()
            }
            drawPill(canvas, (width - labelWidth) / 2f, labelTop.toFloat(),
                labelWidth.toFloat(), 40f, if (own) LinkCarMapPalette.ink else Color.WHITE)
            canvas.drawText(label, width / 2f,
                labelTop + 20f - (textPaint.ascent() + textPaint.descent()) / 2f,
                textPaint.apply { textAlign = Paint.Align.CENTER })
        }
        return LinkCarMapImage(OverlayImage.fromBitmap(bitmap), bitmap, width, height,
            PointF(0.5f, pinTip.toFloat() / height))
    }

    fun destination(context: Context, label: String): LinkCarMapImage {
        val typeface = Typeface.create(context.resources.getFont(R.font.asta_sans), Typeface.BOLD)
        val text = textPaint(typeface, 20f, LinkCarMapPalette.labelInk)
        val labelWidth = (text.measureText(label) + 32f).toInt().coerceAtLeast(150)
        val labelLeft = 152f
        val width = labelLeft.toInt() + labelWidth
        val height = 120
        val bitmap = canvasBitmap(width, height) { canvas ->
            drawResource(context, canvas, R.drawable.mapkit_pin_destination,
                RectF(0f, 40f, 72f, 112f))
            canvas.drawLine(72f, 40f, labelLeft, 40f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFB9BDC7.toInt()
                    strokeWidth = 2f
                })
            drawPill(canvas, labelLeft, 20f, labelWidth.toFloat(), 40f,
                Color.WHITE, shadow = false)
            canvas.drawText(label, labelLeft + labelWidth / 2f,
                40f - (text.ascent() + text.descent()) / 2f,
                text.apply { textAlign = Paint.Align.CENTER })
        }
        return LinkCarMapImage(OverlayImage.fromBitmap(bitmap), bitmap, width, height,
            PointF(36f / width, 76f / height))
    }

    fun voice(context: Context, meters: Int): LinkCarMapImage {
        val label = "남긴 음성 · ${meters}m"
        val typeface = Typeface.create(context.resources.getFont(R.font.asta_sans), Typeface.BOLD)
        val text = textPaint(typeface, 20f, Color.WHITE)
        val labelWidth = (text.measureText(label) + 68f).toInt().coerceAtLeast(170)
        // Keep the microphone directly below the "남긴 음성" callout in the
        // overview. The geographic anchor remains the demo point 200 m ahead.
        val pinWidth = 36f
        val labelLeft = pinWidth + 8f
        val pinLeft = labelLeft + 45f + text.measureText("남긴 음성") / 2f - pinWidth / 2f
        val width = labelWidth + labelLeft.toInt()
        val height = 52 + 10 + 56
        val bitmap = canvasBitmap(width, height) { canvas ->
            drawPill(canvas, labelLeft, 4f, labelWidth.toFloat(), 44f, LinkCarMapPalette.ink)
            drawResource(context, canvas, R.drawable.mapkit_icon_mic,
                RectF(labelLeft + 14f, 15f, labelLeft + 36f, 37f))
            canvas.drawText(label, labelLeft + 45f,
                26f - (text.ascent() + text.descent()) / 2f, text)
            drawResource(context, canvas, R.drawable.mapkit_pin_voice,
                RectF(pinLeft, 73f, pinLeft + pinWidth, 118f))
        }
        return LinkCarMapImage(OverlayImage.fromBitmap(bitmap), bitmap, width, height,
            PointF((pinWidth / 2f) / width, 1f))
    }

    private fun canvasBitmap(width: Int, height: Int, draw: (Canvas) -> Unit): Bitmap {
        val bitmap = Bitmap.createBitmap(width * scale, height * scale, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale.toFloat(), scale.toFloat())
        draw(canvas)
        return bitmap
    }

    private fun drawResource(context: Context, canvas: Canvas, resource: Int, target: RectF) {
        val source = BitmapFactory.decodeResource(context.resources, resource) ?: return
        canvas.drawBitmap(source, null, target,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        source.recycle()
    }

    private fun drawPill(canvas: Canvas, x: Float, y: Float, width: Float, height: Float,
        color: Int, shadow: Boolean = true) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            if (shadow) setShadowLayer(8f, 0f, 3f, 0x22000000)
        }
        canvas.drawRoundRect(RectF(x, y, x + width, y + height), height / 2f, height / 2f, paint)
    }

    private fun textPaint(typeface: Typeface, size: Float, color: Int) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = size
            this.color = color
        }
}

/** Road geometry only; vehicle speed and ETA remain with Pleos or the demo controller. */
internal class LinkCarMapRoute(val points: List<GeoPoint>) {
    private val cumulative = DoubleArray(points.size)
    val lengthMeters: Double

    init {
        require(points.size >= 2)
        for (index in 1 until points.size) {
            cumulative[index] = cumulative[index - 1] + distanceMeters(points[index - 1], points[index])
        }
        lengthMeters = cumulative.last().coerceAtLeast(1.0)
    }

    fun pointAtFraction(fraction: Double): GeoPoint = pointAtMeters(lengthMeters * fraction.coerceIn(0.0, 1.0))

    fun pointAtMeters(meters: Double): GeoPoint {
        val target = meters.coerceIn(0.0, lengthMeters)
        val next = cumulative.indexOfFirst { it >= target }.coerceAtLeast(1)
        if (next >= points.size) return points.last()
        val before = next - 1
        val segment = cumulative[next] - cumulative[before]
        val ratio = if (segment == 0.0) 0.0 else (target - cumulative[before]) / segment
        return GeoPoint(points[before].latitude + (points[next].latitude - points[before].latitude) * ratio,
            points[before].longitude + (points[next].longitude - points[before].longitude) * ratio)
    }

    fun splitAtFraction(fraction: Double): Pair<List<GeoPoint>, List<GeoPoint>> {
        val target = lengthMeters * fraction.coerceIn(0.0, 1.0)
        val next = cumulative.indexOfFirst { it >= target }.coerceAtLeast(1).coerceAtMost(points.lastIndex)
        val split = pointAtMeters(target)
        return (points.take(next) + split) to (listOf(split) + points.drop(next))
    }

    /** Fraction of the closest point on the road geometry to a live vehicle position. */
    fun fractionAt(point: GeoPoint): Double {
        val longitudeScale = kotlin.math.cos(Math.toRadians(point.latitude))
        var bestDistanceSquared = Double.POSITIVE_INFINITY
        var bestMeters = 0.0
        for (index in 1 until points.size) {
            val start = points[index - 1]
            val end = points[index]
            val x = (end.longitude - start.longitude) * longitudeScale
            val y = end.latitude - start.latitude
            val lengthSquared = x * x + y * y
            if (lengthSquared == 0.0) continue
            val offsetX = (point.longitude - start.longitude) * longitudeScale
            val offsetY = point.latitude - start.latitude
            val portion = ((offsetX * x + offsetY * y) / lengthSquared).coerceIn(0.0, 1.0)
            val gapX = offsetX - x * portion
            val gapY = offsetY - y * portion
            val distanceSquared = gapX * gapX + gapY * gapY
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared
                bestMeters = cumulative[index - 1] +
                    (cumulative[index] - cumulative[index - 1]) * portion
            }
        }
        return (bestMeters / lengthMeters).coerceIn(0.0, 1.0)
    }
}

internal data class LinkCarRoadLoad(
    val route: LinkCarMapRoute,
    val friendRoutes: List<LinkCarMapRoute>,
    val source: String,
)

/** One OSRM request per car and process, with bundled road geometry as offline fallback. */
internal object LinkCarRoadRouteRepository {
    private val lock = Any()
    private var cached: LinkCarRoadLoad? = null
    private val origins = listOf(demoRoute.first(), GeoPoint(37.73, 127.30),
        GeoPoint(37.70, 127.57))
    private val bundledFiles = listOf("linkcar-demo-road-route.json",
        "linkcar-demo-road-route-dayeon.json", "linkcar-demo-road-route-munjun.json")

    suspend fun load(context: Context): LinkCarRoadLoad = withContext(Dispatchers.IO) {
        synchronized(lock) {
            cached ?: run {
                var fallbackCount = 0
                var straightCount = 0
                val routes = origins.mapIndexed { index, origin ->
                    val online = runCatching { fetchOsrm(origin) }.getOrNull()
                    val points = online ?: run {
                        fallbackCount++
                        runCatching { bundled(context, bundledFiles[index]) }.getOrNull()
                            ?: run { straightCount++; listOf(origin, demoDestination) }
                    }
                    LinkCarMapRoute(points)
                }
                val source = when {
                    straightCount > 0 -> "OSRM 연결 실패 · 일부 직선 시연 경로"
                    fallbackCount > 0 -> "OSRM 연결 실패 · 저장된 도로 경로"
                    else -> "OSRM 도로 경로"
                }
                val result = LinkCarRoadLoad(routes[0], routes.drop(1), source)
                cached = result
                result
            }
        }
    }

    private fun fetchOsrm(start: GeoPoint): List<GeoPoint> {
        val end = demoDestination
        val uri = "https://router.project-osrm.org/route/v1/driving/" +
            "${start.longitude},${start.latitude};${end.longitude},${end.latitude}" +
            "?overview=full&geometries=geojson"
        val connection = URL(uri).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 6_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("User-Agent", "LinkCar-Demo/0.1")
            if (connection.responseCode != 200) error("OSRM HTTP ${connection.responseCode}")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return parseCoordinates(JSONObject(body).getJSONArray("routes").getJSONObject(0)
                .getJSONObject("geometry").getJSONArray("coordinates"))
        } finally {
            connection.disconnect()
        }
    }

    private fun bundled(context: Context, name: String): List<GeoPoint> =
        context.assets.open(name).bufferedReader().use { reader ->
            parseCoordinates(JSONObject(reader.readText()).getJSONArray("coordinates"))
        }

    private fun parseCoordinates(array: org.json.JSONArray): List<GeoPoint> =
        (0 until array.length()).map { index ->
            val coordinate = array.getJSONArray(index)
            GeoPoint(coordinate.getDouble(1), coordinate.getDouble(0))
        }.also { require(it.size >= 2 && it.all(GeoPoint::isValid)) }
}
