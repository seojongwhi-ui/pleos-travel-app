package com.contest.travel.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

internal enum class GripAxis { VERTICAL, HORIZONTAL }

/** Figma 34:98682-98684: a visible 64x8 grip with a larger drag target. */
@Composable
internal fun DriveResizeGrip(axis: GripAxis, label: String, enabled: Boolean,
    modifier: Modifier = Modifier, barOffsetY: Dp = 0.dp, barVisible: Boolean = true,
    onClick: (() -> Unit)? = null,
    onDrag: (Float) -> Unit, onRelease: (Float) -> Unit) {
    val latestDrag by rememberUpdatedState(onDrag)
    val latestRelease by rememberUpdatedState(onRelease)
    val target = if (axis == GripAxis.VERTICAL) Modifier.size(96.dp, 24.dp)
        else Modifier.size(32.dp, 96.dp)
    Box(modifier.then(target).semantics { contentDescription = label }
        .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
        .pointerInput(enabled, axis) {
            var total = 0f
            detectDragGestures(onDragStart = { total = 0f },
                onDragEnd = { latestRelease(total); total = 0f },
                onDragCancel = { latestRelease(0f); total = 0f }) { change, amount ->
                if (enabled) {
                    change.consume()
                    val delta = if (axis == GripAxis.VERTICAL) amount.y else amount.x
                    total += delta
                    latestDrag(delta)
                }
            }
        }, contentAlignment = Alignment.Center) {
        if (barVisible) Box(Modifier.size(if (axis == GripAxis.VERTICAL) 64.dp else 8.dp,
            if (axis == GripAxis.VERTICAL) 8.dp else 64.dp)
            .offset(y = barOffsetY)
            .background(Pleos215.Gray400, RoundedCornerShape(4.dp)))
    }
}
