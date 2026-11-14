package com.contest.travel.preview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/** Shared motion for the local preview. Compose respects the OS animator scale. */
@Composable
internal fun Modifier.linkCarPress(
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) .98f else 1f,
        spring(dampingRatio = .85f, stiffness = 700f), label = "buttonPress")
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
        alpha = if (pressed && enabled) .92f else 1f
    }.clickable(interactionSource = source, indication = null,
        enabled = enabled, role = role, onClick = onClick)
}

@Composable
internal fun LinkCarPageTransition(
    screen: WireScreen,
    direction: Int,
    modifier: Modifier = Modifier,
    content: @Composable (WireScreen) -> Unit,
) {
    // All driving states share one composition: M1/M3 resize and M2 crossfades in place.
    AnimatedContent(targetState = if (screen.isDriving) WireScreen.DRIVE else screen, modifier = modifier,
        transitionSpec = {
            if (!initialState.isPreDrive && !targetState.isPreDrive) {
                fadeIn(tween(0)).togetherWith(fadeOut(tween(0)))
            } else {
            (fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) {
                    direction * (it / 40).coerceAtMost(42)
                }).togetherWith(
                fadeOut(tween(140)) +
                    slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) {
                        -direction * (it / 60).coerceAtMost(28)
                    })
            }
        }, label = "preDrivePage") { target ->
        Box(Modifier.fillMaxSize()) { content(if (target.isDriving && screen.isDriving) screen else target) }
    }
}
