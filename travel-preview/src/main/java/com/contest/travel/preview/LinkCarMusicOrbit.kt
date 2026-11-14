package com.contest.travel.preview

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val MusicOrbitDurationMs = 24_000
private const val BreathsPerOrbit = 5
private const val CompanionBreathAmount = .035f

/** Motion requested for Figma 443:19762; the source frame has no authored timeline. */
@Composable
internal fun rememberLinkCarMusicOrbit(): State<Float> {
    val transition = rememberInfiniteTransition(label = "musicOrbit")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(MusicOrbitDurationMs, easing = LinearEasing)),
        label = "musicOrbitPhase",
    )
}

// Read state in graphics layers so each frame avoids recomposition and layout.
internal fun Modifier.musicRing(phase: State<Float>, amount: Float): Modifier = graphicsLayer {
    val breath = 1f + amount * sin(phase.value * BreathsPerOrbit)
    scaleX = breath
    scaleY = breath
}

internal fun Modifier.musicCompanionOrbit(
    phase: State<Float>, startCenterX: Float, startCenterY: Float,
): Modifier = graphicsLayer {
    val angle = phase.value
    val sine = sin(angle)
    // Drift outward on the upper/lower arcs to leave the crown and DJ badge visible.
    val drift = 1f + .20f * sine * sine
    val breath = (1f + CompanionBreathAmount * sin(angle * BreathsPerOrbit)) * drift
    // Preserve the original positions on the 405 × 385 Figma orbit at rest.
    val radiusX = 202.5f
    val radiusY = 192.5f
    val startX = startCenterX - 404.5f
    val startY = startCenterY - 368.5f
    val x = (startX * cos(angle) - startY * radiusX / radiusY * sine) * breath
    val y = (startX * radiusY / radiusX * sine + startY * cos(angle)) * breath
    translationX = (x - startX).dp.toPx()
    translationY = (y - startY).dp.toPx()
}

internal fun Modifier.musicDjFloat(phase: State<Float>): Modifier = graphicsLayer {
    translationX = (3f * sin(phase.value * 3f)).dp.toPx()
    translationY = (6f * sin(phase.value * 4f)).dp.toPx()
}
