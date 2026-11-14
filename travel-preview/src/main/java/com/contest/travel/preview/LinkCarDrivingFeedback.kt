package com.contest.travel.preview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val ScoreFeedbackMillis = 2600

/** One native layer per button, shared by the full and compact companion cards. */
internal class CompanionButtonMotion(private val s: WireSession, private val name: String,
    private val scope: CoroutineScope, private val onKnock: () -> Unit) {
    private val impulse = Animatable(0f)
    private var job: Job? = null
    private var generation = 0
    private var knocking by mutableStateOf(false)
    var busy by mutableStateOf(false)
        private set
    var showLockedHint by mutableStateOf(false)
        private set

    val buttonModifier: Modifier get() = Modifier.graphicsLayer {
        val v = impulse.value
        rotationZ = v * if (knocking) 1.4f else 3.2f
        translationX = v * (if (knocking) 1.8.dp else 4.dp).toPx()
        scaleX = 1f - .012f * abs(v)
        scaleY = scaleX
    }
    val iconModifier: Modifier get() = Modifier.graphicsLayer {
        val v = impulse.value
        transformOrigin = TransformOrigin(.6f, .8f)
        rotationZ = v * if (knocking) -12f else 4f
        translationY = if (knocking) v * 5.dp.toPx() else 0f
        translationX = if (knocking) v * 2.dp.toPx() else 0f
    }

    fun tap(enabled: Boolean, canKnock: Boolean) {
        if (!enabled || busy) return
        val request = ++generation
        job?.cancel()
        knocking = canKnock
        busy = true
        showLockedHint = !canKnock
        if (canKnock) {
            s.pendingKnockTarget = name
            onKnock()
        }
        job = scope.launch {
            try {
                impulse.snapTo(0f)
                if (canKnock) impulse.animateTo(0f, keyframes {
                    durationMillis = 460
                    0f at 0; -1f at 55; 1f at 105; -.4f at 155
                    0f at 205; -.7f at 260; .7f at 310; -.2f at 365; 0f at 460
                }) else impulse.animateTo(0f, keyframes {
                    durationMillis = 360
                    0f at 0; -1f at 45; 1f at 100; -.65f at 160
                    .4f at 220; -.15f at 280; 0f at 360
                })
                busy = false
                if (!canKnock) delay(2300)
            } finally {
                if (generation == request) {
                    busy = false
                    showLockedHint = false
                    if (canKnock && s.pendingKnockTarget == name) s.pendingKnockTarget = null
                }
            }
        }
    }
}

@Composable
internal fun rememberCompanionButtonMotion(s: WireSession, name: String,
    onKnock: (String) -> Unit): CompanionButtonMotion {
    val scope = rememberCoroutineScope()
    val latestKnock by rememberUpdatedState(onKnock)
    return remember(s, name, scope) { CompanionButtonMotion(s, name, scope) { latestKnock(name) } }
}

@Composable
internal fun DLockedHint(motion: CompanionButtonMotion, name: String,
    x: Number, y: Number, compact: Boolean = false) {
    AnimatedVisibility(motion.showLockedHint, Modifier.dBounds(x, y, if (compact) 344 else 306, 108),
        enter = fadeIn(tween(130)) + slideInHorizontally(tween(160)) { 8 },
        exit = fadeOut(tween(220))) {
        DPanel(0, 0, if (compact) 344 else 306, 108, 24, Color.White,
            Modifier.border(1.dp, DrivingFigma.Input, RoundedCornerShape(24.dp))
                .pointerInput(Unit) { detectTapGestures {} }
                .semantics { liveRegion = LiveRegionMode.Polite }) {
            DText("${name}는 잠겨 있어요", 20, 20, DType.Meta)
            DText("지금은 노크할 수 없어요", 20, 58, DType.Small, DrivingFigma.Subtle)
        }
    }
}

@Composable
internal fun DDrivingScore(s: WireSession, name: String, index: Int, x: Number, y: Number,
    compact: Boolean = false, enabled: Boolean = true) {
    val score = s.drivingScores.score(name)
    val event = s.drivingScores.penalty(name)
    val number = remember(s, name) { Animatable(score.toFloat()) }
    val reaction = remember(s, name) { Animatable(0f) }
    val badge = remember(s, name) { Animatable(1f) }
    val baseColor = if (isLowDrivingScore(score)) DrivingFigma.Red else DrivingFigma.ScoreBlue
    val feedbackColor = if (event?.gained == true) DrivingFigma.Green else DrivingFigma.PenaltyInk
    LaunchedEffect(score, event?.id) {
        if (event == null) number.snapTo(score.toFloat())
        else number.animateTo(score.toFloat(), tween(680))
    }
    LaunchedEffect(event?.id) {
        reaction.snapTo(0f)
        if (event == null) { badge.snapTo(1f); return@LaunchedEffect }
        val age = (feedbackNowMillis() - event.createdAtMillis).coerceAtLeast(0)
        if (age >= ScoreFeedbackMillis) { badge.snapTo(1f); return@LaunchedEffect }
        badge.snapTo(age.toFloat() / ScoreFeedbackMillis)
        coroutineScope {
            launch {
                if (coroutineContext[MotionDurationScale]?.scaleFactor == 0f) {
                    // A disabled animation scale must not remove the explanation of the lost points.
                    badge.snapTo(.2f)
                    delay(ScoreFeedbackMillis - age)
                    badge.snapTo(1f)
                } else badge.animateTo(1f, tween((ScoreFeedbackMillis - age).toInt(), easing = LinearEasing))
            }
            // Resizing a panel can continue an existing badge, but must not repeat the impact.
            if (age < 150) launch {
                reaction.animateTo(0f, keyframes {
                    durationMillis = 680
                    0f at 0; 1f at 100; -.35f at 230; .18f at 380; 0f at 680
                })
            }
        }
    }
    val displayNumber = number.value.roundToInt()
    val wideHundred = !compact && displayNumber == 100
    val compactHundred = compact && displayNumber == 100
    Box(Modifier.dBounds(x.toFloat() - if (wideHundred) 8 else if (compactHundred) 4 else 0, y,
        if (compactHundred) 80 else if (compact) 72 else if (wideHundred) 106 else 90,
        if (compact) 55 else 73)
        .semantics {
            contentDescription = "$name 주행 점수 ${score}점" +
                (event?.let { ", ${it.reason} ${it.signedPoints}점" } ?: "") +
                if (s.motionPreview) ", 감점 시연" else ""
            liveRegion = LiveRegionMode.Polite
        }.then(if (s.motionPreview) Modifier.linkCarPress(enabled) {
            s.drivingScores.deduct(name, 3, "급가속")
        } else Modifier)) {
        Text(displayNumber.toString(), Modifier.fillMaxSize().graphicsLayer {
            translationY = reaction.value * 3.dp.toPx()
            scaleX = 1f + .045f * abs(reaction.value); scaleY = scaleX
        }, style = (if (compact) DType.CompactScore else DType.Score).style(
            lerp(baseColor, feedbackColor, .65f * abs(reaction.value)),
            if (compact) TextAlign.Start else TextAlign.Center), maxLines = 1, softWrap = false)
    }
    if (event != null) Box(Modifier.dBounds(x.toFloat() + if (compact) -12 else 20,
        y.toFloat() - if (compact) 24 else 12, if (event.reason == "급가속") 75 else 104, 38).graphicsLayer {
            val p = badge.value
            alpha = when { p < .07f -> p / .07f; p > .78f -> (1f - p) / .22f; else -> 1f }
            translationY = -8.dp.toPx() * p
            transformOrigin = TransformOrigin(1f, 1f)
            scaleX = 1.2f * (.92f + .08f * (p / .07f).coerceAtMost(1f)); scaleY = scaleX
        }) {
        if (event.reason == "급가속" && event.points == 3)
            DImage(R.drawable.driving_score_acceleration_penalty, 0, 0, 75, 38)
        else DPanel(0, 6, 104, 26, 4,
            if (event.gained) Color(0xFFE5F7ED) else DrivingFigma.PenaltySurface,
            Modifier.border(1.dp, if (event.gained) DrivingFigma.Green.copy(alpha = .25f)
                else DrivingFigma.PenaltyBorder, RoundedCornerShape(4.dp))) {
            Text("${event.reason} ${event.signedPoints}", Modifier.align(Alignment.Center),
                style = DType.Tiny.style(feedbackColor), maxLines = 1)
        }
    }
}
